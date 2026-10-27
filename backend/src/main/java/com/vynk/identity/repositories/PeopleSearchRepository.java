package com.vynk.identity.repositories;

import com.vynk.identity.entities.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PeopleSearchRepository extends JpaRepository<Profile, Long> {

    @Query("""
            select p from Profile p
            join p.userAccount u
            where u.status = com.vynk.identity.UserAccountStatus.ACTIVE
              and (:query = ''
                or lower(p.firstName) like lower(concat('%', :query, '%'))
                or lower(p.lastName) like lower(concat('%', :query, '%'))
                or lower(p.headline) like lower(concat('%', :query, '%'))
                or exists (select s.id from ProfileSkill s
                           where s.profile = p and lower(s.name) like lower(concat('%', :query, '%'))))
            order by u.usernameLower asc, p.id asc
            """)
    Page<Profile> search(@Param("query") String query, Pageable pageable);
}
