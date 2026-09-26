package com.sindibad.App.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.LocalDate;

@Entity
@Table(name="guide")
@Getter
@Setter
public class Guide {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="guide_id")
    private Long guideId;

    @OneToOne(optional = false)
    @JoinColumn(
            name = "person_id",
            referencedColumnName = "person_id" ,
            nullable = false,
            unique = true
    )
    private Person person;

    @Column(name="bio",nullable = false,columnDefinition = "TEXT")
    private String bio;

    @Column (name="experience_years",nullable = false)
    private Integer experienceYears;

    @Column(name="approval_status",nullable = false,length = 20)
    @ColumnDefault("'Pending'")
    private String approvalStatus="Pending";

    @Column(name="available_date")
    private LocalDate availableDate;

    @Column(name="Start_time")
    private LocalDate startTime;

    @Column(name="end_time")
    private LocalDate endTime;

}
