package com.example.acloc.api;

import com.example.acloc.service.AuthService;
import com.example.acloc.service.FavoriteService;
import com.example.acloc.service.PlaceService;
import com.example.acloc.service.ReportService;
import com.example.acloc.service.ReportTypeService;
import com.example.acloc.service.RoleService;
import com.example.acloc.service.UploadService;
import com.example.acloc.service.UserService;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Centralized API client that manages Retrofit instance and provides access to all service interfaces
 */
public class LocationApiClient {
    private static LocationApiClient instance;
    private final Retrofit retrofit;

    // Service interface instances
    private AuthService authService;
    private FavoriteService favoriteService;
    private PlaceService placeService;
    private ReportService reportService;
    private ReportTypeService reportTypeService;
    private RoleService roleService;
    private UserService userService;
    private UploadService uploadService;

    // Private constructor for singleton pattern
    private LocationApiClient() {
        this.retrofit = RetrofitProvider.getInstance();
    }

    /**
     * Gets the singleton instance of RetrofitClient
     * @return RetrofitClient instance
     */
    public static synchronized LocationApiClient getInstance() {
        if (instance == null) {
            instance = new LocationApiClient();
        }
        return instance;
    }

    /**
     * Gets the AuthService interface
     * @return AuthService implementation
     */
    public AuthService getAuthService() {
        if (authService == null) {
            authService = retrofit.create(AuthService.class);
        }
        return authService;
    }

    /**
     * Gets the FavoriteService interface
     * @return FavoriteService implementation
     */
    public FavoriteService getFavoriteService() {
        if (favoriteService == null) {
            favoriteService = retrofit.create(FavoriteService.class);
        }
        return favoriteService;
    }

    /**
     * Gets the PlaceService interface
     * @return PlaceService implementation
     */
    public PlaceService getPlaceService() {
        if (placeService == null) {
            placeService = retrofit.create(PlaceService.class);
        }
        return placeService;
    }

    /**
     * Gets the ReportService interface
     * @return ReportService implementation
     */
    public ReportService getReportService() {
        if (reportService == null) {
            reportService = retrofit.create(ReportService.class);
        }
        return reportService;
    }

    /**
     * Gets the ReportTypeService interface
     * @return ReportTypeService implementation
     */
    public ReportTypeService getReportTypeService() {
        if (reportTypeService == null) {
            reportTypeService = retrofit.create(ReportTypeService.class);
        }
        return reportTypeService;
    }

    /**
     * Gets the RoleService interface
     * @return RoleService implementation
     */
    public RoleService getRoleService() {
        if (roleService == null) {
            roleService = retrofit.create(RoleService.class);
        }
        return roleService;
    }

    /**
     * Gets the UserService interface
     * @return UserService implementation
     */
    public UserService getUserService() {
        if (userService == null) {
            userService = retrofit.create(UserService.class);
        }
        return userService;
    }

    public UploadService getUploadService(){
        if(uploadService == null){
            uploadService = retrofit.create(UploadService.class);
        }
        return uploadService;
    }
}
