package hr.algebra.parkalot.exceptions;

import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;

public class LocalizedInternalException extends LocalizedException {
    public LocalizedInternalException(LocalizedExceptionKeys messageKey, Object... arguments) {
        super(messageKey, arguments);
    }

    public LocalizedInternalException(LocalizedExceptionKeys messageKey) {
        super(messageKey);
    }
}
