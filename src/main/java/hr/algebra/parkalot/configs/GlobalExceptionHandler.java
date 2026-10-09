package hr.algebra.parkalot.configs;

import hr.algebra.parkalot.services.utils.HttpUtils;
import org.locationtech.jts.io.ParseException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ParseException.class)
    public ResponseEntity<ProblemDetail> handleParseException(ParseException e) {
        return HttpUtils.produceInternalErrorResponse("Failed to parse area polygon");
    }
}