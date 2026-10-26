package com.vynk.profiles.repositories;

import com.vynk.profiles.entities.ProfileLanguage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProfileLanguageRepository extends JpaRepository<ProfileLanguage, Long> {

    List<ProfileLanguage> findByProfileIdOrderBySortOrderAsc(Long profileId);

    @Modifying
    @Query("delete from ProfileLanguage item where item.profile.id = :profileId")
    void deleteByProfileId(Long profileId);
}
