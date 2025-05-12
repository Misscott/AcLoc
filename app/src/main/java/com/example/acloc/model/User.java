package com.example.acloc.model;

import java.io.Serializable;

public class User implements Serializable {
    private String username, email, password;
    private String role, uuid, fkRole ;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getFkRole() {
        return fkRole;
    }

    public void setFkRole(String fkRole) {
        this.fkRole = fkRole;
    }
}
