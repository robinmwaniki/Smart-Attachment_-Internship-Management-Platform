package com.library.smart_internship.security;

import com.library.smart_internship.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:keycloaksecuritytest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.mail.username=test",
        "spring.mail.password=test",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "management.tracing.sampling.probability=0.0",
        "keycloak.api-audience=smart-attachment-api",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/smart-attachment",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/smart-attachment/protocol/openid-connect/certs"
})
class ApiSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext applicationContext;

    @MockitoBean
    private ApplicationService applicationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void rejectsRequestsWithoutBearerAuthentication() throws Exception {
        mockMvc.perform(get("/api/applications/internship/99"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAuthenticatedStudentFromCompanyApplicationList() throws Exception {
        mockMvc.perform(get("/api/applications/internship/99")
                        .with(jwt().jwt(token -> token.subject("student-sub"))
                                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsCompanyRoleToReachItsApplicationListEndpoint() throws Exception {
        when(applicationService.getApplicationsForRecruiter("company-sub", 99L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/applications/internship/99")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(token -> token.subject("company-sub"))
                                .authorities(new SimpleGrantedAuthority("ROLE_COMPANY"))))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void deniesUnmigratedApiRoutesEvenToAdministrators() throws Exception {
        mockMvc.perform(get("/api/unmigrated-resource")
                        .with(jwt().jwt(token -> token.subject("admin-sub"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isForbidden());
    }


    @Test
    void rejectsStudentApplicationListingWithoutBearerAuthentication() throws Exception {
        mockMvc.perform(get("/api/applications/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsCompanyRoleFromStudentApplicationListing() throws Exception {
        mockMvc.perform(get("/api/applications/mine")
                        .with(jwt().jwt(token -> token.subject("company-sub"))
                                .authorities(new SimpleGrantedAuthority("ROLE_COMPANY"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsStudentRoleToListItsOwnApplications() throws Exception {
        when(applicationService.getApplicationsForStudent("student-sub")).thenReturn(List.of());

        mockMvc.perform(get("/api/applications/mine")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(token -> token.subject("student-sub"))
                                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

}
