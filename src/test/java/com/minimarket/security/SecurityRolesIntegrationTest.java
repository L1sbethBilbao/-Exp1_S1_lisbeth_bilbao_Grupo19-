package com.minimarket.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRolesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // DataInitializer carga usuarios al iniciar el contexto
    }

    @Test
    void publicEndpoint_sinLogin_retorna200() throws Exception {
        mockMvc.perform(get("/public/hola"))
                .andExpect(status().isOk());
    }

    @Test
    void productos_sinLogin_retorna401() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "cliente1", roles = "CLIENTE")
    void productos_cliente_retorna200() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cliente1", roles = "CLIENTE")
    void usuarios_cliente_retorna403() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "gerente1", roles = "GERENTE")
    void usuarios_gerente_retorna200() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "gerente1", roles = "GERENTE")
    void gerente_crearUsuario_aceptaPasswordEnJson_sinExponerla() throws Exception {
        String username = "cliente2_" + System.nanoTime();
        String body = """
                {
                  "username": "%s",
                  "password": "cliente456",
                  "activo": true,
                  "roles": [{ "nombre": "ROLE_CLIENTE" }]
                }
                """.formatted(username);
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @WithMockUser(username = "empleado1", roles = "EMPLEADO")
    void inventario_empleado_retorna200() throws Exception {
        mockMvc.perform(get("/api/inventario"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "cliente1", roles = "CLIENTE")
    void inventario_cliente_retorna403() throws Exception {
        mockMvc.perform(get("/api/inventario"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "cliente1", roles = "CLIENTE")
    void categorias_cliente_retorna200() throws Exception {
        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk());
    }
}
