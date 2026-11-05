package hr.algebra.parkalot.configs;

import hr.algebra.parkalot.exceptions.CurrentUserNotFoundException;
import hr.algebra.parkalot.services.utils.HttpUtils;
import jakarta.persistence.EntityNotFoundException;
import org.locationtech.jts.io.ParseException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.postgresql.util.PSQLException;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ParseException.class)
    public ResponseEntity<ProblemDetail> handleParseException(ParseException e) {
        return HttpUtils.produceInternalErrorResponse(e.getMessage());
    }

    @ExceptionHandler(CurrentUserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCurrentUserNotFound(CurrentUserNotFoundException e) {
        return HttpUtils.produceUnauthorizedResponse(e.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException e) {
        return HttpUtils.produceNotFoundResponse(e.getMessage());
    }

    @ExceptionHandler(PSQLException.class)
    public ResponseEntity<ProblemDetail> handlePSQLException(PSQLException e) {
        return HttpUtils.produceConflictErrorResponse(e.getMessage());
    }
}