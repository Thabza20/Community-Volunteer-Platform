package com.error404.communityvolunteerplatform.helpers;

import android.os.Handler;
import android.os.Looper;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * GmailOtpHelper
 * ─────────────────────────────────────────────────────────────────
 * Sends OTP emails via Gmail SMTP using an App Password.
 * Requires android-mail dependency in build.gradle.kts:
 *   implementation("com.sun.mail:android-mail:1.6.7")
 *   implementation("com.sun.mail:android-activation:1.6.7")
 */
public class GmailOtpHelper {

    // ── Gmail credentials ────────────────────────────────────────
    private static final String GMAIL_ADDRESS  = "noreply.dutsut@gmail.com";
    private static final String GMAIL_APP_PASS = "pwonmjzkescnbhhz"; // App Password (no spaces)
    private static final String SENDER_NAME    = "Community Volunteer Platform";

    // OTP expires after 10 minutes
    private static final long OTP_EXPIRY_MS = 10 * 60 * 1000L;

    private String currentOtp;
    private long   otpGeneratedAt;

    private final ExecutorService executor    = Executors.newSingleThreadExecutor();
    private final Handler         mainHandler = new Handler(Looper.getMainLooper());

    public interface EmailCallback {
        void onSuccess();
        void onFailure(String error);
    }

    // ── Step 1: Generate OTP ─────────────────────────────────────
    public String generateOtp() {
        currentOtp     = String.format("%06d", new Random().nextInt(1_000_000));
        otpGeneratedAt = System.currentTimeMillis();
        return currentOtp;
    }

    // ── Step 2: Send OTP via Gmail SMTP ─────────────────────────
    public void sendOtp(String recipientEmail, String recipientName, EmailCallback callback) {
        if (currentOtp == null) {
            callback.onFailure("OTP not generated. Call generateOtp() first.");
            return;
        }

        executor.execute(() -> {
            try {
                // SMTP properties for Gmail TLS
                Properties props = new Properties();
                props.put("mail.smtp.auth",                "true");
                props.put("mail.smtp.starttls.enable",     "true");
                props.put("mail.smtp.host",                "smtp.gmail.com");
                props.put("mail.smtp.port",                "587");
                props.put("mail.smtp.ssl.trust",           "smtp.gmail.com");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(GMAIL_ADDRESS, GMAIL_APP_PASS);
                    }
                });

                // Build the email
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(GMAIL_ADDRESS, SENDER_NAME));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(recipientEmail));
                message.setSubject("Your OTP Code – Community Volunteer Platform");
                message.setContent(buildEmailHtml(currentOtp, recipientName), "text/html; charset=utf-8");

                Transport.send(message);

                mainHandler.post(callback::onSuccess);

            } catch (Exception e) {
                mainHandler.post(() -> callback.onFailure("Email error: " + e.getMessage()));
            }
        });
    }

    // ── Step 3: Verify entered OTP ───────────────────────────────
    public boolean verifyOtp(String entered) {
        if (currentOtp == null || entered == null) return false;
        boolean expired = (System.currentTimeMillis() - otpGeneratedAt) > OTP_EXPIRY_MS;
        if (expired) return false;
        return currentOtp.equals(entered.trim());
    }

    public boolean isExpired() {
        return currentOtp == null ||
                (System.currentTimeMillis() - otpGeneratedAt) > OTP_EXPIRY_MS;
    }

    public void invalidate() {
        currentOtp = null;
    }

    // ── Email HTML template ──────────────────────────────────────
    private String buildEmailHtml(String otp, String name) {
        String displayName = (name != null && !name.isEmpty()) ? name : "there";
        return "<!DOCTYPE html><html><body style='font-family:sans-serif;padding:24px;'>" +
                "<h2 style='color:#4A148C;'>Community Volunteer Platform</h2>" +
                "<p>Hi " + displayName + ",</p>" +
                "<p>Your one-time verification code is:</p>" +
                "<div style='font-size:36px;font-weight:bold;letter-spacing:8px;" +
                "color:#4A148C;padding:16px;background:#F3E5F5;border-radius:8px;" +
                "display:inline-block;margin:8px 0;'>" + otp + "</div>" +
                "<p style='color:#666;font-size:13px;'>This code expires in <strong>10 minutes</strong>." +
                " Do not share it with anyone.</p>" +
                "<hr style='border:none;border-top:1px solid #eee;margin-top:24px;'/>" +
                "<p style='color:#aaa;font-size:11px;'>If you did not request this, ignore this email.</p>" +
                "</body></html>";
    }
}