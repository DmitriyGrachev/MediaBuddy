package org.hrachov.com.filmproject.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        System.out.println("Отправка письма на: " + to); // Логирование
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("javageminisprin@gmail.com");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
