package hr.algebra.parkalot.exceptions;

import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
import lombok.Getter;

@Getter
public class LocalizedException extends RuntimeException {
    private final LocalizedExceptionKeys messageKey;
    private final Object[] arguments;

    public LocalizedException(LocalizedExceptionKeys messageKey, Object... arguments) {
        this.messageKey = messageKey;
        this.arguments = arguments;
    }

    public LocalizedException(LocalizedExceptionKeys messageKey) {
        this.messageKey = messageKey;
        arguments = new Object[]{};
    }
}
