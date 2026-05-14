package com.example.jobico.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends OTP SMS via smslogin.co HTTP API.
 * <p>
 * Official FAQ uses {@code GET} to {@code /v3/api.php} with query params:
 * {@code username}, {@code apikey}, {@code senderid}, {@code mobile}, {@code message}, {@code templateid}.
 * Use {@code smslogin.request-style=GET_QUERY} (default in prod profile).
 */
@Service
public class SmsLoginService {

    private static final Logger log = LoggerFactory.getLogger(SmsLoginService.class);

    private final RestTemplate restTemplate;

    @Value("${smslogin.username:}")
    private String username;

    @Value("${smslogin.password:}")
    private String password;

    @Value("${smslogin.api-key:}")
    private String apiKey;

    @Value("${smslogin.base-url:https://smslogin.co}")
    private String baseUrl;

    @Value("${smslogin.send-path:/v3/api.php}")
    private String sendPath;

    /** If set, overrides base-url + send-path */
    @Value("${smslogin.full-url:}")
    private String fullUrl;

    @Value("${smslogin.mobile-prefix:91}")
    private String mobilePrefix;

    @Value("${smslogin.message-template:Your OTP is {otp}}")
    private String messageTemplate;

    /** JSON_POST | FORM_POST | GET_QUERY — smslogin FAQ: GET_QUERY */
    @Value("${smslogin.request-style:GET_QUERY}")
    private String requestStyle;

    /** DLT-approved 6-char sender id (FAQ: senderid) */
    @Value("${smslogin.sender-id:}")
    private String senderId;

    /** DLT template id (FAQ: templateid) */
    @Value("${smslogin.template-id:}")
    private String templateId;

    /** If true, adds password to GET query (FAQ send-SMS example does not use password) */
    @Value("${smslogin.include-password-query:false}")
    private boolean includePasswordInQuery;

    @Value("${smslogin.field.username:username}")
    private String fieldUsername;

    @Value("${smslogin.field.password:password}")
    private String fieldPassword;

    @Value("${smslogin.field.api-key:apikey}")
    private String fieldApiKey;

    @Value("${smslogin.field.mobile:mobile}")
    private String fieldMobile;

    @Value("${smslogin.field.message:message}")
    private String fieldMessage;

    /** FAQ uses senderid */
    @Value("${smslogin.field.sender:senderid}")
    private String fieldSender;

    @Value("${smslogin.field.template:templateid}")
    private String fieldTemplate;

    public SmsLoginService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String sendOtp(String mobile10Digits, String otp) {
        if (username == null || username.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "SMSLOGIN: set SMSLOGIN_USERNAME and SMSLOGIN_API_KEY in environment");
        }

        String style = requestStyle == null ? "GET_QUERY" : requestStyle.trim().toUpperCase();
        boolean needPassword = "JSON_POST".equals(style) || "FORM_POST".equals(style);
        if (needPassword && (password == null || password.isBlank())) {
            throw new IllegalStateException(
                    "SMSLOGIN: password required for request-style=" + style + " (set SMSLOGIN_PASSWORD)");
        }

        if ("GET_QUERY".equals(style) && (senderId == null || senderId.isBlank())) {
            throw new IllegalStateException(
                    "SMSLOGIN: DLT sender id required for GET_QUERY (set SMSLOGIN_SENDER_ID)");
        }
        if ("GET_QUERY".equals(style) && (templateId == null || templateId.isBlank())) {
            throw new IllegalStateException(
                    "SMSLOGIN: DLT template id required for GET_QUERY (set SMSLOGIN_TEMPLATE_ID)");
        }

        String url = resolveSendUrl();
        String message = messageTemplate.replace("{otp}", otp);
        String mobile = (mobilePrefix == null ? "" : mobilePrefix.trim()) + mobile10Digits;

        log.info("SMSLOGIN: style={} url={}", style, maskPasswordInUrl(url));

        try {
            return switch (style) {
                case "GET_QUERY" -> sendGetQuery(url, mobile, message);
                case "FORM_POST" -> sendFormPost(url, mobile, message);
                case "JSON_POST" -> sendJsonPost(url, mobile, message);
                default -> throw new IllegalStateException(
                        "SMSLOGIN: unknown smslogin.request-style=" + requestStyle
                                + " (use JSON_POST, FORM_POST, or GET_QUERY)");
            };
        } catch (RestClientException e) {
            log.error("SMSLOGIN: request failed: {}", e.getMessage());
            throw e;
        }
    }

    private String sendJsonPost(String url, String mobile, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("apikey", apiKey);
        body.put("mobile", mobile);
        body.put("message", message);
        if (senderId != null && !senderId.isBlank()) {
            body.put(fieldSender, senderId.trim());
        }
        if (templateId != null && !templateId.isBlank()) {
            body.put(fieldTemplate, templateId.trim());
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        log.info("SMSLOGIN: POST JSON {}", url);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        logResponse(response);
        return response.getBody();
    }

    private String sendFormPost(String url, String mobile, String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(fieldUsername, username);
        form.add(fieldPassword, password);
        form.add(fieldApiKey, apiKey);
        form.add(fieldMobile, mobile);
        form.add(fieldMessage, message);
        if (senderId != null && !senderId.isBlank()) {
            form.add(fieldSender, senderId.trim());
        }
        if (templateId != null && !templateId.isBlank()) {
            form.add(fieldTemplate, templateId.trim());
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        log.info("SMSLOGIN: POST FORM {}", url);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        logResponse(response);
        return response.getBody();
    }

    private String sendGetQuery(String url, String mobile, String message) {
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(url)
                .queryParam(fieldUsername, username)
                .queryParam(fieldApiKey, apiKey)
                .queryParam(fieldMobile, mobile)
                .queryParam(fieldMessage, message);

        if (includePasswordInQuery && password != null && !password.isBlank()) {
            b.queryParam(fieldPassword, password);
        }
        b.queryParam(fieldSender, senderId.trim());
        b.queryParam(fieldTemplate, templateId.trim());

        // build(false) then encode — do NOT use build(true); it skips encoding and rejects spaces in message
        String fullUrl = b.build(false)
                .encode(StandardCharsets.UTF_8)
                .toUriString();
        log.info("SMSLOGIN: GET {}", maskUrl(fullUrl));
        ResponseEntity<String> response = restTemplate.getForEntity(fullUrl, String.class);
        logResponse(response);
        return response.getBody();
    }

    private String resolveSendUrl() {
        if (fullUrl != null && !fullUrl.isBlank()) {
            return fullUrl.trim();
        }
        String normalizedBase = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        String path = sendPath == null || sendPath.isBlank() ? "/v3/api.php" : sendPath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return normalizedBase + path;
    }

    private static String maskPasswordInUrl(String url) {
        return url.replaceAll("(?i)([?&]password=)[^&]*", "$1***");
    }

    private static String maskUrl(String url) {
        return maskPasswordInUrl(url);
    }

    private void logResponse(ResponseEntity<String> response) {
        String body = response.getBody();
        if (body != null && body.length() > 500) {
            body = body.substring(0, 500) + "...";
        }
        log.info("SMSLOGIN: status={} body={}", response.getStatusCode(), body);
    }
}
