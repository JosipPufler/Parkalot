package hr.algebra.parkalot.models.dto;

public record RegistrationForm (String username, String password, String confirmPassword, String email, String firstName, String lastName){
    public RegistrationForm (){
        this("", "", "", "", "", "");
    }
}
