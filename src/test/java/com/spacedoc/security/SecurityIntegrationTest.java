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
    "spring.datasource.url=jdbc:h2:mem:securitytestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.flyway.enabled=false",
    "spacedoc.answer.enable-mock=true"
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

    @Test
    @DisplayName("8. Test successful POST /logout with valid CSRF token redirects to login?logout and clears auth")
    @WithMockUser(username = "admin")
    void testSuccessfulLogoutWithCsrf() throws Exception {
        mockMvc.perform(post("/logout")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"))
            .andExpect(unauthenticated())
            .andExpect(cookie().maxAge("JSESSIONID", 0));
    }

    @Test
    @DisplayName("9. Test POST /logout without CSRF token returns 403 Forbidden")
    @WithMockUser(username = "admin")
    void testLogoutWithoutCsrf() throws Exception {
        mockMvc.perform(post("/logout"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("10. Test end-to-end session lifecycle: login, access protected page, logout, access denied")
    void testSessionLifecycle() throws Exception {
        var loginResult = mockMvc.perform(formLogin("/login").user("admin").password("admin_local_dev_password"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"))
            .andExpect(authenticated().withUsername("admin"))
            .andReturn();

        var session = (org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/logout")
                .session(session)
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?logout"))
            .andExpect(unauthenticated());

        mockMvc.perform(get("/dashboard").session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }
}
