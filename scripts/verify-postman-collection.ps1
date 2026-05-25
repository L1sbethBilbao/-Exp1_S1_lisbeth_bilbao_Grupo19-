# Verifica la coleccion Postman completa (equivalente HTTP) por rol.
$base = "http://localhost:8080"
$results = @()
$failCount = 0

function Add-Result($folder, $name, $role, $code, $expected, $note = "") {
    $expStr = if ($expected -is [array]) { ($expected -join '/') } else { "$expected" }
    $ok = $expected -contains [string]$code
    if (-not $ok) { $script:failCount++ }
    $script:results += [PSCustomObject]@{
        Carpeta = $folder; Peticion = $name; Rol = $role
        Codigo = $code; Esperado = $expStr; OK = $ok; Nota = $note
    }
}

function Get-Csrf($jar) {
    $html = curl.exe -s -c $jar -b $jar "$base/login"
    $m = [regex]::Match($html, 'name="_csrf" value="([^"]+)"')
    if ($m.Success) { return $m.Groups[1].Value }
    return $null
}

function Invoke-Api($method, $url, $jar, $bodyJson = $null, $csrf = $null, $form = $null) {
    $args = @("-s", "-o", "NUL", "-w", "%{http_code}", "-b", $jar, "-c", $jar, "-X", $method)
    if ($csrf) { $args += @("-H", "X-XSRF-TOKEN: $csrf") }
    if ($bodyJson) {
        $bodyFile = "$env:TEMP\pm-body.json"
        Set-Content -Path $bodyFile -Value $bodyJson -Encoding UTF8 -NoNewline
        $args += @("-H", "Content-Type: application/json", "--data-binary", "@$bodyFile")
    }
    if ($form) { $args += @("-d", $form) }
    $args += $url
    return (curl.exe @args)
}

function Invoke-ApiGetBody($url, $jar) {
    return (curl.exe -s -b $jar $url)
}

function Login-As($jar, $user, $pass) {
    $csrf = Get-Csrf $jar
    if (-not $csrf) { return $false }
    $code = Invoke-Api POST "$base/login" $jar $null $csrf "username=$user&password=$pass&_csrf=$csrf"
    return $code -eq "302"
}

function Logout-Session($jar) {
    $csrf = Get-Csrf $jar
    if ($csrf) { Invoke-Api POST "$base/logout" $jar $null $csrf "_csrf=$csrf" | Out-Null }
}

function New-Jar { return "$env:TEMP\pm-$(New-Guid).txt" }

# --- Health ---
try {
    $h = curl.exe -s -o NUL -w "%{http_code}" "$base/public/hola" 2>$null
    if ($h -ne "200") {
        Write-Host "ERROR: App no responde en $base (codigo $h). Ejecuta: mvnw spring-boot:run" -ForegroundColor Red
        exit 2
    }
} catch {
    Write-Host "ERROR: No se puede conectar a $base" -ForegroundColor Red
    exit 2
}

Write-Host "=== Verificacion coleccion Postman (HTTP) ===" -ForegroundColor Cyan

# 01 - Publico
Add-Result "01" "GET /public/hola" "anon" (curl.exe -s -o NUL -w "%{http_code}" "$base/public/hola") @("200")

# 00 - GET Login CSRF
$j0 = New-Jar
$html = curl.exe -s -c $j0 -b $j0 "$base/login"
Add-Result "00" "GET Login page (CSRF)" "anon" $(if ($html -match '_csrf') { "200" } else { "000" }) @("200")

# 09 - 401 sin sesion
$jAnon = New-Jar
Add-Result "09" "401 Productos sin autenticacion" "anon" (Invoke-Api GET "$base/api/productos" $jAnon) @("401")

# 00 - Login invalido (1 intento)
$ji = New-Jar
$csrf = Get-Csrf $ji
$bad = Invoke-Api POST "$base/login" $ji $null $csrf "username=cliente1&password=wrongpassword&_csrf=$csrf"
Add-Result "00" "Login credenciales invalidas" "anon" $bad @("302") "redirect /login?error"

# 00 - Logins OK
foreach ($u in @(
    @{ user = "cliente1"; pass = "cliente123"; rol = "CLIENTE" }
    @{ user = "empleado1"; pass = "empleado123"; rol = "EMPLEADO" }
    @{ user = "gerente1"; pass = "gerente123"; rol = "GERENTE" }
)) {
    $j = New-Jar
    $ok = Login-As $j $u.user $u.pass
    Add-Result "00" "Login $($u.user)" $u.rol $(if ($ok) { "302" } else { "000" }) @("302")
    Set-Variable -Name "jar$($u.rol)" -Value $j -Scope Script
}

$jc = $jarCLIENTE
$je = $jarEMPLEADO
$jg = $jarGERENTE

# 09 - Seguridad por rol
Add-Result "09" "403 CLIENTE usuarios" "CLIENTE" (Invoke-Api GET "$base/api/usuarios" $jc) @("403")
Add-Result "09" "403 CLIENTE inventario" "CLIENTE" (Invoke-Api GET "$base/api/inventario" $jc) @("403")
Add-Result "09" "403 CLIENTE crea producto" "CLIENTE" (Invoke-Api POST "$base/api/productos" $jc '{"nombre":"Hack","precio":1,"stock":1,"categoria":{"id":1}}') @("403")
Add-Result "09" "200 EMPLEADO inventario" "EMPLEADO" (Invoke-Api GET "$base/api/inventario" $je) @("200")
Add-Result "09" "200 GERENTE usuarios" "GERENTE" (Invoke-Api GET "$base/api/usuarios" $jg) @("200")

# IDOR: empleado crea carrito (usuario id 2 = empleado1), cliente intenta leerlo
$idorId = $null
$postCar = Invoke-Api POST "$base/api/carrito" $je '{"usuario":{"id":2},"producto":{"id":1},"cantidad":1}'
if ($postCar -eq "200") {
    $resp = Invoke-ApiGetBody "$base/api/carrito" $je
    $ids = [regex]::Matches($resp, '"id":(\d+)') | ForEach-Object { [int]$_.Groups[1].Value }
    if ($ids.Count -gt 0) { $idorId = ($ids | Measure-Object -Maximum).Maximum }
}
if ($idorId) {
    Add-Result "09" "403 CLIENTE carrito ajeno (IDOR)" "CLIENTE" (Invoke-Api GET "$base/api/carrito/$idorId" $jc) @("403") "carrito id $idorId de empleado"
} else {
    Add-Result "09" "403 CLIENTE carrito ajeno (IDOR)" "CLIENTE" "000" @("403") "no se pudo crear carrito empleado"
}

# Coleccion dice carrito/2 -> en seed ambos son de cliente1 -> 200 esperado por diseno seed
Add-Result "09" "GET carrito/2 (seed cliente1)" "CLIENTE" (Invoke-Api GET "$base/api/carrito/2" $jc) @("200","403") "Postman espera 403 pero seed id2 es del mismo cliente"

# --- CRUD por rol: CLIENTE ---
$folder = "02-08"
Add-Result $folder "GET productos" "CLIENTE" (Invoke-Api GET "$base/api/productos" $jc) @("200")
Add-Result $folder "GET producto/1" "CLIENTE" (Invoke-Api GET "$base/api/productos/1" $jc) @("200")
Add-Result $folder "POST producto" "CLIENTE" (Invoke-Api POST "$base/api/productos" $jc '{"nombre":"X","precio":1,"stock":1,"categoria":{"id":1}}') @("403")
Add-Result $folder "GET categorias" "CLIENTE" (Invoke-Api GET "$base/api/categorias" $jc) @("200")
Add-Result $folder "GET categoria/1" "CLIENTE" (Invoke-Api GET "$base/api/categorias/1" $jc) @("200")
Add-Result $folder "POST categoria" "CLIENTE" (Invoke-Api POST "$base/api/categorias" $jc '{"nombre":"X"}') @("403")
Add-Result $folder "GET carrito" "CLIENTE" (Invoke-Api GET "$base/api/carrito" $jc) @("200")
Add-Result $folder "GET carrito/1" "CLIENTE" (Invoke-Api GET "$base/api/carrito/1" $jc) @("200")
Add-Result $folder "POST carrito" "CLIENTE" (Invoke-Api POST "$base/api/carrito" $jc '{"usuario":{"id":1},"producto":{"id":3},"cantidad":1}') @("200")
Add-Result $folder "PUT carrito/1" "CLIENTE" (Invoke-Api PUT "$base/api/carrito/1" $jc '{"usuario":{"id":1},"producto":{"id":1},"cantidad":2}') @("200")
Add-Result $folder "GET ventas" "CLIENTE" (Invoke-Api GET "$base/api/ventas" $jc) @("200")
Add-Result $folder "GET venta/1" "CLIENTE" (Invoke-Api GET "$base/api/ventas/1" $jc) @("200")
Add-Result $folder "POST venta simple" "CLIENTE" (Invoke-Api POST "$base/api/ventas" $jc '{"usuario":{"id":1},"fecha":"2026-05-24T12:00:00.000+00:00"}') @("200")
Add-Result $folder "GET inventario" "CLIENTE" (Invoke-Api GET "$base/api/inventario" $jc) @("403")
Add-Result $folder "GET detalle-ventas" "CLIENTE" (Invoke-Api GET "$base/api/detalle-ventas" $jc) @("403")
Add-Result $folder "GET usuarios" "CLIENTE" (Invoke-Api GET "$base/api/usuarios" $jc) @("403")

# --- EMPLEADO ---
Add-Result $folder "GET productos" "EMPLEADO" (Invoke-Api GET "$base/api/productos" $je) @("200")
Add-Result $folder "POST producto" "EMPLEADO" (Invoke-Api POST "$base/api/productos" $je '{"nombre":"GalletasTest","precio":900,"stock":25,"categoria":{"id":2}}') @("200")
Add-Result $folder "POST producto precio negativo" "EMPLEADO" (Invoke-Api POST "$base/api/productos" $je '{"nombre":"Invalido","precio":-1,"stock":1,"categoria":{"id":1}}') @("400")
Add-Result $folder "PUT producto/1" "EMPLEADO" (Invoke-Api PUT "$base/api/productos/1" $je '{"nombre":"Agua 500ml","precio":850,"stock":45,"categoria":{"id":1}}') @("200")
Add-Result $folder "GET categorias" "EMPLEADO" (Invoke-Api GET "$base/api/categorias" $je) @("200")
Add-Result $folder "POST categoria" "EMPLEADO" (Invoke-Api POST "$base/api/categorias" $je '{"nombre":"LacteosTest"}') @("200")
Add-Result $folder "GET inventario" "EMPLEADO" (Invoke-Api GET "$base/api/inventario" $je) @("200")
Add-Result $folder "GET inventario/1" "EMPLEADO" (Invoke-Api GET "$base/api/inventario/1" $je) @("200")
Add-Result $folder "POST inventario" "EMPLEADO" (Invoke-Api POST "$base/api/inventario" $je '{"producto":{"id":1},"cantidad":10,"tipoMovimiento":"Entrada","fechaMovimiento":"2026-05-24T12:00:00.000+00:00"}') @("200")
Add-Result $folder "GET detalle-ventas" "EMPLEADO" (Invoke-Api GET "$base/api/detalle-ventas" $je) @("200")
Add-Result $folder "GET detalle-venta/1" "EMPLEADO" (Invoke-Api GET "$base/api/detalle-ventas/1" $je) @("200")
Add-Result $folder "POST detalle-venta" "EMPLEADO" (Invoke-Api POST "$base/api/detalle-ventas" $je '{"venta":{"id":1},"producto":{"id":2},"cantidad":1,"precio":1200}') @("200")
Add-Result $folder "GET usuarios" "EMPLEADO" (Invoke-Api GET "$base/api/usuarios" $je) @("403")
Add-Result $folder "GET carrito (todos)" "EMPLEADO" (Invoke-Api GET "$base/api/carrito" $je) @("200")

# --- GERENTE ---
Add-Result $folder "GET usuarios" "GERENTE" (Invoke-Api GET "$base/api/usuarios" $jg) @("200")
$usersJson = curl.exe -s -b $jg "$base/api/usuarios"
$noPass = -not ($usersJson -match '"password"')
Add-Result $folder "JSON sin password" "GERENTE" $(if ($noPass) { "200" } else { "000" }) @("200") "campo password ausente"
Add-Result $folder "GET usuario/1" "GERENTE" (Invoke-Api GET "$base/api/usuarios/1" $jg) @("200")
Add-Result $folder "PUT usuario/1" "GERENTE" (Invoke-Api PUT "$base/api/usuarios/1" $jg '{"username":"cliente1","activo":true,"roles":[{"nombre":"ROLE_CLIENTE"}]}') @("200")
$newUser = "pmuser" + (Get-Random -Maximum 99999)
Add-Result $folder "POST usuario nuevo" "GERENTE" (Invoke-Api POST "$base/api/usuarios" $jg "{`"username`":`"$newUser`",`"password`":`"testpass99`",`"activo`":true,`"roles`":[{`"nombre`":`"ROLE_CLIENTE`"}]}") @("200")
Add-Result $folder "GET inventario" "GERENTE" (Invoke-Api GET "$base/api/inventario" $jg) @("200")
Add-Result $folder "POST producto" "GERENTE" (Invoke-Api POST "$base/api/productos" $jg '{"nombre":"GerenteProd","precio":100,"stock":5,"categoria":{"id":1}}') @("200")

# 00 - Logout
$csrfL = Get-Csrf $jc
$lo = Invoke-Api POST "$base/logout" $jc $null $csrfL "_csrf=$csrfL"
Add-Result "00" "Logout" "CLIENTE" $lo @("302")
Add-Result "00" "Tras logout API 401" "anon" (Invoke-Api GET "$base/api/productos" $jc) @("401")

Write-Host ""
$results | Format-Table -AutoSize -Wrap
$ok = ($results | Where-Object { $_.OK }).Count
$total = $results.Count
Write-Host ""
Write-Host "Resultado: $ok / $total OK" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Yellow" })
if ($failCount -gt 0) {
    Write-Host "Fallos:" -ForegroundColor Red
    $results | Where-Object { -not $_.OK } | Format-Table -AutoSize
    exit 1
}
exit 0
