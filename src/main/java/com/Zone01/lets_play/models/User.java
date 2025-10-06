package com.Zone01.lets_play.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
    @Id
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    @Email
    @Indexed(unique = true)
    private String email;

//    @Size(min = 8, message = "Password must be at least 8 characters long")
//    @Pattern(
//            regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*]).*$",
//            message = "Password must contain at least one number and one special character"
//    )
    private String password;

    private String role;

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
