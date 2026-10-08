package hr.algebra.parkalot.services.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpUtils {
    public static ResponseEntity<ProblemDetail> produceErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, message));
    }

    public static ResponseEntity<ProblemDetail> produceBadRequestResponse(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message));
    }

    public static ResponseEntity<ProblemDetail> produceUnauthorizedResponse(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, message));
    }

    public static ResponseEntity<ProblemDetail> produceForbiddenResponse(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, message));
    }

    public static ResponseEntity<ProblemDetail> produceNotFoundResponse(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message));
    }
}
