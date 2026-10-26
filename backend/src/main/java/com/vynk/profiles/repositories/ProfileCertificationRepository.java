package com.vynk.profiles.repositories;

import com.vynk.profiles.entities.ProfileCertification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProfileCertificationRepository extends JpaRepository<ProfileCertification, Long> {

    List<ProfileCertification> findByProfileIdOrderBySortOrderAsc(Long profileId);

    @Modifying
    @Query("delete from ProfileCertification item where item.profile.id = :profileId")
    void deleteByProfileId(Long profileId);
}
