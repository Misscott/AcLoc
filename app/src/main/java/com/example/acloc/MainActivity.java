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

import com.example.acloc.activities.ManageRolesActivity;
import com.example.acloc.api.ApiClient;
import com.example.acloc.dialog.AlertChangePasswordDialog;
import com.example.acloc.dialog.AlertViewOrUpdateProfileDialog;
import com.example.acloc.fragments.FavoriteFragment;
import com.example.acloc.fragments.MapFragment;
import com.example.acloc.fragments.MyReportsFragment;
import com.example.acloc.interfaces.ApiService;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private RelativeLayout rlMainActivity;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private Dialog dialog;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Get saved language from SharedPreferences
        String savedLanguage = SharedPref.getLanguage(this);

        // Check if current language matches saved
        if (!Locale.getDefault().getLanguage().equals(savedLanguage)) {
            setLocale(savedLanguage);  // Apply only if needed
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initToolbar();
        initUI();
        initObj();
        initListeners();
        // Load default fragment
        loadDefaultFragment(savedInstanceState);
        getRoleUuids();
    }

    private void initToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initUI() {
        rlMainActivity = findViewById(R.id.rlMainActivity);
        bottomNavigationView = findViewById(R.id.bottomNavView);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        if (menu instanceof MenuBuilder) {
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);
        }

        // show/hide the "Manage Roles" menu item
        MenuItem manageNotesItem = menu.findItem(R.id.menu_manageRoles);
        if (!"admin".equalsIgnoreCase(SharedPref.getRole(context))) {
            manageNotesItem.setVisible(false); // hide for non-admins
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_profile) {
            dialog = new AlertViewOrUpdateProfileDialog(context)
                    .openProfileDialog();
            return true;
        } else if (id == R.id.menu_changePassword) {
            dialog = new AlertChangePasswordDialog(context)
                    .openChangePasswordDialog();
        } else if (id == R.id.menu_changeLanguage) {
            changeLanguage();
        } else if (id == R.id.menu_manageRoles) {
            Helper.goTo(MainActivity.this, ManageRolesActivity.class);
        } else if (id == R.id.menu_logout) {
            AlertDialog dialog = DialogUtils.logoutDialog(context);
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void changeLanguage() {
        String currentLanguage = Locale.getDefault().getLanguage();
        String newLanguage = currentLanguage.equals("es") ? "en" : "es";

        AlertDialog dialog = DialogUtils.confirmationDialog(
                this,
                getString(R.string.change_language_confirmation),
                (dialogInterface, i) -> {
                    SharedPref.setLanguage(this, newLanguage); // Save to SharedPref
                    setLocale(newLanguage); // Apply language

                    // Restart app to apply change
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
        );
        dialog.show();
    }

    // Method to update the language dynamically
    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    private void initObj() {
        context = this;
    }

    private void initListeners() {
        // Set item selected listener for bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
                    loadFragment(selectedFragment, title);
                }
                return true;
            }
        });
    }

    private void loadFragment(Fragment fragment, String title) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.flMainContainer, fragment);
        transaction.commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title); // Correct way to set title with support action bar
        }
    }

    private void loadDefaultFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Fragment defaultFragment = new MapFragment();
            String defaultTitle = getString(R.string.Map);

            loadFragment(defaultFragment, defaultTitle);
            bottomNavigationView.setSelectedItemId(R.id.menu_map);
        }
    }

    //  to change the fragment
    public void openFragmentFromChild(Fragment fragment, String title, int navItemId) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.flMainContainer);
        if (currentFragment != null && currentFragment.getClass().equals(fragment.getClass())) {
            return;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        transaction.replace(R.id.flMainContainer, fragment);
        transaction.commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        bottomNavigationView.setSelectedItemId(navItemId);
    }

    private void getRoleUuids() {
        DialogUtils.showLoadingDialog(context, "");

        String token = "Bearer " + SharedPref.getAccessToken(context);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Call<JsonObject> call = apiService.getRoles(token);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
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
                } else {
                    Log.d(TAG, "Failed to load roles. Try again.");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Get Roles Failure: ", t);
//                Helper.makeSnackBar(rlMainActivity, context.getString(R.string.Network_error_Try_again));
            }
        });
    }


}