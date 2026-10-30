package hr.algebra.parkalot.configs;

import hr.algebra.parkalot.exceptions.CurrentUserNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class MvcExceptionHandler {

    @ExceptionHandler(CurrentUserNotFoundException.class)
    public String handleCurrentUserNotFound() {
        return "redirect:/mvc/public/auth/login";
    }
}
