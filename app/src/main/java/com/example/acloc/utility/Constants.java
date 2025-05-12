package com.example.acloc.utility;

import java.util.Arrays;
import java.util.List;

public class Constants {
    public static final String SHARED_PREF = "ACLOC";
    public static final String ADMIN = "admin";
    public static final String VIEWER = "viewer";
    public static final String PLACE = "Place";
    public static final String REPORT = "Report";
    public static final String SOMETHING_WENT_WRONG = "Something went wrong";

    public static final int GOOD_RATING = 3;
    public static final int AVERAGE_RATING = 2;
    public static final int BAD_RATING = 1;

    public static final List<String> ROLES_OPTIONS = Arrays.asList("admin", "viewer");
    public static final String BASE_URL = "https://locationapi-m13l.onrender.com/";
}
