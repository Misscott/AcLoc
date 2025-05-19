package com.example.acloc;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.acloc.activity.ManageRolesActivity;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.dialog.AlertChangePasswordDialog;
import com.example.acloc.dialog.AlertViewOrUpdateProfileDialog;
import com.example.acloc.fragments.FavoriteFragment;
import com.example.acloc.fragments.MapFragment;
import com.example.acloc.fragments.MyReportsFragment;
import com.example.acloc.service.RoleService;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ieslamar.acloc.R;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    // UI Components
    private RelativeLayout rlMainActivity;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;

    // Variables
    private Dialog dialog;
    private Context context;
    private Fragment currentFragment;

    // Animation constants
    private static final int ANIM_ENTER_RIGHT = R.anim.slide_in_right;
    private static final int ANIM_EXIT_LEFT = R.anim.slide_out_left;
    private static final int ANIM_ENTER_LEFT = R.anim.slide_in_left;
    private static final int ANIM_EXIT_RIGHT = R.anim.slide_out_right;
    private static final int ANIM_FADE_IN = android.R.anim.fade_in;
    private static final int ANIM_FADE_OUT = android.R.anim.fade_out;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyLanguageSettings();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initToolbar();
        initUI();
        initObj();
        initListeners();

        // Load default fragment
        loadDefaultFragment(savedInstanceState);
        fetchRoleUuids();
    }

    /**
     * Apply language settings from SharedPreferences
     */
    private void applyLanguageSettings() {
        String savedLanguage = SharedPref.getLanguage(this);
        if (!Locale.getDefault().getLanguage().equals(savedLanguage)) {
            setLocale(savedLanguage);
        }
    }

    /**
     * Initialize toolbar
     */
    private void initToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    /**
     * Initialize UI components
     */
    private void initUI() {
        rlMainActivity = findViewById(R.id.rlMainActivity);
        bottomNavigationView = findViewById(R.id.bottomNavView);
    }

    /**
     * Initialize objects
     */
    private void initObj() {
        context = this;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        if (menu instanceof MenuBuilder) {
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }

        // Show/hide the "Manage Roles" menu item based on user role
        MenuItem manageRolesItem = menu.findItem(R.id.menu_manageRoles);
        boolean isAdmin = "admin".equalsIgnoreCase(SharedPref.getRole(context));
        manageRolesItem.setVisible(isAdmin);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_profile) {
            dialog = new AlertViewOrUpdateProfileDialog(context).openProfileDialog();
            return true;
        } else if (id == R.id.menu_changePassword) {
            dialog = new AlertChangePasswordDialog(context).openChangePasswordDialog();
            return true;
        } else if (id == R.id.menu_changeLanguage) {
            showLanguageChangeDialog();
            return true;
        } else if (id == R.id.menu_manageRoles) {
            Helper.goTo(MainActivity.this, ManageRolesActivity.class);
            return true;
        } else if (id == R.id.menu_logout) {
            DialogUtils.logoutDialog(context).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Show dialog to confirm language change
     */
    private void showLanguageChangeDialog() {
        String currentLanguage = Locale.getDefault().getLanguage();
        String newLanguage = currentLanguage.equals("es") ? "en" : "es";

        AlertDialog dialog = DialogUtils.confirmationDialog(
                this,
                getString(R.string.change_language_confirmation),
                (dialogInterface, i) -> {
                    SharedPref.setLanguage(this, newLanguage);
                    setLocale(newLanguage);
                    restartApp();
                }
        );
        dialog.show();
    }

    /**
     * Restart app to apply language changes
     */
    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Set app locale
     * @param lang Language code (e.g., "en", "es")
     */
    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        getBaseContext().getResources().updateConfiguration(
                config,
                getBaseContext().getResources().getDisplayMetrics()
        );
    }

    /**
     * Initialize listeners for UI components
     */
    private void initListeners() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String title = "";
            int id = item.getItemId();

            if (id == R.id.menu_myReports) {
                selectedFragment = new MyReportsFragment();
                title = getString(R.string.My_Report);
            } else if (id == R.id.menu_map) {
                selectedFragment = new MapFragment();
                title = getString(R.string.Map);
            } else if (id == R.id.menu_favorite) {
                selectedFragment = new FavoriteFragment();
                title = getString(R.string.Favorite);
            }

            if (selectedFragment != null) {
                loadFragmentWithAnimation(selectedFragment, title, getAnimationDirection(selectedFragment));
            }
            return true;
        });
    }

    /**
     * Determine animation direction based on fragment navigation
     * @param newFragment The fragment being navigated to
     * @return Array of animation resource IDs [enter, exit]
     */
    private int[] getAnimationDirection(Fragment newFragment) {
        if (currentFragment == null) {
            return new int[]{ANIM_FADE_IN, ANIM_FADE_OUT};
        }

        // Determine navigation direction based on fragment types
        boolean goingRight = isNavigatingRight(currentFragment, newFragment);

        if (goingRight) {
            return new int[]{ANIM_ENTER_RIGHT, ANIM_EXIT_LEFT};
        } else {
            return new int[]{ANIM_ENTER_LEFT, ANIM_EXIT_RIGHT};
        }
    }

    /**
     * Determine if navigation is going right in the bottom nav
     * @param current Current fragment
     * @param next Fragment being navigated to
     * @return true if navigating right, false if navigating left
     */
    private boolean isNavigatingRight(Fragment current, Fragment next) {
        // Get position in navigation order
        int currentPos = getFragmentPosition(current);
        int nextPos = getFragmentPosition(next);

        return nextPos > currentPos;
    }

    /**
     * Get fragment position in navigation order
     * @param fragment Fragment to check
     * @return Position index (0 for MyReports, 1 for Map, 2 for Favorites)
     */
    private int getFragmentPosition(Fragment fragment) {
        if (fragment instanceof MyReportsFragment) return 0;
        if (fragment instanceof MapFragment) return 1;
        if (fragment instanceof FavoriteFragment) return 2;
        return 1; // Default to middle position
    }

    /**
     * Load fragment with animation
     * @param fragment Fragment to load
     * @param title Title to display in toolbar
     * @param animations Array of animation resource IDs [enter, exit]
     */
    private void loadFragmentWithAnimation(Fragment fragment, String title, int[] animations) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                animations[0], animations[1]
        );
        transaction.replace(R.id.flMainContainer, fragment);
        transaction.commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        currentFragment = fragment;
    }

    /**
     * Load fragment without animation
     * @param fragment Fragment to load
     * @param title Title to display in toolbar
     */
    private void loadFragment(Fragment fragment, String title) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.flMainContainer, fragment);
        transaction.commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        currentFragment = fragment;
    }

    /**
     * Load default fragment if no saved instance state
     * @param savedInstanceState Saved instance state
     */
    private void loadDefaultFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Fragment defaultFragment = new MapFragment();
            String defaultTitle = getString(R.string.Map);

            loadFragment(defaultFragment, defaultTitle);
            bottomNavigationView.setSelectedItemId(R.id.menu_map);
            currentFragment = defaultFragment;
        }
    }

    /**
     * Open fragment from child component
     * @param fragment Fragment to open
     * @param title Title to display
     * @param navItemId Navigation item ID to select
     */
    public void openFragmentFromChild(Fragment fragment, String title, int navItemId) {
        Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.flMainContainer);
        if (currentFrag != null && currentFrag.getClass().equals(fragment.getClass())) {
            return;
        }

        int[] animations = getAnimationDirection(fragment);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(animations[0], animations[1]);
        transaction.replace(R.id.flMainContainer, fragment);
        transaction.commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        bottomNavigationView.setSelectedItemId(navItemId);
        currentFragment = fragment;
    }

    /**
     * Fetch role UUIDs from API
     */
    private void fetchRoleUuids() {
        DialogUtils.showLoadingDialog(context, "");

        String token = "Bearer " + SharedPref.getAccessToken(context);

        RoleService roleService = LocationApiClient.getInstance().getRoleService();

        Call<JsonObject> call = roleService.getRoles(token);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful() && response.body() != null) {
                    processRolesResponse(response.body());
                } else {
                    Log.d(TAG, "Failed to load roles. Try again.");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Get Roles Failure: ", t);
                // Helper.makeSnackBar(rlMainActivity, context.getString(R.string.Network_error_Try_again));
            }
        });
    }

    /**
     * Process roles response from API
     * @param responseBody JSON response body
     */
    private void processRolesResponse(JsonObject responseBody) {
        JsonObject data = responseBody.getAsJsonObject("_data");

        if (data != null && data.has("role")) {
            JsonArray rolesArray = data.getAsJsonArray("role");

            for (JsonElement element : rolesArray) {
                JsonObject roleObj = element.getAsJsonObject();
                String roleName = roleObj.get("name").getAsString();
                String roleUuid = roleObj.get("uuid").getAsString();

                if ("admin".equalsIgnoreCase(roleName)) {
                    SharedPref.setAdminRoleUuid(context, roleUuid);
                } else if ("viewer".equalsIgnoreCase(roleName)) {
                    SharedPref.setViewerRoleUuid(context, roleUuid);
                }
            }
            Log.d(TAG, "Admin UUID: " + SharedPref.getAdminRoleUuid(context));
            Log.d(TAG, "Viewer UUID: " + SharedPref.getViewerRoleUuid(context));
        } else {
            Log.d(TAG, "No roles found.");
        }
    }
}
