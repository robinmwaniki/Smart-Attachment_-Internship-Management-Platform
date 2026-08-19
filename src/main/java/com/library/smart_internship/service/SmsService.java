package com.library.smart_internship.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class SmsService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${africastalking.api-key:}")
    private String apiKey;

    @Value("${africastalking.username:sandbox}")
    private String username;

    @Value("${africastalking.base-url:https://api.sandbox.africastalking.com/version1/messaging}")
    private String baseUrl;

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void sendSms(String phoneNumber, String message) {
        if (apiKey == null || apiKey.isBlank() || phoneNumber == null || phoneNumber.isBlank()) {
            System.out.println("SMS skipped (missing API key or phone number): " + message);
            return;
        }

        String normalizedPhone = normalizePhoneNumber(phoneNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.set("apiKey", apiKey);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
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