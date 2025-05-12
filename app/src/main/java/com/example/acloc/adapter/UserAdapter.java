package com.example.acloc.adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.acloc.R;
import com.example.acloc.activities.ManageRolesActivity;
import com.example.acloc.api.ApiClient;
import com.example.acloc.interfaces.ApiService;
import com.example.acloc.model.User;
import com.example.acloc.utility.Constants;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
    public static final String TAG = UserAdapter.class.getSimpleName();
    private final Context context;
    private List<User> userList;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateUserList(List<User> userList) {
        try {
            if (userList != null) {
                this.userList = userList;
                notifyDataSetChanged();
            }
        } catch (Exception exception) {
            Log.e(TAG, "Error in UserAdapter", exception);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View detailItem = inflater.inflate(R.layout.list_view_users, parent, false);
        return new ViewHolder(detailItem);
    }

    @Override
    public void onBindViewHolder(@NonNull UserAdapter.ViewHolder holder, int position) {
        try {
            if (!userList.isEmpty()) {
                User user = userList.get(position);
                holder.tvUsername.setText(user.getUsername());

                ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, Constants.ROLES_OPTIONS);
                holder.acRole.setAdapter(roleAdapter);
                holder.acRole.setThreshold(1);

                if (user.getRole() != null) {
                    holder.acRole.setText(user.getRole(), false);
                }

                // Show dropdown on click or focus
                holder.acRole.setOnClickListener(v -> holder.acRole.showDropDown());
                holder.acRole.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) holder.acRole.showDropDown();
                });

                // Handle item selection
                holder.acRole.setOnItemClickListener((parent, view, posInDropdown, id) -> {
                    String selectedRole = parent.getItemAtPosition(posInDropdown).toString();

                    AlertDialog dialog = DialogUtils.confirmationDialog(context, context.getString(R.string.Change_Role_to) + " " + selectedRole, (dialogInterface, i) -> {
                        if (selectedRole.equalsIgnoreCase(Constants.ADMIN)) {
                            updateRole(user.getUuid(), SharedPref.getAdminRoleUuid(context), holder.itemView);
                        } else if (selectedRole.equalsIgnoreCase(Constants.VIEWER)) {
                            updateRole(user.getUuid(), SharedPref.getViewerRoleUuid(context), holder.itemView);
                        }
                    });

                    dialog.show();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in User Adapter", e);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUsername;
        private AutoCompleteTextView acRole;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            acRole = itemView.findViewById(R.id.acRole);
        }
    }

    private void updateRole(String uuid, String fkRole, View rootView) {

        DialogUtils.showLoadingDialog(context, context.getString(R.string.Updating_Role));

        JsonObject userBody = new JsonObject();
        userBody.addProperty("role", fkRole);

        String token = "Bearer " + SharedPref.getAccessToken(context);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Call<JsonObject> call = apiService.updateRole(token, uuid, userBody);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful()) {
                    Helper.makeSnackBar(rootView, context.getString(R.string.User_Role_update_successfully));
                    // Reload userList after update
                    if (context instanceof ManageRolesActivity) {
                        ((ManageRolesActivity) context).getAllUsers();
                    }
                } else {
                    Helper.makeSnackBar(rootView, context.getString(R.string.Update_failed_Server_error_Try_again));
                    notifyDataSetChanged(); // Reload previous userList
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Update Role Error: ", t);
                Helper.makeSnackBar(rootView, context.getString(R.string.Network_error_Try_again));
            }
        });
    }
}
