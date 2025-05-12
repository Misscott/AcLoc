package com.example.acloc.interfaces;

import com.google.gson.JsonObject;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {
    @POST("signin")
    Call<JsonObject> registerUser(@Body JsonObject userData);

    @POST("login")
    Call<JsonObject> loginUser(@Body JsonObject body);

    @PUT("users/{uuid}")
    Call<JsonObject> updateUser(
            @Header("Authorization") String bearerToken,
            @Path("uuid") String uuid,
            @Body JsonObject body
    );

    @POST("login")
    Call<JsonObject> verifyOldPassword(@Body JsonObject loginBody);

    @PUT("users/{uuid}")
    Call<JsonObject> changePassword(
            @Header("Authorization") String token,
            @Path("uuid") String uuid,
            @Body JsonObject body
    );

    @POST("places")
    Call<JsonObject> insertPlace(
            @Header("Authorization") String token,
            @Body JsonObject body
    );

    @POST("report_types")
    Call<JsonObject> insertReportType(
            @Header("Authorization") String token,
            @Body JsonObject body
    );

    @PUT("places/{uuid}")
    Call<JsonObject> updatePlace(@Header("Authorization") String token,
                                 @Path("uuid") String uuid,
                                 @Body JsonObject placeData);


    @GET("places")
    Call<ResponseBody> getAllPlaces(@Header("Authorization") String token);

    @POST("users/{user_uuid}/places")
    Call<JsonObject> addPlaceToFavorites(
            @Header("Authorization") String token,
            @Path("user_uuid") String userUuid,
            @Body JsonObject placeBody
    );

    @DELETE("users/{user_uuid}/places/{place_uuid}")
    Call<Void> removePlaceFromFavorites(
            @Header("Authorization") String token,
            @Path("user_uuid") String userUuid,
            @Path("place_uuid") String placeUuid
    );

    @GET("users/{user_uuid}/places")
    Call<JsonObject> getFavoritePlaces(
            @Header("Authorization") String token,
            @Path("user_uuid") String userUuid
    );

    @PUT("users/{user_uuid}/places")
    Call<JsonObject> restorePlaceToFavorites(
            @Header("Authorization") String authToken,
            @Path("user_uuid") String userUuid,
            @Body JsonObject body
    );

    @POST("reports")
    Call<JsonObject> insertReport(
            @Header("Authorization") String token,
            @Body JsonObject reportBody
    );

    @GET("users/{user_uuid}/reports")
    Call<JsonObject> getUserReports(
            @Header("Authorization") String token,
            @Path("user_uuid") String userUuid
    );

    @DELETE("reports/{report_uuid}")
    Call<Void> removeReport(
            @Header("Authorization") String token,
            @Path("report_uuid") String reportUuid
            );

    @PUT("/reports/{uuid}")
    Call<JsonObject> updateReport(@Header("Authorization") String token,
                                 @Path("uuid") String uuid,
                                 @Body JsonObject placeData);

    @GET("places/{place_uuid}/reports")
    Call<JsonObject> getPlaceReports(
            @Header("Authorization") String token,
            @Path("place_uuid") String placeUuid
    );

    @GET("users/{user_uuid}/places")
    Call<JsonObject> getUserFavorites(
            @Header("Authorization") String token,
            @Path("user_uuid") String userUuid
    );

    @GET("roles")
    Call<JsonObject> getRoles(@Header("Authorization") String token);

    @GET("users")
    Call<JsonObject> getAllUsers(@Header("Authorization") String token);


    @PUT("users/{uuid}")
    Call<JsonObject> updateRole(@Header("Authorization") String token,
                                  @Path("uuid") String uuid,
                                  @Body JsonObject userData);

    @GET("places/{uuid}")
    Call<JsonObject> getPlaceFromUuid(
            @Header("Authorization") String token,
            @Path("uuid") String uuid
    );
}
