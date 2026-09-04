package com.library.smart_internship;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "SPRING_MAIL_HOST=smtp.test.com",
        "SPRING_MAIL_PORT=587",
        "SPRING_MAIL_USERNAME=test@test.com",
        "SPRING_MAIL_PASSWORD=testpassword",
        "SPRING_DATASOURCE_URL=jdbc:h2:mem:testdb",
        "SPRING_DATASOURCE_USERNAME=sa",
        "SPRING_DATASOURCE_PASSWORD=password",
        "SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop",
        "SPRING_JPA_SHOW_SQL=true"
})
class SmartAttachmentInternshipManagementPlatformApplicationTests {

    @Test
    void contextLoads() {
    }
}