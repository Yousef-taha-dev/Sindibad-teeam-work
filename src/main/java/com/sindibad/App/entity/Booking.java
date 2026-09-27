package com.sindibad.App.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;


import java.time.LocalDateTime;

@Entity
@Table(name = "booking")
@Getter
@Setter
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long bookingId;

    @ManyToOne(fetch =FetchType.LAZY,optional = false )
    @JoinColumn(
            name = "trip_id",
            referencedColumnName = "trip_id",
            nullable = false
    )
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = true)
    private Guide guide;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(
            name = "tourist_id",
            referencedColumnName = "tourist_id",
            nullable = false
    )
    private Tourist tourist;

    @Column(name = "booking_date",nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private LocalDateTime bookingDate;


    @Column(name = "number_of_guests", nullable = false)
    private Integer numberOfGuests;

    @Column(name = "booking_status", length = 30)
    @ColumnDefault("'Open'")
    private String bookingStatus = "Open";

}
