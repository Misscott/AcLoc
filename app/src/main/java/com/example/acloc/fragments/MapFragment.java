package com.example.acloc.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.ieslamar.acloc.R;
import com.example.acloc.activity.AddNewPlaceActivity;
import com.example.acloc.activity.PlaceDetailActivity;
import com.example.acloc.interfaces.ApiService;
import com.example.acloc.model.Place;
import com.example.acloc.utility.Constants;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MapFragment extends Fragment {
    public static final String TAG = MapFragment.class.getSimpleName();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private RelativeLayout rlMap;
    private View view;
    private TextInputEditText etSearchLocation;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Geocoder geocoder;
    private Dialog dialog;
    private Context context;

    private List<Place> placeList = new ArrayList<>();


    public MapFragment() {
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchAndShowAllPlaces();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_map, container, false);
        initUI();
        initListener();
        initObj();
        return view;
    }

    private void initUI() {
        rlMap = view.findViewById(R.id.rlMap);
        etSearchLocation = view.findViewById(R.id.etSearchLocation);
    }

    private void initObj() {
        context = getContext();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initListener() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.mapFrame);

        if (mapFragment != null) {
            mapFragment.getMapAsync(map -> {
                googleMap = map;
                initMap();
            });
        }

        // Handle search icon click
        etSearchLocation.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = etSearchLocation.getCompoundDrawables()[2]; // Right drawable

                if (drawableEnd != null) {
                    int drawableWidth = drawableEnd.getBounds().width();
                    int touchAreaStart = etSearchLocation.getWidth() - drawableWidth - etSearchLocation.getPaddingEnd();

                    if (event.getX() >= touchAreaStart) {
                        v.performClick(); // important for accessibility
                        String address = Helper.getStringFromInput(etSearchLocation);
                        if (!address.isEmpty()) {
                            searchLocationByAddress(address);
                        }
                        return true;
                    }
                }
            }
            return false;
        });

        // trigger on Enter/Done key
        etSearchLocation.setOnEditorActionListener((v, actionId, event) -> {
            String address = Helper.getStringFromInput(etSearchLocation);
            if (!address.isEmpty()) {
                searchLocationByAddress(address);
            }
            return true;
        });
    }

    private void initMap() {
        fetchAndShowAllPlaces();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        geocoder = new Geocoder(requireContext(), Locale.getDefault());

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            googleMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f));
                    showNearbyPlaces(currentLatLng);
                }
            });

            googleMap.setOnMapClickListener(latLng -> {
                Place existingPlace = getPlaceIfExists(latLng);
                if (existingPlace != null) {
                    Helper.goTo(getContext(), PlaceDetailActivity.class, Constants.PLACE, existingPlace); //Navigate to PlaceDetailActivity if the place already exists
                    return;
//
//                    dialog = new AlertViewAddNewPlaceDialog(requireContext(),
//                            Double.parseDouble(existingPlace.getLatitude()),
//                            Double.parseDouble(existingPlace.getLongitude()),
//                            existingPlace.getName(),
//                            existingPlace.getAddress(),
//                            existingPlace.getDescription(),
//                            existingPlace.getUuid())
//                            .openPlaceDialog();
                } else {
                    try {
                        List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                        if (addressList != null && !addressList.isEmpty()) {
                            Address address = addressList.get(0);
                            String addressLine = address.getAddressLine(0);
                            String placeName = address.getFeatureName();

//                            dialog = new AlertViewAddNewPlaceDialog(requireContext(),
//                                    latLng.latitude,
//                                    latLng.longitude,
//                                    placeName,
//                                    addressLine,
//                                    "",      // Empty description
//                                    null     // UUID
//                            ).openPlaceDialog();

                            Place placeEntity = new Place();
                            placeEntity.setLatitude(String.valueOf(latLng.latitude));
                            placeEntity.setLongitude(String.valueOf(latLng.longitude));
                            placeEntity.setName(placeName);
                            placeEntity.setAddress(addressLine);
                            placeEntity.setDescription(""); // Empty description
                            placeEntity.setUuid(null); // No UUID for new place
                            Helper.goTo(getContext(), AddNewPlaceActivity.class, Constants.PLACE, placeEntity);
                        } else {
                            Place placeEntity = new Place();
                            placeEntity.setLatitude(String.valueOf(latLng.latitude));
                            placeEntity.setLongitude(String.valueOf(latLng.longitude));
                            placeEntity.setName("");
                            placeEntity.setAddress("Address not found");
                            placeEntity.setDescription(""); // Empty description
                            placeEntity.setUuid(null); // No UUID for a new place
                            Helper.goTo(getContext(), AddNewPlaceActivity.class, Constants.PLACE, placeEntity);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            googleMap.setOnMarkerClickListener(marker -> {
                marker.hideInfoWindow();

                LatLng latLng = marker.getPosition();
                Place existingPlace = getPlaceIfExists(latLng);

                if (existingPlace != null) {
                    Helper.goTo(getContext(), PlaceDetailActivity.class, Constants.PLACE, existingPlace);
                    return true;
                }

                return true;
            });


        } else {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private Place getPlaceIfExists(LatLng clickedLatLng) {
        final float[] result = new float[1];
        for (Place place : placeList) {
            double lat = Double.parseDouble(place.getLatitude());
            double lng = Double.parseDouble(place.getLongitude());

            Location.distanceBetween(
                    clickedLatLng.latitude, clickedLatLng.longitude,
                    lat, lng,
                    result);

            if (result[0] < 10) { // less than 10 meters
                return place;
            }
        }
        return null;
    }

    private void showNearbyPlaces(LatLng latLng) {
        googleMap.clear();
//        googleMap.addMarker(new MarkerOptions()
//                .position(latLng)
//                .title("You are here"));
    }

    private void searchLocationByAddress(String address) {
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f));
                showNearbyPlaces(latLng);
            } else {
                Helper.makeSnackBar(rlMap, "Location not found");
            }
        } catch (IOException e) {
            Log.d(TAG, getString(R.string.Something_went_wrong_Try_again));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initMap();
        } else {
            boolean showRationale = shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION);
            if (!showRationale) {
                showPermissionRequiredDialog();
            } else {
                Helper.showToast(context, getString(R.string.Location_permission_is_required));
            } }
    }
    private void showPermissionRequiredDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.Location_Permission_Required))
                .setMessage(getString(R.string.Location_permission_rationale))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.Go_to_Settings), (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.Exit_App), (dialog, which) -> requireActivity().finish())
                .show();
    }

    private void fetchAndShowAllPlaces() {
        String token = "Bearer " + SharedPref.getAccessToken(context); // get saved token

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://locationapi-m13l.onrender.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<ResponseBody> call = apiService.getAllPlaces(token);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray placesArray = jsonObject.getJSONObject("_data").getJSONArray("places");

                        for (int i = 0; i < placesArray.length(); i++) {
                            JSONObject placeObj = placesArray.getJSONObject(i);
                            String name = placeObj.getString("name");
                            String address = placeObj.getString("address");
                            String description = placeObj.optString("description", "");
                            String createdBy = placeObj.optString("createdBy", "");
                            String uuid = placeObj.optString("uuid", "");
                            double lat = placeObj.getDouble("latitude");
                            double lng = placeObj.getDouble("longitude");

                            LatLng latLng = new LatLng(lat, lng);
                            googleMap.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title(name)
                                    .snippet(address));

                            // Add to memory list
                            Place place = new Place();
                            place.setName(name);
                            place.setAddress(address);
                            place.setLatitude(String.valueOf(lat));
                            place.setLongitude(String.valueOf(lng));
                            place.setDescription(description);
                            place.setCreatedBy(createdBy);
                            place.setUuid(uuid);

                            placeList.add(place);
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Helper.makeSnackBar(rlMap, getString(R.string.Failed_to_load_places));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                Helper.makeSnackBar(rlMap, getString(R.string.Network_error_Try_again));
            }
        });
    }
}