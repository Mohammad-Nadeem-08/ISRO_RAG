package com.spacedoc.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/spacedoc",
    "spring.datasource.username=spacedoc",
    "spring.datasource.password=spacedoc_local_dev_password",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
@DisplayName("Security & CSRF Login Integration Tests")
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("1. Test access to login page without authentication")
    void testLoginPageAccess() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("2. Test unauthenticated request to protected dashboard redirects to login")
    void testProtectedDashboardRedirect() throws Exception {
        mockMvc.perform(get("/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @DisplayName("3. Test successful login with valid credentials and CSRF token")
    void testSuccessfulLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("admin_local_dev_password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"))
            .andExpect(authenticated().withUsername("admin"));
    }

    @Test
    @DisplayName("4. Test failed login with invalid credentials")
    void testFailedLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin").password("wrongpassword"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?error"))
            .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("5. Test login submission without CSRF token returns 403 Forbidden")
    void testLoginWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "admin")
                .param("password", "admin_local_dev_password"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("6. Test POST /api/rag/ask without CSRF token returns 403 Forbidden")
    @WithMockUser(username = "admin")
    void testRagAskWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/rag/ask")
                .contentType("application/json")
                .content("{\"query\":\"test query\",\"topK\":5}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("7. Test POST /api/rag/ask with valid CSRF token")
    @WithMockUser(username = "admin")
    void testRagAskWithCsrf() throws Exception {
        mockMvc.perform(post("/api/rag/ask")
                .with(csrf())
                .contentType("application/json")
                .content("{\"query\":\"test query\",\"topK\":5}"))
            .andExpect(status().is2xxSuccessful());
    }
}
