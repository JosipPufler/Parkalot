package hr.algebra.parkalot.models.dto;

public record RegistrationForm (String firstName, String lastName, String password, String confirmPassword, String email){
    public RegistrationForm (){
        this("", "", "", "", "");
    }
}
