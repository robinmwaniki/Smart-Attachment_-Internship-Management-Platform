package com.library.smart_internship.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SmsService {

    @Qualifier("smsRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${africastalking.api-key:}")
    private String apiKey;

    @Value("${africastalking.username:sandbox}")
    private String username;

    @Value("${africastalking.base-url:https://api.sandbox.africastalking.com/version1/messaging}")
    private String baseUrl;

    /**
     * SMS submission is not idempotent: an ambiguous network timeout can occur
     * after the provider accepted the message. Do not automatically retry and
     * risk sending duplicate messages; use timeouts, a circuit breaker and a
     * bounded concurrent-call bulkhead instead.
     */
    @CircuitBreaker(name = "smsGateway")
    @Bulkhead(name = "smsGateway")
    public void sendSms(String phoneNumber, String message) {
        if (apiKey == null || apiKey.isBlank() || phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }

        String normalizedPhone = normalizePhoneNumber(phoneNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.set("apiKey", apiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", username);
        body.add("to", normalizedPhone);
        body.add("message", message);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(baseUrl, request, String.class);
    }

    private String normalizePhoneNumber(String phone) {
        String cleaned = phone.trim().replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("0")) {
            return "+254" + cleaned.substring(1);
        }
        if (!cleaned.startsWith("+")) {
            return "+" + cleaned;
        }
        return cleaned;
    }
}
