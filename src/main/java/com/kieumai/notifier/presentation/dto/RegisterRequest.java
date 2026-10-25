package com.kieumai.notifier.presentation.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class RegisterRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;
    @NotBlank(message = "Password không được để trống")
    private String password;
    private String display_name;


    public RegisterRequest() {
    }

    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public RegisterRequest(String username, String password, String role, String display_name, LocalDateTime created_At) {
        this.username = username;
        this.password = password;
        this.display_name = display_name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public static RegisterRequestBuilder builder() {
        return new RegisterRequestBuilder();
    }

    public static class RegisterRequestBuilder {
        private String username;
        private String password;

        public RegisterRequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        public RegisterRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public RegisterRequest build() {
            return new RegisterRequest(username, password);
        }
    }
}
