package com.sindibad.App.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name="tourist")
@Getter
@Setter
public class Tourist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="tourist_id")
    private Long touristId;

    @OneToOne(optional = false)
    @JoinColumn (
            name="person_id",
            referencedColumnName = "person_id",
            nullable = false,
            unique = true
    )
    private Person person;

    @Column(name="preferred_language",length=50)
    @ColumnDefault("'English'")
    private String preferred_language="English";
}
