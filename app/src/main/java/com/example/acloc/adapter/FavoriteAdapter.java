package com.example.acloc.adapter;

import android.annotation.SuppressLint;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ieslamar.acloc.R;
import com.example.acloc.activity.PlaceDetailActivity;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.model.Favorite;
import com.example.acloc.model.Place;
import com.example.acloc.service.FavoriteService;
import com.example.acloc.service.PlaceService;
import com.example.acloc.utility.Constants;
import com.example.acloc.utility.DialogUtils;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {
    public interface OnFavoriteClickListener {
        void onFavoriteClick(Favorite favorite);
    }

    private OnFavoriteClickListener clickListener;
    public static final String TAG = FavoriteAdapter.class.getSimpleName();
    private final Context context;
    private List<Favorite> favoriteList;

    public FavoriteAdapter(Context context, List<Favorite> favoriteList, OnFavoriteClickListener clickListener) {
        this.context = context;
        this.favoriteList = favoriteList;
        this.clickListener = clickListener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateFavoriteList(List<Favorite> favoriteList) {
        try {
            if (favoriteList != null) {
                this.favoriteList = favoriteList;
                notifyDataSetChanged();
            }
        } catch (Exception exception) {
            Log.e(TAG, "Error in FavoriteAdapter", exception);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View detailItem = inflater.inflate(R.layout.list_view_favorite, parent, false);
        return new ViewHolder(detailItem);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteAdapter.ViewHolder holder, int position) {
        try {
            if (!favoriteList.isEmpty()) {
                Favorite favorite = favoriteList.get(position);
                holder.tvPlaceName.setText(favorite.getPlaceName());
                holder.tvDescription.setText(favorite.getPlaceDescription());

                // handle favorite on click
                holder.ivFavorite.setOnClickListener(v -> removePlaceFromFavorites(SharedPref.getUserUuid(context), favorite.getPlaceUuid(), favorite.getUuid()));

                //holder.itemView.setOnClickListener(v ->  getPlaceByUuid(favorite.getPlaceUuid()));

                holder.itemView.setOnClickListener(v -> {
                    if (clickListener != null) {
                        clickListener.onFavoriteClick(favorite);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in Favorite Adapter", e);
        }
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvPlaceName, tvDescription;
        private final ImageView ivFavorite;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaceName = itemView.findViewById(R.id.tvPlaceName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
        }
    }

    private void removePlaceFromFavorites(String userUuid, String placeUuid, String favoriteUuid) {
        DialogUtils.showLoadingDialog(context, context.getString(R.string.Removing_from_favorites));
        String token = "Bearer " + SharedPref.getAccessToken(context);

        FavoriteService favoriteService = LocationApiClient.getInstance().getFavoriteService();
        Call<Void> call = favoriteService.removePlaceFromFavorites(token, userUuid, placeUuid);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                DialogUtils.dismissDialog();
                View rootView = ((Activity) context).findViewById(android.R.id.content);

                if (response.isSuccessful()) {
                    // Find position of the favorite to be removed
                    int positionToRemove = -1;
                    for (int i = 0; i < favoriteList.size(); i++) {
                        if (favoriteList.get(i).getUuid().equals(favoriteUuid)) {
                            positionToRemove = i;
                            break;
                        }
                    }

                    // Remove the favorite from the list
                    if (positionToRemove != -1) {
                        favoriteList.remove(positionToRemove);
                        notifyItemRemoved(positionToRemove);  // Notify the adapter that the item was removed
                    }

                    Log.d(TAG, "Favorite removed!");
                    Helper.makeSnackBar(rootView, context.getString(R.string.Favorite_removed));
                } else {
                    Log.d(TAG, "Failed to remove Favorite");
                    Helper.makeSnackBar(rootView, context.getString(R.string.Failed_to_remove_Favorite));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                DialogUtils.dismissDialog();
                View rootView = ((Activity) context).findViewById(android.R.id.content);
                Log.e(TAG, "Remove Favorite Error: ", t);
                Helper.makeSnackBar(rootView, context.getString(R.string.Network_error_Try_again));
            }
        });
    }

    /*private void getPlaceByUuid(String placeUuid) {
        DialogUtils.showLoadingDialog(context, context.getString(R.string.Please_wait));

        String token = "Bearer " + SharedPref.getAccessToken(context);

        PlaceService placeService = LocationApiClient.getInstance().getPlaceService();
        Call<JsonObject> call = placeService.getPlaceFromUuid(token, placeUuid);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                DialogUtils.dismissDialog();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    JsonObject data = responseBody.getAsJsonObject("_data");

                    if (data != null && data.has("places")) {
                        Place place = new Place();

                        for (JsonElement element : data.getAsJsonArray("places")) {
                            JsonObject placeObject = element.getAsJsonObject();

                            place.setUuid(placeObject.get("uuid").getAsString());
                            place.setName(placeObject.get("name").getAsString());
                            place.setDescription(placeObject.get("description").getAsString());
                            place.setAddress(placeObject.get("address").getAsString());
                            place.setLatitude(placeObject.get("latitude").getAsString());
                            place.setLongitude(placeObject.get("longitude").getAsString());
                        }
                        Helper.goTo(context, PlaceDetailActivity.class, Constants.PLACE, place);
                    } else {
                        Log.e(TAG, "No place found");
                    }
                } else {
                    Log.e(TAG, "Get Reports Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                DialogUtils.dismissDialog();
                Log.e(TAG, "Get Reports Failure: ", t);
            }
        });
    }*/
}
