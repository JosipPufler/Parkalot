package hr.algebra.parkalot.exceptions;

import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;

public class LocalizedConflictException extends LocalizedException {

    public LocalizedConflictException(LocalizedExceptionKeys messageKey, Object... arguments) {
        super(messageKey, arguments);
    }

    public LocalizedConflictException(LocalizedExceptionKeys messageKey) {
        super(messageKey);
    }
}
