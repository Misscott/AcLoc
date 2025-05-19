package com.example.acloc.service;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface RoleService {
    /**
     * Obtains all roles from database
     * @param token Bearer token
     * @return JsonObject containing all roles
     */
    @GET("roles")
    Call<JsonObject> getRoles(@Header("Authorization") String token);
}
