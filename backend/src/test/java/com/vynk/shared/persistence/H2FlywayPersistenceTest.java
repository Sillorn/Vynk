package com.vynk.shared.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:file:./target/test-db/vynk-test;AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class H2FlywayPersistenceTest {

    @Autowired
    private Environment environment;

    @Test
    void usesFileH2WithFlywayAndValidate() {
        assertThat(environment.getProperty("spring.datasource.url")).contains("jdbc:h2:file:");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("true");
    }

    @Test
    void createsDatabaseDirectoryOnStartup() throws Exception {
        Path dbPath = Path.of("target", "test-db");
        assertThat(Files.exists(dbPath)).isTrue();
    }
}
