package com.example.jobico.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

@Service
public class SmsLoginService {

    private static final Logger log =
            LoggerFactory.getLogger(SmsLoginService.class);

    @Value("${smslogin.username}")
    private String username;

    @Value("${smslogin.apikey}")
    private String apiKey;

    @Value("${smslogin.senderid}")
    private String senderId;

    @Value("${smslogin.templateid}")
    private String templateId;

    public void sendOtp(
            String mobile,
            String otp
    ) {

        try {

            String message =
                    "Welcome to Jobico! Your secure verification code is: "
                            + otp
                            + " Enter this OTP to continue your login/signup process. "
                            + "This code expires in 5 minutes.";

            String encodedMessage =
                    URLEncoder.encode(
                            message,
                            "UTF-8"
                    );

            String apiUrl =
                    "https://smslogin.co/v3/api.php?"
                            + "username=" + username
                            + "&apikey=" + apiKey
                            + "&mobile=" + mobile
                            + "&senderid=" + senderId
                            + "&message=" + encodedMessage
                            + "&templateid=" + templateId;

            URL url = new URL(apiUrl);

            HttpURLConnection connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod("GET");

            int responseCode =
                    connection.getResponseCode();

            log.info(
                    "SMS Response Code : {}",
                    responseCode
            );

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getInputStream()
                            )
                    );

            String inputLine;

            StringBuilder response =
                    new StringBuilder();

            while ((inputLine = in.readLine()) != null) {

                response.append(inputLine);
            }

            in.close();

            log.info(
                    "SMS Response : {}",
                    response
            );

            connection.disconnect();

        } catch (Exception e) {

            log.error(
                    "SMS sending failed : {}",
                    e.getMessage()
            );

            throw new RuntimeException(
                    "Failed to send OTP SMS"
            );
        }
    }
}