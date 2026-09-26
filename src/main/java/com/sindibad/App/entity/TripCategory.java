package com.sindibad.App.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="trip_categories")
@Getter
@Setter
public class TripCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private long categoryId;

    @Column(nullable = false,unique = true,length = 100)
    private String categoryName;

    @Column(name = "description", columnDefinition = "TEXT")
    private  String description;
}
