package com.example.jobico.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP store + Fast2SMS integration
 */
@Service
public class OtpService {

    private static final long OTP_EXPIRY_MS = 5 * 60 * 1000L;      // 5 minutes
    private static final long RESEND_COOLDOWN_MS = 30 * 1000L;     // 30 seconds

    private record OtpEntry(String otp, long generatedAt) {}

    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Value("${otp.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${fast2sms.api.key:}")
    private String apiKey;

    /** Generate & store OTP */
    public String generateOtp(String mobile) {

        // ✅ Validate mobile
        if (mobile == null || !mobile.matches("\\d{10}")) {
            throw new RuntimeException("Invalid mobile number. Must be 10 digits.");
        }

        OtpEntry existing = store.get(mobile);
        if (existing != null) {
            long elapsed = Instant.now().toEpochMilli() - existing.generatedAt();
            if (elapsed < RESEND_COOLDOWN_MS) {
                long remaining = (RESEND_COOLDOWN_MS - elapsed) / 1000;
                throw new RuntimeException("Please wait " + remaining + " seconds before requesting a new OTP.");
            }
        }

        String otp = String.format("%04d", 1000 + random.nextInt(9000));
        store.put(mobile, new OtpEntry(otp, Instant.now().toEpochMilli()));

        // ✅ Send SMS or fallback
        if (smsEnabled) {
            sendSms(mobile, otp);
        } else {
            System.out.println("OTP (DEV MODE) for " + mobile + ": " + otp);
        }

        return otp;
    }

    /** Verify OTP */
    public boolean verifyOtp(String mobile, String otp) {
        OtpEntry entry = store.get(mobile);
        if (entry == null) return false;

        long elapsed = Instant.now().toEpochMilli() - entry.generatedAt();

        if (elapsed > OTP_EXPIRY_MS) {
            store.remove(mobile);
            return false;
        }

        if (entry.otp().equals(otp)) {
            store.remove(mobile);
            return true;
        }

        return false;
    }

    /** Fast2SMS API call */
    private void sendSms(String mobile, String otp) {
        try {
            System.out.println("📩 Sending OTP to: " + mobile);

            String url = "https://www.fast2sms.com/dev/bulk";

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", apiKey);
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String body = "sender_id=TXTIND" +
                          "&message=Your OTP is " + otp +
                          "&language=english" +
                          "&route=v3" +   // ✅ IMPORTANT
                          "&numbers=" + mobile;

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            String response = restTemplate.postForObject(url, request, String.class);

            System.out.println("✅ FAST2SMS RESPONSE: " + response);

        } catch (Exception e) {
            System.out.println("❌ SMS FAILED — fallback OTP: " + otp);
            e.printStackTrace();
        }
    }
}