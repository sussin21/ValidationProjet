package com.example.app.services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class MailService {

    public void sendMail(String recipient, String subject, String body) throws Exception {
        String host = requireEnv("SMTP_HOST");
        String username = requireEnv("SMTP_USERNAME");
        String password = requireEnv("SMTP_PASSWORD");
        String from = getEnvOrDefault("SMTP_FROM", username);
        int port = Integer.parseInt(getEnvOrDefault("SMTP_PORT", "587"));
        boolean startTls = Boolean.parseBoolean(getEnvOrDefault("SMTP_STARTTLS", "true"));
        boolean ssl = Boolean.parseBoolean(getEnvOrDefault("SMTP_SSL", "false"));

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", String.valueOf(port));
        properties.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        properties.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        properties.put("mail.mime.charset", StandardCharsets.UTF_8.name());

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient, false));
        message.setSubject(subject == null ? "" : subject, StandardCharsets.UTF_8.name());
        message.setText(body == null ? "" : body, StandardCharsets.UTF_8.name());

        Transport.send(message);
    }

    private String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing SMTP configuration: " + key);
        }
        return value.trim();
    }

    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}