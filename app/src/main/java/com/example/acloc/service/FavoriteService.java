package com.example.acloc.service;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface FavoriteService {
    /**
     * Deletes a place from user's favorite places
     * @param token Bearer token
     * @param userUuid User UUID
     * @param placeUuid Place UUID
     * @return Void (No response)
     */
    @DELETE("users/{user_uuid}/places/{place_uuid}")
    Call<Void> removePlaceFromFavorites(
            @Header("Authorization") String token,
            @Path("user_uuid") String userUuid,
            @Path("place_uuid") String placeUuid
    );

    /**
     * Obtains all favorite places from user
     * @param token Bearer token
     * @param userUuid User UUID
     * @return JsonObject containing all favorite places
     */
    @GET("users/{user_uuid}/places")
    Call<JsonObject> getFavoritePlaces(
            @Header("Authorization") String token,
            @Path("user_uuid") String userUuid
    );

    /**
     * Restores a place to favorite places for user
     * @param authToken Bearer token
     * @param userUuid User UUID
     * @param body Data to modify (favorite)
     * @return JsonObject containing place information
     */
    @PUT("users/{user_uuid}/places")
    Call<JsonObject> restorePlaceToFavorites(
            @Header("Authorization") String authToken,
            @Path("user_uuid") String userUuid,
            @Body JsonObject body
    );
}
