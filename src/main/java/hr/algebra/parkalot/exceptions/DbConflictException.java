package hr.algebra.parkalot.exceptions;

public class DbConflictException extends RuntimeException {
    public DbConflictException(String message) {
        super(message);
    }
}
