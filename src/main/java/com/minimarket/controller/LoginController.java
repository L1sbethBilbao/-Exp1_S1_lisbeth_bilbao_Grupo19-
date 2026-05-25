package com.minimarket.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoginController {

    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String loginPage(@RequestParam(required = false) String error, CsrfToken csrfToken) {
        String errorMsg = error != null
                ? "<p style=\"color:red;\">Usuario o contrasena incorrectos.</p>"
                : "";
        String csrfField = csrfToken != null
                ? "<input type=\"hidden\" name=\"_csrf\" value=\"" + csrfToken.getToken() + "\"/>"
                : "";
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8"/>
                    <title>Login - Minimarket</title>
                </head>
                <body>
                    <h1>Minimarket - Login</h1>
                    %s
                    <form method="post" action="/login">
                        %s
                        <label>Usuario: <input type="text" name="username" required/></label><br/><br/>
                        <label>Contrasena: <input type="password" name="password" required/></label><br/><br/>
                        <button type="submit">Ingresar</button>
                    </form>
                    <p>Prueba: cliente1 / cliente123</p>
                </body>
                </html>
                """.formatted(errorMsg, csrfField);
    }
}
