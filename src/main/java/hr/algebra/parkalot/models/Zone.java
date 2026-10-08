package hr.algebra.parkalot.models;

import lombok.Getter;

@Getter
public enum Zone {
    ZONE_1("Zone I", 15.),
    ZONE_2("Zone II", 10.),
    ZONE_3("Zone III", 5.);

    Zone(String name, Double price){
        this.name = name;
        this.price = price;
    }

    private final String name;
    private final Double price;
}
