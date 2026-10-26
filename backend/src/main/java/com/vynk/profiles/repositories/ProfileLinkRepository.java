package com.vynk.profiles.repositories;

import com.vynk.profiles.entities.ProfileLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProfileLinkRepository extends JpaRepository<ProfileLink, Long> {

    List<ProfileLink> findByProfileIdOrderBySortOrderAsc(Long profileId);

    @Modifying
    @Query("delete from ProfileLink item where item.profile.id = :profileId")
    void deleteByProfileId(Long profileId);
}
