package com.mediminder.dto;

import com.mediminder.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank(message = "Name darf nicht leer sein") String name,
            @NotBlank(message = "E-Mail darf nicht leer sein") @Email(message = "Keine gültige E-Mail-Adresse") String email,
            @NotBlank @Size(min = 8, message = "Passwort braucht mindestens 8 Zeichen") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "E-Mail darf nicht leer sein") String email,
            @NotBlank(message = "Passwort darf nicht leer sein") String password) {
    }

    public record UserDto(Long id, String name, String email) {
        public static UserDto from(User user) {
            return new UserDto(user.getId(), user.getName(), user.getEmail());
        }
    }

    public record AuthResponse(String token, UserDto user) {
    }
}
