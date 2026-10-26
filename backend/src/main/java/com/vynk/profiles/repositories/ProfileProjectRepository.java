package com.vynk.profiles.repositories;

import com.vynk.profiles.entities.ProfileProject;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProfileProjectRepository extends JpaRepository<ProfileProject, Long> {

    List<ProfileProject> findByProfileIdOrderBySortOrderAsc(Long profileId);

    @Modifying
    @Query("delete from ProfileProject item where item.profile.id = :profileId")
    void deleteByProfileId(Long profileId);
}
