package com.example.jobico.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP store; SMS via Fast2SMS or SMSLOGIN (configured by {@code otp.sms.provider}).
 */
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private static final long OTP_EXPIRY_MS = 5 * 60 * 1000L;
    private static final long RESEND_COOLDOWN_MS = 30 * 1000L;

    private record OtpEntry(String otp, long generatedAt) {}

    private final ConcurrentHashMap<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Value("${otp.sms.enabled:false}")
    private boolean smsEnabled;

    /** none | fast2sms | smslogin */
    @Value("${otp.sms.provider:none}")
    private String smsProvider;

    @Value("${fast2sms.api.key:}")
    private String fast2smsApiKey;

    @Autowired
    private SmsLoginService smsLoginService;

    @Autowired
    private RestTemplate restTemplate;

    /** Generate & store OTP */
    public String generateOtp(String mobile) {
        log.info("OtpService: generateOtp mobile={} smsEnabled={} provider={}", mobile, smsEnabled, smsProvider);

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

        if (smsEnabled) {
            sendSms(mobile, otp);
        } else {
            log.warn("OTP (SMS disabled) for {}: {}", mobile, otp);
        }

        return otp;
    }

    /** Verify OTP */
    public boolean verifyOtp(String mobile, String otp) {
        OtpEntry entry = store.get(mobile);
        if (entry == null) {
            return false;
        }

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

    private void sendSms(String mobile, String otp) {
        String p = smsProvider == null ? "none" : smsProvider.trim().toLowerCase();
        try {
            log.info("Sending OTP SMS to {} via provider={}", mobile, p);
            switch (p) {
                case "smslogin" -> smsLoginService.sendOtp(mobile, otp);
                case "fast2sms" -> sendFast2sms(mobile, otp);
                case "none" -> log.warn("otp.sms.enabled=true but otp.sms.provider=none; skipping SMS");
                default -> log.error("Unknown otp.sms.provider={}; set to smslogin or fast2sms", smsProvider);
            }
        } catch (Exception e) {
            log.error("SMS FAILED for {} — OTP was generated but delivery failed: {}", mobile, e.getMessage());
            log.debug("SMS failure detail", e);
        }
    }

    private void sendFast2sms(String mobile, String otp) {
        if (fast2smsApiKey == null || fast2smsApiKey.isBlank()) {
            log.error("FAST2SMS: missing fast2sms.api.key / FAST2SMS_API_KEY");
            return;
        }
        String url = "https://www.fast2sms.com/dev/bulk";
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", fast2smsApiKey);
        headers.set("Content-Type", "application/x-www-form-urlencoded");
        String body = "sender_id=TXTIND"
                + "&message=Your OTP is " + otp
                + "&language=english"
                + "&route=v3"
                + "&numbers=" + mobile;
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        String response = restTemplate.postForObject(url, request, String.class);
        log.info("FAST2SMS response: {}", response);
    }
}
