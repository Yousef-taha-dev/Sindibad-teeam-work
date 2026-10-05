package com.sindibad.App.service;

import com.sindibad.App.dto.NotificationResponseDTO;
import com.sindibad.App.entity.Notifications;
import com.sindibad.App.entity.Person;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.mapper.NotificationMapper;
import com.sindibad.App.repository.NotificationsRepository;
import com.sindibad.App.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationsRepository notificationsRepository;
    private final PersonRepository personRepository;
    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(
            NotificationsRepository notificationsRepository,
            PersonRepository personRepository,
            NotificationMapper notificationMapper) {

        this.notificationsRepository = notificationsRepository;
        this.personRepository = personRepository;
        this.notificationMapper = notificationMapper;
    }

    // ==================== Get My Notifications ====================

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> getMyNotifications(String email) {

        Person person = getPersonByEmail(email);

        List<Notifications> notifications =
                notificationsRepository.findByPersonPersonId(
                        person.getPersonId()
                );

        return notificationMapper.toDtoList(notifications);
    }

    // ==================== Delete My Notification ====================

    @Override
    @Transactional
    public void deleteMyNotification(
            Long notificationId,
            String email) {

        Person person = getPersonByEmail(email);

        Notifications notification =
                notificationsRepository
                        .findByNotificationIdAndPersonPersonId(
                                notificationId,
                                person.getPersonId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Notification not found"
                                )
                        );

        notificationsRepository.delete(notification);
    }

    // ==================== Create Notification ====================

    @Override
    @Transactional
    public void createNotification(
            Long personId,
            String title,
            String message,
            String notificationType) {

        Person person = personRepository.findById(personId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Person not found with ID: " + personId
                        )
                );

        Notifications notification = new Notifications();

        notification.setPerson(person);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(notificationType);

        notificationsRepository.save(notification);
    }

    // ==================== Helper ====================

    private Person getPersonByEmail(String email) {

        return personRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Person not found with email: " + email
                        )
                );
    }
}