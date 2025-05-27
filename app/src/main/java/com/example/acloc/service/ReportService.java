package com.example.acloc.service;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ReportService {
    /**
     * Obtains all reports from a place
     * @param token Bearer token
     * @param placeUuid Place UUID
     * @return JsonObject containing all reports
     */
    @GET("places/{place_uuid}/reports")
    Call<JsonObject> getPlaceReports(
            @Header("Authorization") String token,
            @Path("place_uuid") String placeUuid
    );

    /**
     * Obtains a user's reports
     * @param token Bearer token
     * @param userUuid User UUID
     * @return JSON Object containing report information
     */
    @GET("users/{user_uuid}/reports")
    Call<JsonObject> getUserReports(
            @Header("Authorization") String token,
            @Path("user_uuid") String userUuid
    );

    /**
     * Inserts a new report
     * @param token Bearer token
     * @param reportBody report data
     * @return JsonObject containing report information
     */
    @POST("reports")
    Call<JsonObject> insertReport(
            @Header("Authorization") String token,
            @Body JsonObject reportBody
    );

    /**
     * Deletes a report from database (soft delete)
     * @param token Bearer token
     * @param reportUuid Report UUID
     * @return Void (No response)
     */
    @DELETE("reports/{report_uuid}")
    Call<Void> removeReport(
            @Header("Authorization") String token,
            @Path("report_uuid") String reportUuid
    );

    /**
     * Updates an existing report in database
     * @param token Bearer token
     * @param uuid Report UUID
     * @param reportData Report data containing parameters to modify
     * @return JsonObject containing updated report information
     */
    @PUT("reports/{uuid}")
    Call<JsonObject> updateReport(
            @Header("Authorization") String token,
            @Path("uuid") String uuid,
            @Body JsonObject reportData
    );
}
