package com.drc.aidbridge.data.remote.dto.request;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("full_name")
    private final String name;

    @SerializedName("email")
    private final String email;

    @SerializedName("phone_number")
    private final String phone;

    @SerializedName("password")
    private final String password;

    @SerializedName("role")
    private final String role;

    public RegisterRequest(String name, String email, String phone, String password, String role) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public String getRole() {
        return role;
    }
}
