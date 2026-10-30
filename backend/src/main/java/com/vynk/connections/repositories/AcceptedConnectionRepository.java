package com.vynk.connections.repositories;

import com.vynk.connections.entities.AcceptedConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AcceptedConnectionRepository extends JpaRepository<AcceptedConnection, AcceptedConnection.Key> {
    @Query(value = "SELECT u.username FROM accepted_connection c JOIN user_account u ON u.id = CASE WHEN c.user_low_id = :userId THEN c.user_high_id ELSE c.user_low_id END WHERE c.user_low_id = :userId OR c.user_high_id = :userId ORDER BY u.username_lower", nativeQuery = true)
    java.util.List<String> findContacts(@Param("userId") long userId);

    @org.springframework.data.jpa.repository.Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN TRUE ELSE FALSE END FROM accepted_connection WHERE user_low_id = :low AND user_high_id = :high", nativeQuery = true)
    boolean existsPair(@org.springframework.data.repository.query.Param("low") long low,
                       @org.springframework.data.repository.query.Param("high") long high);

    @Modifying
    @Query(value = "DELETE FROM accepted_connection WHERE user_low_id = :low AND user_high_id = :high", nativeQuery = true)
    int deletePair(@Param("low") long low, @Param("high") long high);
}
