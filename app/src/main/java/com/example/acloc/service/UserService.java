package com.example.acloc.service;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface UserService {
    /**
     * Updates user data
     * @param bearerToken Bearer token
     * @param uuid User UUID
     * @param body Parameters to modify
     * @return JsonObject containing user information
     */
    @PUT("users/{uuid}")
    Call<JsonObject> updateUser(
            @Header("Authorization") String bearerToken,
            @Path("uuid") String uuid,
            @Body JsonObject body
    );

    /**
     * Changes user password
     * @param token Bearer token
     * @param uuid User UUID
     * @param body Parameters to modify
     * @return JsonObject containing user information
     */
    @PUT("users/{uuid}")
    Call<JsonObject> changePassword(
            @Header("Authorization") String token,
            @Path("uuid") String uuid,
            @Body JsonObject body
    );

    /**
     * Obtains all users from database
     * @param token Bearer token
     * @return JsonObject containing all users
     */
    @GET("users")
    Call<JsonObject> getAllUsers(@Header("Authorization") String token);

    /**
     * Updates user role (only admins)
     * @param token Bearer token
     * @param uuid User UUID
     * @param userData User data to modify (role)
     * @return JsonObject containing user information
     */
    @PUT("users/{uuid}")
    Call<JsonObject> updateRole(
            @Header("Authorization") String token,
            @Path("uuid") String uuid,
            @Body JsonObject userData
    );
}
