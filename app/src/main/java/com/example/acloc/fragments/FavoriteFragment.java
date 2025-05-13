package com.example.acloc.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.ieslamar.acloc.R;
import com.example.acloc.adapter.FavoriteAdapter;
import com.example.acloc.api.ApiClient;
import com.example.acloc.interfaces.ApiService;
import com.example.acloc.model.Favorite;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoriteFragment extends Fragment {
    public static final String TAG = FavoriteFragment.class.getSimpleName();
    private View view;
    private FrameLayout rlFavorite;
    private RecyclerView rvFavorite;
    private TextView tvNoData;
    private Context context;
    private FavoriteAdapter adapter;
    private Favorite favoriteEntity;
    private List<Favorite> favoriteList;

    public FavoriteFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_favorite, container, false);
        initUI();
        initObj();
        initListener();
        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            setDataVisibility(false);
            favoriteList.clear();
        }
        if (context != null) loadData();
    }

    private void setDataVisibility(boolean isDataAvailable) {
        if (isDataAvailable) {
            rvFavorite.setVisibility(View.VISIBLE);
            tvNoData.setVisibility(View.GONE);
        } else {
            rvFavorite.setVisibility(View.GONE);
            tvNoData.setVisibility(View.VISIBLE);
        }
    }

    private void initUI() {
        rlFavorite = view.findViewById(R.id.rlFavorite);
        rvFavorite = view.findViewById(R.id.rvFavorite);
        tvNoData = view.findViewById(R.id.tvNoData);
    }

    private void initObj() {
        context = getContext();
        favoriteEntity = new Favorite();
    }

    private void initListener() {

    }

    private void loadData() {
        try {
            if (favoriteList == null) {
                favoriteList = new ArrayList<>();
            }
            String userUuid = SharedPref.getUserUid(context);
            getFavoriteByUserUuid(userUuid);
        } catch (Exception e) {
            Log.e(TAG, "Error in FavoriteFragment", e);
            Helper.makeSnackBar(view, getString(R.string.Something_went_wrong_Try_again));
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private void setUpRecyclerView() {
        try {
            if (adapter != null) {
                adapter.updateFavoriteList(favoriteList);
            } else {
                adapter = new FavoriteAdapter(context, favoriteList);
                rvFavorite.setAdapter(adapter);
                rvFavorite.setLayoutManager(Helper.getVerticalManager(context));
                adapter.notifyDataSetChanged();
            }
            setDataVisibility(true);
        } catch (Exception e) {
            Log.e(TAG, "Error in FavoriteFragment", e);
            Helper.showToast(context, getString(R.string.Something_went_wrong_Try_again));
            setDataVisibility(false);
        }
    }

    private void getFavoriteByUserUuid(String userUuid) {
        DialogUtils.showLoadingDialog(context, context.getString(R.string.Loading_Favorites));

        String token = "Bearer " + SharedPref.getAccessToken(context);
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        Call<JsonObject> call = apiService.getUserFavorites(token, userUuid);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    JsonObject data = responseBody.getAsJsonObject("_data");

                    if (data != null && data.has("favorites")) {
                        favoriteList.clear(); // Clear previous list

                        for (JsonElement element : data.getAsJsonArray("favorites")) {
                            JsonObject favoriteObject = element.getAsJsonObject();
                            Favorite favorite = new Favorite();

                            favorite.setUuid(favoriteObject.get("uuid").getAsString());
                            favorite.setActive(favoriteObject.get("active").getAsString());
                            favorite.setPlaceUuid(favoriteObject.get("place_uuid").getAsString());
                            favorite.setPlaceName(favoriteObject.get("place_name").getAsString());
                            favorite.setPlaceAddress(favoriteObject.get("place_address").getAsString());
                            favorite.setPlaceLat(favoriteObject.get("place_latitude").getAsString());
                            favorite.setPlaceLng(favoriteObject.get("place_longitude").getAsString());
                            favorite.setPlaceDescription(favoriteObject.get("place_description").getAsString());

                            favoriteList.add(favorite);
                        }

                        // Set up recyclerview
                        if (!favoriteList.isEmpty()) {
                            setUpRecyclerView();
                        } else {
                            setDataVisibility(false);
                        }

                    } else {
                        setDataVisibility(false);
                        Helper.makeSnackBar(rlFavorite, context.getString(R.string.No_Favorite_found));
                    }
                } else {
                    setDataVisibility(false);
                    Helper.makeSnackBar(rlFavorite, context.getString(R.string.Failed_to_load_favorite_Try_again));
                    Log.e(TAG, "Get favorite Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Get favorite Failure: ", t);
                Helper.makeSnackBar(rlFavorite, context.getString(R.string.Network_error_Try_again));
            }
        });
    }


}