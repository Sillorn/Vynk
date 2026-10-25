package com.vynk.profiles;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProfileEducationRepository extends JpaRepository<ProfileEducation, Long> {

    List<ProfileEducation> findByProfileIdOrderBySortOrderAsc(Long profileId);

    @Modifying
    @Query("delete from ProfileEducation item where item.profile.id = :profileId")
    void deleteByProfileId(Long profileId);
}
