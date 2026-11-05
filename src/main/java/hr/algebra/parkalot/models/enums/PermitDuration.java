package hr.algebra.parkalot.models.enums;

import lombok.Getter;

import java.time.LocalDate;
import java.util.function.UnaryOperator;

public enum PermitDuration {
    DAY(1., date -> date.plusDays(1)),
    WEEK(6., date -> date.plusDays(7)),
    MONTH(25., date -> date.plusMonths(1)),
    YEAR(330., date -> date.plusYears(1));

    PermitDuration(Double multiplier, UnaryOperator<LocalDate> function) {
        this.multiplier = multiplier;
        this.calculateEndDate = function;
    }

    @Getter
    private final Double multiplier;
    public final UnaryOperator<LocalDate> calculateEndDate;
}
