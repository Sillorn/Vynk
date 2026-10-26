package com.vynk.identity;

import com.vynk.identity.entities.Profile;
import com.vynk.identity.entities.UserAccount;
import com.vynk.identity.repositories.ProfileRepository;
import com.vynk.identity.repositories.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:file:./target/test-db/vynk-pn009;AUTO_SERVER=FALSE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@Transactional
class IdentitySchemaTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Test
    void createsAccountAndProfileAtomically() {
        UserAccount user = userAccountRepository.save(new UserAccount("alice", "hash"));
        Profile profile = profileRepository.save(new Profile(user, "Alice", "Smith", "Engineer", "Summary"));

        assertThat(userAccountRepository.findById(user.getId())).isPresent();
        assertThat(profileRepository.findByUserAccountUsernameIgnoreCase("alice")).isPresent();
    }

    @Test
    void enforcesLowercaseLetterUsernameFormat() {
        assertThatThrownBy(() -> userAccountRepository.save(new UserAccount("Alice", "hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> userAccountRepository.save(new UserAccount("alice1", "hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> userAccountRepository.save(new UserAccount("ab", "hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> userAccountRepository.save(new UserAccount("a".repeat(31), "hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesCaseInsensitiveUsernameUniqueness() {
        userAccountRepository.save(new UserAccount("bob", "hash1"));
        assertThatThrownBy(() -> userAccountRepository.save(new UserAccount("BOB", "hash2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesProfilePerUser() {
        UserAccount user = userAccountRepository.save(new UserAccount("carol", "hash"));
        profileRepository.save(new Profile(user, "Carol", "Jones", "Dev", "Summary"));
        assertThatThrownBy(() -> profileRepository.save(new Profile(user, "Carol2", "Jones", "Dev", "Summary")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cascadesProfileDeletionOnUserDeletion() {
        UserAccount user = userAccountRepository.save(new UserAccount("dave", "hash"));
        userAccountRepository.flush();
        UserAccount managedUser = userAccountRepository.findById(user.getId()).orElseThrow();
        profileRepository.save(new Profile(managedUser, "Dave", "Brown", "Dev", "Summary"));
        profileRepository.flush();
        profileRepository.deleteAll();
        profileRepository.flush();
        userAccountRepository.delete(managedUser);
        userAccountRepository.flush();
        Optional<Profile> deleted = profileRepository.findByUserAccountUsernameIgnoreCase("dave");
        assertThat(deleted).isEmpty();
    }
}
