package com.hrachovhistoryservice.microserviceforhistory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String email;
    private String password;

    // Optional: Add more user fields as needed
    private String firstName;
    private String lastName;

    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    // Optional: Add relationships with other entities
    //@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    //private List<Comment> comments;
}