package com.library.smart_internship.security;

import com.library.smart_internship.entity.Student;
import com.library.smart_internship.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sessionboundarytest;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
class SessionApiIsolationIntegrationTest {

    private static final String EMAIL = "session.student@example.test";
    private static final String PASSWORD = "correct-test-password";

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private StudentRepository studentRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Student student = new Student();
        student.setId(1L);
        student.setName("Session Test Student");
        student.setEmail(EMAIL);
        student.setPassword(passwordEncoder.encode(PASSWORD));
        student.setRole("STUDENT");
        when(studentRepository.findAll()).thenReturn(List.of(student));

        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void keepsLegacySessionLoginWorkingButDoesNotAcceptThatSessionForApiCalls() throws Exception {
        MvcResult loginResult = mockMvc.perform(formLogin()
                        .user(EMAIL)
                        .password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/dashboard"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "Successful form login should establish a browser session");

        mockMvc.perform(get("/student/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));

        mockMvc.perform(get("/api/applications/mine").session(session))
                .andExpect(status().isUnauthorized());
    }
}
