package com.vynk.profiles.repositories;

import com.vynk.profiles.entities.ProfileExperience;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProfileExperienceRepository extends JpaRepository<ProfileExperience, Long> {

    List<ProfileExperience> findByProfileIdOrderBySortOrderAsc(Long profileId);

    @Modifying
    @Query("delete from ProfileExperience item where item.profile.id = :profileId")
    void deleteByProfileId(Long profileId);
}
