package com.sindibad.App.repository;

import com.sindibad.App.entity.Notifications;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationsRepository extends JpaRepository<Notifications, Long> {

    List<Notifications> findByPersonPersonId(Long personId);

    Optional<Notifications> findByNotificationIdAndPersonPersonId(
            Long notificationId,
            Long personId
    );
}