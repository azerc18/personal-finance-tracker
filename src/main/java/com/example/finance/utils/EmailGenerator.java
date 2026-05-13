package com.example.finance.utils;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@RequiredArgsConstructor
public class EmailGenerator {
    private final JavaMailSender mailSender;
    @Async
    public  void getEmailReport(String email, String filePath, String fileName) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(email);
        helper.setSubject("Financial Report");
        helper.setText("Please access to this file to view detail report");

        FileSystemResource file = new FileSystemResource(new File(filePath));
        helper.addAttachment(fileName, file);

        mailSender.send(message);
    }
    @Async
    public void sendVerificationEmail(String email, String token) throws MessagingException{
        String link =  "http://localhost:8080/api/auth/verify-email?token="+ token;

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(email);
        helper.setSubject("Verification Email");
        helper.setText("Please click the link to verify your account: \n" +
                link + "\n\n" +
                "Link will expire in 24h\n");

        mailSender.send(message);
    }
}
