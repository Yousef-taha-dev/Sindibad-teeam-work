package com.sindibad.App.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admin")
@Getter
@Setter
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name ="admin")
    private long adminId;
    @OneToOne
    @JoinColumn(
            name="person_id",
            referencedColumnName = "person_id",
            nullable = false,
            unique = true
    )
    private Person person;

}
