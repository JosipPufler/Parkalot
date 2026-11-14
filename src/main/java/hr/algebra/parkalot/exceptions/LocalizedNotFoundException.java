package hr.algebra.parkalot.exceptions;

import hr.algebra.parkalot.models.enums.LocalizedExceptionKeys;
import lombok.Getter;

@Getter
public class LocalizedNotFoundException extends LocalizedException {

    public LocalizedNotFoundException(LocalizedExceptionKeys messageKey, Object... arguments) {
        super(messageKey, arguments);
    }

    public LocalizedNotFoundException(LocalizedExceptionKeys messageKey) {
        super(messageKey);
    }
}
