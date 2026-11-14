package hr.algebra.parkalot.models.enums;

public enum EmissionClass {
    EURO_1(1),
    EURO_2(2),
    EURO_3(3),
    EURO_4(4),
    EURO_5(5),
    EURO_6(6),
    EURO_7(7);

    private final int level;

    EmissionClass(int level) {
        this.level = level;
    }

    public boolean meetsRequirement(EmissionClass minimum) {
        return this.level >= minimum.level;
    }
}
