package com.sindibad.App.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="trips")
@Getter
@Setter
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name="trip_id")
    private long tripId

            ;
@ManyToOne(fetch=FetchType.LAZY,optional = false)
    @JoinColumn(
            name="guide_id",
            referencedColumnName = "guide_id",
            nullable = false
    )
    private Guide guide;


@ManyToOne(fetch = FetchType.LAZY,optional = false)
@JoinColumn(
        name="category_id",
        referencedColumnName = "category_id",
        nullable = false
)
    private TripCategory category;

@Column(name="title",nullable = false,length = 200)
    private String title;


@Column(name="description",nullable = false,columnDefinition = "TEXT")
    private String description;


@Column(name="start_date",nullable = false)
    private LocalDateTime startDate;

    @Column(name="end_date",nullable = false)
    private LocalDateTime endDate;


    @Column(name = "max_participants",nullable = false)
    private  Integer maxParticipants;


    @Column(name = "price_per_person",nullable = false,precision = 10,scale = 2)
    private BigDecimal pricePerPerson;


    @Column(name = "trip_status", length = 30)
    @ColumnDefault("'Trip New'")
    private String tripStatus = "Trip New";

    @Column(name = "trip_image_url", length = 1000)
    private String tripImageUrl;
    @Column(name = "trip_image_public_id", length = 255)
    private String tripImagePublicId;
}
