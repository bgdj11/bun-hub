package com.example.onlybunsbe.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PetCareLocationDTO {

    private Long id;
    private String name;

    private String country;
    private String city;
    private String address;
    private int number;

    private double latitude;
    private double longitude;
}
