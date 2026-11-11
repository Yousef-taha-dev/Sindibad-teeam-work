package com.sindibad.App.entity;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name="person")
@Getter
@Setter
public class Person {
    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "person_id")
    private Long personId;

    @Column(nullable = false,length = 50)
    private String firstName;

    @Column(nullable=false,length = 50)
    private String lastName;

    @Column(nullable = false,unique = true,length = 100)
    private String email;

    @Column(nullable = false,length = 255)
    private  String password;

    @Column (unique = true,length = 20)
    private String phoneNumber;

    @Column(length = 20)
    private String gender ;

    private java.time.LocalDate dateOfBirth;

    @Column(length = 50)
    private String nationality;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "profile_image_public_id", length = 255)
    private String profileImagePublicId;

    @Column(name = "account_status", length = 20)
    @ColumnDefault("'Active'")
    private String accountStatus = "Active";

}
