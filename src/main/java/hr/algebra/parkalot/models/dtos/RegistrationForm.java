package hr.algebra.parkalot.models.dtos;

public record RegistrationForm (String firstName, String lastName, String password, String confirmPassword, String email, HouseholdDto household){
    public RegistrationForm (){
        this("", "", "", "", "",
                new HouseholdDto(0L, "", "", "", "", ""));
    }
}
