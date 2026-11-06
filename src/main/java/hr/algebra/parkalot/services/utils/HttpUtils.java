package hr.algebra.parkalot.services.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpUtils {
    public static final String UNAUTHORIZED_DEFAULT_MESSAGE = "Unauthorized";

    public static ResponseEntity<ProblemDetail> produceErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, message));
    }

    public static ResponseEntity<ProblemDetail> produceBadRequestResponse(String message) {
        return produceErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    public static ResponseEntity<ProblemDetail> produceUnauthorizedResponse() {
        return produceUnauthorizedResponse(UNAUTHORIZED_DEFAULT_MESSAGE);
    }

    public static ResponseEntity<ProblemDetail> produceUnauthorizedResponse(String message) {
        return produceErrorResponse(HttpStatus.UNAUTHORIZED, message);
    }

    public static ResponseEntity<ProblemDetail> produceForbiddenResponse(String message) {
        return produceErrorResponse(HttpStatus.FORBIDDEN, message);
    }

    public static ResponseEntity<ProblemDetail> produceNotFoundResponse(String message) {
        return produceErrorResponse(HttpStatus.NOT_FOUND, message);
    }

    public static ResponseEntity<ProblemDetail> produceInternalErrorResponse(String message) {
        return produceErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public static ResponseEntity<ProblemDetail> produceConflictErrorResponse(String message) {
        return produceErrorResponse(HttpStatus.CONFLICT, message);
    }

    public static ResponseEntity<Void> produceOkResponse() {
        return ResponseEntity.ok().build();
    }

    public static ResponseEntity<?> produceOkResponseWithBody(Object body) {
        return ResponseEntity.ok(body);
    }
}
