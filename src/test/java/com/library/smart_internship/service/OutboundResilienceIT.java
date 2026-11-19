package com.library.smart_internship.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:outbound-resilience;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
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
        "management.health.mail.enabled=false",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/smart-attachment",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/smart-attachment/protocol/openid-connect/certs",
        "africastalking.api-key=test-api-key",
        "africastalking.username=sandbox",
        "africastalking.base-url=https://api.sandbox.africastalking.com/version1/messaging",
        "resilience4j.circuitbreaker.instances.smsGateway.slidingWindowSize=3",
        "resilience4j.circuitbreaker.instances.smsGateway.minimumNumberOfCalls=3",
        "resilience4j.circuitbreaker.instances.smsGateway.failureRateThreshold=50",
        "resilience4j.circuitbreaker.instances.smsGateway.waitDurationInOpenState=30s",
        "resilience4j.circuitbreaker.instances.emailGateway.slidingWindowSize=3",
        "resilience4j.circuitbreaker.instances.emailGateway.minimumNumberOfCalls=3",
        "resilience4j.circuitbreaker.instances.emailGateway.failureRateThreshold=50",
        "resilience4j.circuitbreaker.instances.emailGateway.waitDurationInOpenState=30s"
})
class OutboundResilienceIT {

    @Autowired
    private SmsService smsService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockitoBean(name = "smsRestTemplate")
    private RestTemplate restTemplate;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void resetCircuitAndMock() {
        circuitBreakerRegistry.circuitBreaker("smsGateway").reset();
        circuitBreakerRegistry.circuitBreaker("emailGateway").reset();
        reset(restTemplate, mailSender);
    }

    @Test
    void opensCircuitAfterRepeatedProviderFailuresAndStopsFurtherRequests() {
        doThrow(new ResourceAccessException("simulated provider timeout"))
                .when(restTemplate)
                .postForEntity(anyString(), any(HttpEntity.class), eq(String.class));

        for (int attempt = 0; attempt < 3; attempt++) {
            assertThatThrownBy(() -> smsService.sendSms("0712345678", "Test notification"))
                    .isInstanceOf(ResourceAccessException.class);
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("smsGateway");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> smsService.sendSms("0712345678", "Test notification"))
                .isInstanceOf(CallNotPermittedException.class);

        // Sending notifications is not idempotent, so one request is made per call
        // and the circuit breaker blocks later calls without replaying the message.
        verify(restTemplate, times(3))
                .postForEntity(anyString(), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void opensEmailCircuitWithoutRetryingNonIdempotentMessages() {
        doThrow(new MailSendException("simulated SMTP timeout"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        for (int attempt = 0; attempt < 3; attempt++) {
            assertThatThrownBy(() -> emailService.sendPasswordResetEmail(
                    "student@example.test", "https://example.test/reset"))
                    .isInstanceOf(MailSendException.class);
        }

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("emailGateway");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> emailService.sendPasswordResetEmail(
                "student@example.test", "https://example.test/reset"))
                .isInstanceOf(CallNotPermittedException.class);

        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
    }

}
