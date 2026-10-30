package hr.algebra.parkalot.exceptions;

public class CurrentUserNotFoundException extends RuntimeException {

    public CurrentUserNotFoundException(String message) {
        super(message);
    }
}
