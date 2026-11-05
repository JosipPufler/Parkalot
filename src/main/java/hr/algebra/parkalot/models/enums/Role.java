package hr.algebra.parkalot.models.enums;

public enum Role {
    USER,
    OFFICER,
    ADMIN;

    public String getRoleName(){
        return name();
    }
}
