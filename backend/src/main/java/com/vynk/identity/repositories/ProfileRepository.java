package com.vynk.identity.repositories;

import com.vynk.identity.entities.Profile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserAccountUsernameIgnoreCase(String username);
}
