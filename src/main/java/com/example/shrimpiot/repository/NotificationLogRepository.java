package com.example.shrimpiot.repository;

import com.example.shrimpiot.model.NotificationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByDeviceIdOrderByCreatedAtDesc(String deviceId);
    Optional<NotificationLog> findTopByEventKeyAndSuppressedFalseAndStatusInOrderByCreatedAtDesc(String eventKey, Collection<String> statuses);

    @Query("""
            select n from NotificationLog n
            where n.deviceId = :deviceId
              and n.channel = 'APP'
              and (n.recipientUserId = :userId or n.recipientUserId is null)
              and (:unreadOnly = false or n.readFlag = false)
            order by n.createdAt desc
            """)
    List<NotificationLog> findInAppForUserAndDevice(@Param("deviceId") String deviceId,
                                                     @Param("userId") Long userId,
                                                     @Param("unreadOnly") boolean unreadOnly,
                                                     Pageable pageable);

    @Query("""
            select count(n) from NotificationLog n
            where n.deviceId = :deviceId
              and n.channel = 'APP'
              and n.readFlag = false
              and (n.recipientUserId = :userId or n.recipientUserId is null)
            """)
    long countUnreadInAppForUserAndDevice(@Param("deviceId") String deviceId, @Param("userId") Long userId);

    @Query("""
            select n from NotificationLog n
            where n.id = :id
              and n.channel = 'APP'
              and (n.recipientUserId = :userId or n.recipientUserId is null)
            """)
    Optional<NotificationLog> findVisibleInAppById(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("""
            update NotificationLog n
               set n.readFlag = true, n.readAt = :readAt, n.readBy = :readBy
             where n.deviceId = :deviceId
               and n.channel = 'APP'
               and n.readFlag = false
               and (n.recipientUserId = :userId or n.recipientUserId is null)
            """)
    int markAllInAppReadForUserAndDevice(@Param("deviceId") String deviceId,
                                          @Param("userId") Long userId,
                                          @Param("readAt") LocalDateTime readAt,
                                          @Param("readBy") String readBy);
}
