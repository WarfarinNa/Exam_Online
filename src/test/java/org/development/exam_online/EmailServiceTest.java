package org.development.exam_online;

import org.development.exam_online.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"dev", "email"})
public class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @Test
    public void testEmailServiceBeanExists() {
        // This test verifies that the EmailService bean is properly created
        // and the JavaMailSender dependency is resolved
        assert emailService != null;
        System.out.println("EmailService bean created successfully!");
    }
}