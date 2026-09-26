package com.sindibad.App.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name="notifications")
@Getter
@Setter

public class Notifications {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="notification_id")
    private long notificationId;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(
            name = "person_id",
            referencedColumnName = "person_id",
            unique = false
    )
    private Person person;

    @Column(name="title",length = 255)
    private String title;

    @Column (name="message",nullable = false,columnDefinition = "TEXT")
    private String message;

    @Column(name="notification_type",length = 50)
    private String notificationType;
}
