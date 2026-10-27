package com.kieumai.notifier.presentation.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties({"role", "created_At"})
public class RegisterRequest {
    @NotBlank
    @Size(max = 100)
    private String username;
    @NotBlank
    @Size(min = 8, max = 128)
    private String password;
    @JsonAlias("display_name")
    @Size(max = 255)
    private String displayName;
}
