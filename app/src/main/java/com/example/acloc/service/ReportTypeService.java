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
import retrofit2.http.Query;

public interface ReportTypeService {

    @GET("report_types")
    Call<JsonObject> getReportTypes(
            @Header("Authorization") String token,
            @Query("uuid") String uuid,
            @Query("name") String name
    );

    @GET("report_types/{uuid}")
    Call<JsonObject> getReportTypeByUuid(
            @Header("Authorization") String token,
            @Path("uuid") String uuid
    );

    @POST("report_types")
    Call<JsonObject> createReportType(
            @Header("Authorization") String token,
            @Body JsonObject body
    );

    @PUT("report_types/{uuid}")
    Call<JsonObject> updateReportType(
            @Header("Authorization") String token,
            @Path("uuid") String uuid,
            @Body JsonObject body
    );

    @DELETE("report_types/{uuid}")
    Call<Void> deleteReportType(
            @Header("Authorization") String token,
            @Path("uuid") String uuid
    );
}
