package com.example.onlybunsbe.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "care_locations")
public class PetCareLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long externalId;   // ID iz druge aplikacije
    private String name;

    private String country;
    private String city;
    private String address;
    private int number;

    private double latitude;
    private double longitude;
}
