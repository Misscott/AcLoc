package com.example.acloc.service;

import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PlaceService {
    /**
     * Inserts a new place into database
     * @param token Bearer token
     * @param body Place data
     * @return JsonObject containing place information
     */
    @POST("places")
    Call<JsonObject> insertPlace(
            @Header("Authorization") String token,
            @Body JsonObject body
    );

    /**
     * Updates an existing place in the database
     * @param token Bearer token
     * @param uuid Place UUID
     * @param placeData Place data
     * @return JsonObject containing place information
     */
    @PUT("places/{uuid}")
    Call<JsonObject> updatePlace(
            @Header("Authorization") String token,
            @Path("uuid") String uuid,
            @Body JsonObject placeData
    );

    /**
     * Obtains all places from database
     * @param token Bearer token
     * @return JsonObject containing all places
     */
    @GET("places")
    Call<ResponseBody> getAllPlaces(@Header("Authorization") String token);

    /**
     * Obtains all places that match the query
     * @param token Bearer token
     * @param query Search term to be matched
     * @return JsonObject containing all places that match the query
     */
    @GET("places")
    Call<ResponseBody> getSearchPlaces(@Header("Authorization") String token,
                                       @Query("query") String query);

    /**
     * Obtains a place from its UUID
     * @param token Bearer token
     * @param uuid Place UUID
     * @return JsonObject containing place information
     */
    @GET("places/{uuid}")
    Call<JsonObject> getPlaceFromUuid(
            @Header("Authorization") String token,
            @Path("uuid") String uuid
    );
}
