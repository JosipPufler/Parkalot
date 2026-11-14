package hr.algebra.parkalot.configs;

import hr.algebra.parkalot.exceptions.*;
import hr.algebra.parkalot.services.utils.HttpUtils;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.io.ParseException;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.postgresql.util.PSQLException;

import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
public class RestExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(ParseException.class)
    public ResponseEntity<ProblemDetail> handleParseException(ParseException e) {
        return HttpUtils.produceInternalErrorResponse(e.getMessage());
    }

    @ExceptionHandler(CurrentUserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCurrentUserNotFound(CurrentUserNotFoundException e) {
        return HttpUtils.produceUnauthorizedResponse(e.getMessage());
    }

    @ExceptionHandler(LocalizedNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            LocalizedNotFoundException e,
            Locale locale) {

        String message = messageSource.getMessage(e.getMessageKey().getKey(), e.getArguments(), locale);

        return HttpUtils.produceNotFoundResponse(message);
    }

    @ExceptionHandler(PSQLException.class)
    public ResponseEntity<ProblemDetail> handlePSQLException(PSQLException e) {
        return HttpUtils.produceConflictErrorResponse(e.getMessage());
    }

    @ExceptionHandler(LocalizedConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(
            LocalizedNotFoundException e,
            Locale locale) {

        String message = messageSource.getMessage(e.getMessageKey().getKey(), e.getArguments(), locale);

        return HttpUtils.produceConflictErrorResponse(message);
    }

    @ExceptionHandler(LocalizedBadRequestException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(
            LocalizedBadRequestException e,
            Locale locale) {

        String message = messageSource.getMessage(e.getMessageKey().getKey(), e.getArguments(), locale);

        return HttpUtils.produceBadRequestResponse(message);
    }
}