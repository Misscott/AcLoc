package com.example.acloc.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acloc.adapter.UserAdapter;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.model.User;
import com.example.acloc.service.UserService;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.ieslamar.acloc.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageRolesActivity extends AppCompatActivity {

    public static final String TAG = ManageRolesActivity.class.getSimpleName();
    private RelativeLayout rlManageRoles;
    private TextInputLayout tilSearch;
    private TextInputEditText etSearchUser;
    private RecyclerView rvUsers;
    private List<User> userList;
    private UserAdapter adapter;
    private Context context;
    private User userEntity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_roles);

        initToolbar();
        initUI();
        initListener();
        initObj();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            setDataVisibility(false);
            userList.clear();
        }
        if (context != null) loadData();
    }

    private void setDataVisibility(boolean isDataAvailable) {
        if (isDataAvailable) {
            rvUsers.setVisibility(View.VISIBLE);
        } else {
            //            rvUsers.setVisibility(View.GONE);

        }
    }

    private void initToolbar() {
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle(getString(R.string.Manage_Roles));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in ManageRoles", e);
        }
    }

    private void initUI() {
        rlManageRoles = findViewById(R.id.rlManageRoles);
        tilSearch = findViewById(R.id.tilSearch);
        etSearchUser = findViewById(R.id.etSearchUser);
        rvUsers = findViewById(R.id.rvUsers);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initListener() {

        //Search drawable on click
        etSearchUser.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableEnd = etSearchUser.getCompoundDrawables()[2] != null
                        ? etSearchUser.getWidth() - etSearchUser.getPaddingEnd()
                        : 0;

                if (event.getRawX() >= (etSearchUser.getRight() - etSearchUser.getCompoundDrawables()[2].getBounds().width())) {
                    // Search icon clicked
                    filterUserList(Helper.getStringFromInput(etSearchUser));
                    return true;
                }
            }
            return false;
        });

        //Enter
        etSearchUser.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterUserList(Helper.getStringFromInput(etSearchUser));
                return true;
            }
            return false;
        });
    }

    private void initObj() {
        context = this;
        userEntity = new User();
    }

    private void loadData() {
        try {
            if (userList == null) {
                userList = new ArrayList<>();
            }
            getAllUsers();
        } catch (Exception e) {
            Log.e(TAG, "Error in ManageRoles", e);
            Helper.makeSnackBar(rlManageRoles, getString(R.string.Something_went_wrong_Try_again));
        }
    }

    private void setUpRecyclerView(List<User> userList) {
        try {
            if (adapter != null) {
                adapter.updateUserList(userList);
            } else {
                adapter = new UserAdapter(context, userList);
                rvUsers.setAdapter(adapter);
                rvUsers.setLayoutManager(Helper.getVerticalManager(context));
                adapter.notifyDataSetChanged();
            }
            if (userList != null && !userList.isEmpty()) {
                setDataVisibility(true); // Data available
            } else {
                setDataVisibility(false); // No data
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in ManageRoles", e);
            Helper.showToast(context, getString(R.string.Something_went_wrong_Try_again));
            setDataVisibility(false);
        }
    }

    private void filterUserList(String query) {
        if (TextUtils.isEmpty(query)) {
            setUpRecyclerView(userList); // Show full list if query is empty
            return;
        }

        List<User> filteredList = new ArrayList<>();
        for (User user : userList) {
            if (user.getUsername() != null && user.getUsername().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(user);
            }
        }

        setUpRecyclerView(filteredList);
    }

    public void getAllUsers() {
        DialogUtils.showLoadingDialog(context, getString(R.string.Loading_users));

        String token = "Bearer " + SharedPref.getAccessToken(context);

        UserService userService = LocationApiClient.getInstance().getUserService();
        Call<JsonObject> call = userService.getAllUsers(token);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    JsonObject data = responseBody.getAsJsonObject("_data");

                    if (data != null && data.has("users")) {
                        userList.clear(); // clear previous list

                        for (JsonElement element : data.getAsJsonArray("users")) {
                            JsonObject userObj = element.getAsJsonObject();
                            String username = userObj.get("username").getAsString();
                            // Skip the current logged-in user
                            if (username.equals(SharedPref.getUserName(context))) {
                                continue;
                            }

                            User user = new User();
                            user.setUsername(username);
                            user.setEmail(userObj.has("email") && !userObj.get("email").isJsonNull()
                                    ? userObj.get("email").getAsString() : null);
                            user.setUuid(userObj.get("uuid").getAsString());
                            user.setFkRole(userObj.get("fk_role").getAsString());
                            user.setRole(userObj.get("role").getAsString());

                            userList.add(user);
                        }

                        if (!userList.isEmpty()) {
                            setUpRecyclerView(userList);
                        } else {
                            setDataVisibility(false);
                        }

                    } else {
                        setDataVisibility(false);
                        Helper.makeSnackBar(rlManageRoles, getString(R.string.No_users_found));
                    }

                } else {
                    setDataVisibility(false);
                    Helper.makeSnackBar(rlManageRoles, getString(R.string.Failed_to_load_users_Try_again));
                    Log.e(TAG, "Get users error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Get users failure: ", t);
                Helper.makeSnackBar(rlManageRoles, context.getString(R.string.Network_error_Try_again));
            }
        });
    }
}
