$base = "http://localhost:8080"
$results = @()

function Add-Result($name, $code, $expected) {
    $ok = $expected -contains [string]$code
    $script:results += [PSCustomObject]@{ Prueba = $name; Codigo = $code; Esperado = ($expected -join '/'); OK = $ok }
    if (-not $ok) { Write-Host "FAIL: $name => $code (esperado $($expected -join '/'))" -ForegroundColor Red }
}

function Login-As($jar, $user, $pass) {
    curl.exe -s -c $jar -b $jar "$base/login" -o "$env:TEMP\login-$user.html" | Out-Null
    $m = Select-String -Path "$env:TEMP\login-$user.html" -Pattern 'name="_csrf" value="([^"]+)"'
    if (-not $m) { return $false }
    $csrf = $m.Matches.Groups[1].Value
    $code = curl.exe -s -o NUL -w "%{http_code}" -b $jar -c $jar -X POST `
        -d "username=$user&password=$pass&_csrf=$csrf" -H "X-XSRF-TOKEN: $csrf" "$base/login"
    return $code -eq "302"
}

Write-Host "=== Verificacion Minimarket ===" -ForegroundColor Cyan

Add-Result "GET /public/hola" (curl.exe -s -o NUL -w "%{http_code}" "$base/public/hola") @("200")
$body = curl.exe -s "$base/public/hola"
Add-Result "Contenido public/hola" $(if ($body -match "Hola") { "200" } else { "000" }) @("200")

Add-Result "GET /api/productos sin sesion" (curl.exe -s -o NUL -w "%{http_code}" "$base/api/productos") @("401")

$html = curl.exe -s "$base/login"
Add-Result "GET /login HTML" $(if ($html -match "Minimarket") { "200" } else { "000" }) @("200")
Add-Result "Formulario con _csrf" $(if ($html -match '_csrf') { "200" } else { "000" }) @("200")

$headers = curl.exe -s -I "$base/login"
Add-Result "Cabecera CSP" $(if ($headers -match "Content-Security-Policy") { "200" } else { "000" }) @("200")

$jc = "$env:TEMP\jar-cliente.txt"
if (Login-As $jc "cliente1" "cliente123") {
    Add-Result "Login cliente1" "302" @("302")
    Add-Result "CLIENTE GET productos" (curl.exe -s -o NUL -w "%{http_code}" -b $jc "$base/api/productos") @("200")
    Add-Result "CLIENTE GET usuarios (403)" (curl.exe -s -o NUL -w "%{http_code}" -b $jc "$base/api/usuarios") @("403")
    Add-Result "CLIENTE GET inventario (403)" (curl.exe -s -o NUL -w "%{http_code}" -b $jc "$base/api/inventario") @("403")
} else {
    Add-Result "Login cliente1" "000" @("302")
}

$je = "$env:TEMP\jar-empleado.txt"
if (Login-As $je "empleado1" "empleado123") {
    Add-Result "Login empleado1" "302" @("302")
    Add-Result "EMPLEADO GET inventario" (curl.exe -s -o NUL -w "%{http_code}" -b $je "$base/api/inventario") @("200")
    Add-Result "EMPLEADO GET usuarios (403)" (curl.exe -s -o NUL -w "%{http_code}" -b $je "$base/api/usuarios") @("403")
} else {
    Add-Result "Login empleado1" "000" @("302")
}

$jg = "$env:TEMP\jar-gerente.txt"
if (Login-As $jg "gerente1" "gerente123") {
    Add-Result "Login gerente1" "302" @("302")
    Add-Result "GERENTE GET usuarios" (curl.exe -s -o NUL -w "%{http_code}" -b $jg "$base/api/usuarios") @("200")
} else {
    Add-Result "Login gerente1" "000" @("302")
}

Write-Host ""
$results | Format-Table -AutoSize
$ok = ($results | Where-Object { $_.OK }).Count
$total = $results.Count
Write-Host "Resultado: $ok / $total OK" -ForegroundColor $(if ($ok -eq $total) { "Green" } else { "Yellow" })
if ($ok -ne $total) { exit 1 }
