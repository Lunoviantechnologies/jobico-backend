package com.example.jobico.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.company.name:Jobico}")
    private String companyName;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    // ─── Generic Send 

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("[EmailService] Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    // ─── 1. Status: SHORTLISTED ───────────────────────────────────────────────

    @Async
    public void sendShortlistedEmail(String to, String candidateName, String role) {
        String subject = "Congratulations! You've been Shortlisted – " + companyName;
        String body = buildHtml("Application Shortlisted 🎉", candidateName,
                "We are pleased to inform you that your application for the position of <b>" + role + "</b> "
                + "has been <b style='color:#2e7d32;'>shortlisted</b>.",
                "Our HR team will reach out to you shortly with further details regarding the next steps.",
                null, null);
        sendHtmlEmail(to, subject, body);
    }

    // ─── 2. Status: SELECTED ─────────────────────────────────────────────────

    @Async
    public void sendSelectedEmail(String to, String candidateName, String role) {
        String subject = "You've been Selected! – " + companyName;
        String body = buildHtml("Application Selected ✅", candidateName,
                "We are thrilled to let you know that you have been <b style='color:#1565c0;'>selected</b> "
                + "for the position of <b>" + role + "</b> at " + companyName + ".",
                "Your offer letter will be shared with you shortly. Please keep an eye on your email.",
                null, null);
        sendHtmlEmail(to, subject, body);
    }

    // ─── 3. Status: REJECTED ─────────────────────────────────────────────────

    @Async
    public void sendRejectedEmail(String to, String candidateName, String role) {
        String subject = "Application Update – " + companyName;
        String body = buildHtml("Application Status Update", candidateName,
                "Thank you for your interest in the <b>" + role + "</b> position at " + companyName + ".",
                "After careful consideration, we regret to inform you that we will not be moving forward "
                + "with your application at this time. We appreciate the time you invested and encourage "
                + "you to apply for future openings that match your profile.",
                null, null);
        sendHtmlEmail(to, subject, body);
    }

    // ─── 4. Offer Letter Email ────────────────────────────────────────────────

    @Async
    public void sendOfferLetterEmail(String to, String candidateName, String role,
                                      double salary, LocalDate joiningDate) {
        String subject = "Offer Letter – " + role + " at " + companyName;
        String details =
                "<table style='width:100%;border-collapse:collapse;margin:16px 0;'>" +
                "<tr><td style='padding:8px;background:#f5f5f5;font-weight:bold;width:40%;'>Designation</td>" +
                "<td style='padding:8px;'>" + role + "</td></tr>" +
                "<tr><td style='padding:8px;background:#f5f5f5;font-weight:bold;'>Joining Date</td>" +
                "<td style='padding:8px;'>" + DATE_FMT.format(joiningDate) + "</td></tr>" +
                "<tr><td style='padding:8px;background:#f5f5f5;font-weight:bold;'>CTC (Annual)</td>" +
                "<td style='padding:8px;'>₹ " + String.format("%,.2f", salary) + "</td></tr>" +
                "</table>";
        String body = buildHtml("Offer Letter 📄", candidateName,
                "We are pleased to offer you the position of <b>" + role + "</b> at <b>" + companyName + "</b>.",
                "Please find the details of your appointment below:" + details +
                "Kindly reply to this email confirming your acceptance.",
                null, null);
        sendHtmlEmail(to, subject, body);
    }

    // ─── 5. Employee Onboarding Welcome ──────────────────────────────────────

    @Async
    public void sendOnboardingWelcomeEmail(String to, String employeeName, String employeeId,
                                            String department, LocalDate joiningDate) {
        String subject = "Welcome to " + companyName + "! Your Onboarding Details";
        String details =
                "<table style='width:100%;border-collapse:collapse;margin:16px 0;'>" +
                "<tr><td style='padding:8px;background:#f5f5f5;font-weight:bold;width:40%;'>Employee ID</td>" +
                "<td style='padding:8px;'>" + employeeId + "</td></tr>" +
                "<tr><td style='padding:8px;background:#f5f5f5;font-weight:bold;'>Department</td>" +
                "<td style='padding:8px;'>" + department + "</td></tr>" +
                "<tr><td style='padding:8px;background:#f5f5f5;font-weight:bold;'>Joining Date</td>" +
                "<td style='padding:8px;'>" + DATE_FMT.format(joiningDate) + "</td></tr>" +
                "</table>";
        String body = buildHtml("Welcome to the Team! 🎊", employeeName,
                "We are excited to have you join <b>" + companyName + "</b>. Your onboarding has been completed successfully.",
                "Here are your onboarding details:" + details +
                "Please report to the HR desk on your joining date. Our team will guide you through the rest.",
                null, null);
        sendHtmlEmail(to, subject, body);
    }

    // ─── 6. Payslip Email ────────────────────────────────────────────────────

    @Async
    public void sendPayslipEmail(String to, String employeeName, String employeeId,
                                  int month, int year, double basicSalary, double hra,
                                  double allowances, double deductions, double netSalary) {
        String[] monthNames = {"", "January", "February", "March", "April", "May", "June",
                               "July", "August", "September", "October", "November", "December"};
        String monthYear = monthNames[month] + " " + year;
        String subject = "Payslip for " + monthYear + " – " + companyName;

        String details =
                "<table style='width:100%;border-collapse:collapse;margin:16px 0;'>" +
                "<tr style='background:#1565c0;color:#fff;'>" +
                "<th style='padding:10px;text-align:left;'>Component</th>" +
                "<th style='padding:10px;text-align:right;'>Amount (₹)</th></tr>" +
                "<tr><td style='padding:8px;border-bottom:1px solid #eee;'>Basic Salary</td>" +
                "<td style='padding:8px;text-align:right;border-bottom:1px solid #eee;'>" + String.format("%,.2f", basicSalary) + "</td></tr>" +
                "<tr><td style='padding:8px;border-bottom:1px solid #eee;'>HRA</td>" +
                "<td style='padding:8px;text-align:right;border-bottom:1px solid #eee;'>" + String.format("%,.2f", hra) + "</td></tr>" +
                "<tr><td style='padding:8px;border-bottom:1px solid #eee;'>Allowances</td>" +
                "<td style='padding:8px;text-align:right;border-bottom:1px solid #eee;'>" + String.format("%,.2f", allowances) + "</td></tr>" +
                "<tr style='color:#c62828;'><td style='padding:8px;border-bottom:1px solid #eee;'>Deductions</td>" +
                "<td style='padding:8px;text-align:right;border-bottom:1px solid #eee;'>- " + String.format("%,.2f", deductions) + "</td></tr>" +
                "<tr style='background:#e8f5e9;font-weight:bold;font-size:15px;'>" +
                "<td style='padding:10px;'>Net Salary</td>" +
                "<td style='padding:10px;text-align:right;color:#2e7d32;'>₹ " + String.format("%,.2f", netSalary) + "</td></tr>" +
                "</table>";

        String body = buildHtml("Payslip – " + monthYear, employeeName,
                "Your salary for <b>" + monthYear + "</b> has been processed. Employee ID: <b>" + employeeId + "</b>",
                details + "The net salary has been credited to your registered bank account.",
                null, null);
        sendHtmlEmail(to, subject, body);
    }

    // ─── 7. Experience Letter Email ───────────────────────────────────────────

    @Async
    public void sendExperienceLetterEmail(String to, String employeeName, String role,
                                           String department, LocalDate joiningDate) {
        String subject = "Experience Letter – " + companyName;
        String body = buildHtml("Experience Letter 📋", employeeName,
                "Please find your Experience Letter from <b>" + companyName + "</b> attached.",
                "Your service as <b>" + role + "</b> in the <b>" + department + "</b> department "
                + "from <b>" + DATE_FMT.format(joiningDate) + "</b> is duly acknowledged. "
                + "We wish you the very best in your future endeavors.",
                null, null);
        sendHtmlEmail(to, subject, body);
    }

    // ─── 8. Applied Confirmation ──────────────────────────────────────────────

    @Async
    public void sendApplicationReceivedEmail(String to, String candidateName, String role) {
        String subject = "Application Received – " + companyName;
        String body = buildHtml("Application Received ✔", candidateName,
                "Thank you for applying for the <b>" + role + "</b> position at <b>" + companyName + "</b>.",
                "We have received your application and our team will review it shortly. "
                + "You will be notified of any updates via email.",
                null, null);
        sendHtmlEmail(to, subject, body);
    }

    // ─── 9. Password Reset Email ──────────────────────────────────────────────

    @Async
    public void sendPasswordResetEmail(String to, String resetLink) {
        String subject = "Reset Your Password – " + companyName;
        String body = buildHtml("Password Reset Request 🔐", "Admin",
                "We received a request to reset the password for your <b>" + companyName + "</b> admin account.",
                "Click the button below to reset your password. This link is valid for <b>15 minutes</b>.<br><br>"
                + "If you did not request a password reset, please ignore this email — your password will remain unchanged.",
                "Reset Password", resetLink);
        sendHtmlEmail(to, subject, body);
    }

    // ─── 10. Password Changed Confirmation ───────────────────────────────────

    @Async
    public void sendPasswordChangedEmail(String to) {
        String subject = "Your Password Has Been Changed – " + companyName;
        String body = buildHtml("Password Changed ✅", "Admin",
                "Your <b>" + companyName + "</b> admin account password was successfully changed.",
                "If you did not make this change, please contact support immediately or use the "
                + "\"Forgot Password\" option to regain access to your account.",
                null, null);
        sendHtmlEmail(to, subject, body);
    }

    // ─── HTML Template Builder ────────────────────────────────────────────────

    private String buildHtml(String heading, String recipientName,
                              String intro, String body,
                              String ctaLabel, String ctaLink) {
        String cta = "";
        if (ctaLabel != null && ctaLink != null) {
            cta = "<div style='text-align:center;margin:24px 0;'>"
                + "<a href='" + ctaLink + "' style='background:#1565c0;color:#fff;"
                + "padding:12px 28px;border-radius:4px;text-decoration:none;"
                + "font-size:14px;font-weight:bold;'>" + ctaLabel + "</a></div>";
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body "
                + "style='margin:0;padding:0;font-family:Arial,sans-serif;background:#f4f4f4;'>"
                + "<table width='100%' style='background:#f4f4f4;padding:32px 0;'><tr><td align='center'>"
                + "<table width='600' style='background:#ffffff;border-radius:8px;"
                + "overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);'>"

                // Header
                + "<tr><td style='background:#1565c0;padding:28px 32px;'>"
                + "<h1 style='color:#fff;margin:0;font-size:22px;'>" + companyName + "</h1></td></tr>"

                // Body
                + "<tr><td style='padding:32px;'>"
                + "<h2 style='color:#1a1a1a;font-size:20px;margin-top:0;'>" + heading + "</h2>"
                + "<p style='color:#444;font-size:15px;'>Dear <b>" + recipientName + "</b>,</p>"
                + "<p style='color:#444;font-size:15px;line-height:1.6;'>" + intro + "</p>"
                + "<div style='color:#444;font-size:15px;line-height:1.6;'>" + body + "</div>"
                + cta
                + "<p style='color:#888;font-size:13px;margin-top:32px;'>Best regards,<br>"
                + "<b>HR Team</b><br>" + companyName + "</p></td></tr>"

                // Footer
                + "<tr><td style='background:#f9f9f9;padding:16px 32px;text-align:center;"
                + "border-top:1px solid #eee;'>"
                + "<p style='color:#aaa;font-size:12px;margin:0;'>This is an automated email from "
                + companyName + ". Please do not reply to this email.</p></td></tr>"

                + "</table></td></tr></table></body></html>";
    }
 // ─── Send Email with PDF Attachment ──────────────────────────────────────────

    @Async
    public void sendEmailWithPdfAttachment(String to, String subject,
                                            String htmlBody, byte[] pdfBytes,
                                            String pdfFilename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.addAttachment(pdfFilename,
                    new org.springframework.core.io.ByteArrayResource(pdfBytes),
                    "application/pdf");
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("[EmailService] Failed to send attachment email to " + to + ": " + e.getMessage());
        }
    }

    // Email body builder

    public String buildOfferLetterEmailBody(String candidateName, String role,
                                             double salary, java.time.LocalDate joiningDate,
                                             String company) {
        String details =
                "<table style='width:100%;border-collapse:collapse;margin:16px 0;'>" +
                "<tr><td style='padding:8px;background:#f5f5f5;font-weight:bold;width:40%;'>Designation</td>" +
                "<td style='padding:8px;'>" + role + "</td></tr>" +
                "<tr><td style='padding:8px;background:#f5f5f5;font-weight:bold;'>Joining Date</td>" +
                "<td style='padding:8px;'>" + DATE_FMT.format(joiningDate) + "</td></tr>" +
                "<tr><td style='padding:8px;background:#f5f5f5;font-weight:bold;'>CTC (Annual)</td>" +
                "<td style='padding:8px;'>&#8377; " + String.format("%,.2f", salary) + "</td></tr>" +
                "</table>";
        return buildHtml("Offer Letter \uD83D\uDCC4", candidateName,
                "We are delighted to offer you the position of <b>" + role + "</b> at <b>" + company + "</b>.",
                "Please find your <b>Offer Letter attached</b> to this email." + details +
                "Kindly review the letter and reply with your acceptance.",
                null, null);
    }

    public String buildExperienceLetterEmailBody(String employeeName, String role,
                                                  String department, java.time.LocalDate joiningDate,
                                                  String company) {
        return buildHtml("Experience Letter \uD83D\uDCCB", employeeName,
                "Please find your <b>Experience Letter</b> from <b>" + company + "</b> attached to this email.",
                "Your service as <b>" + role + "</b> in the <b>" + department + "</b> department " +
                "from <b>" + DATE_FMT.format(joiningDate) + "</b> is duly acknowledged. " +
                "We wish you the very best in your future endeavors.",
                null, null);
    }
}