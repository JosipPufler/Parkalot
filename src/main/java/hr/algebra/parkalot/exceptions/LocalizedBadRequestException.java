package hr.algebra.parkalot.exceptions;

import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;

public class LocalizedBadRequestException extends LocalizedException {

    public LocalizedBadRequestException(LocalizedExceptionKeys messageKey, Object... arguments) {
        super(messageKey, arguments);
    }

    public LocalizedBadRequestException(LocalizedExceptionKeys messageKey) {
        super(messageKey);
    }
}
