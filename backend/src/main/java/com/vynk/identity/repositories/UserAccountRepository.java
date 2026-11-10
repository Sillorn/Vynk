package com.vynk.identity.repositories;

import com.vynk.identity.entities.UserAccount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    /** Id and username only. Loading full accounts also loads each inverse profile one by one. */
    interface UsernameRef {
        Long getId();
        String getUsername();
    }

    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("select u.id as id, u.username as username from UserAccount u where u.id in :ids")
    List<UsernameRef> findUsernameRefsByIdIn(@Param("ids") Collection<Long> ids);
}
