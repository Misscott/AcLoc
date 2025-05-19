package com.example.acloc.service;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {
    /**
     * User registration endpoint
     * @param userData User data
     * @return JsonObject containing user information from signup
     */
    @POST("signin")
    Call<JsonObject> registerUser(@Body JsonObject userData);

    /**
     * User login endpoint
     * @param body Login data (username and password)
     * @return JsonObject containing user information from login
     */
    @POST("login")
    Call<JsonObject> loginUser(@Body JsonObject body);

    /**
     * Verify old password endpoint (specifically made for password reset)
     * @param loginBody Login data (username and password)
     * @return JsonObject containing user information from login
     */
    @POST("login")
    Call<JsonObject> verifyOldPassword(@Body JsonObject loginBody);
}
