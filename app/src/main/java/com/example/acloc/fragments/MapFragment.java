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
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.acloc.activity.AddNewPlaceActivity;
import com.example.acloc.activity.PlaceDetailActivity;
import com.example.acloc.api.LocationApiClient;
import com.example.acloc.dialog.PlaceBottomSheetDialog;
import com.example.acloc.model.Place;
import com.example.acloc.service.PlaceService;
import com.example.acloc.utility.Constants;
import com.example.acloc.utility.Helper;
import com.example.acloc.utility.SharedPref;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.ieslamar.acloc.R;

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

public class MapFragment extends Fragment {
    public static final String TAG = MapFragment.class.getSimpleName();
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float DEFAULT_ZOOM = 14f;
    private static final float SEARCH_ZOOM = 16f;

    private RelativeLayout rlMap;
    private View view;
    private TextInputEditText etSearchLocation;
    private ListView lvSuggestions;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Geocoder geocoder;
    private Dialog dialog;
    private Context context;
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private List<String> suggestionsList = new ArrayList<>();
    private ArrayAdapter<String> suggestionsAdapter;
    private List<Address> addressResults = new ArrayList<>();

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

        // Initialize suggestions ListView
        lvSuggestions = view.findViewById(R.id.lvSuggestions);
        if (lvSuggestions == null) {
            // If the ListView doesn't exist in the layout, create it programmatically
            lvSuggestions = new ListView(getContext());
            lvSuggestions.setId(View.generateViewId());
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.addRule(RelativeLayout.BELOW, etSearchLocation.getId());
            lvSuggestions.setLayoutParams(params);
            lvSuggestions.setVisibility(View.GONE);
            lvSuggestions.setBackgroundColor(getResources().getColor(android.R.color.white));
            rlMap.addView(lvSuggestions);
        }
    }

    private void initObj() {
        context = getContext();

        // Initialize suggestions adapter
        suggestionsAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_1, suggestionsList);
        lvSuggestions.setAdapter(suggestionsAdapter);
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
                            lvSuggestions.setVisibility(View.GONE);
                        }
                        return true;
                    }
                }
            }
            return false;
        });

        // Add TextWatcher for search suggestions
        etSearchLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel any pending searches
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                // If text is empty, hide suggestions
                if (s.length() == 0) {
                    lvSuggestions.setVisibility(View.GONE);
                    return;
                }

                // Delay search to avoid too many API calls while typing
                searchRunnable = () -> getSuggestions(s.toString());
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        lvSuggestions.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = suggestionsList.get(position);

            // Check if this is the "Add new place" option
            if (selectedItem.startsWith("➕ Add new place:")) {
                String placeName = selectedItem.substring(selectedItem.indexOf("\"") + 1, selectedItem.lastIndexOf("\""));
                etSearchLocation.setText(placeName);

                // Get current map center as the location for the new place
                LatLng center = googleMap.getCameraPosition().target;

                Place newPlace = new Place();
                newPlace.setLatitude(String.valueOf(center.latitude));
                newPlace.setLongitude(String.valueOf(center.longitude));
                newPlace.setName(placeName);
                newPlace.setAddress(""); // Will be filled in AddNewPlaceActivity
                newPlace.setDescription(""); // Empty description
                newPlace.setUuid(null); // No UUID for new place

                // Navigate to add new place activity
                Helper.goTo(getContext(), AddNewPlaceActivity.class, Constants.PLACE, newPlace);

                lvSuggestions.setVisibility(View.GONE);
                return;
            }

            // Handle regular suggestion selection (existing code)
            etSearchLocation.setText(selectedItem);

            // Get the corresponding Address object
            if (position < addressResults.size()) {
                Address address = addressResults.get(position);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                // zoom to selected location
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, SEARCH_ZOOM));

                // Find the closest place
                Place closestPlace = findClosestPlace(latLng);
                if (closestPlace != null) {
                    // Show the place details in a bottom sheet
                    showPlaceBottomSheet(closestPlace);
                }
            }

            lvSuggestions.setVisibility(View.GONE);
        });

        // trigger on Enter/Done key
        etSearchLocation.setOnEditorActionListener((v, actionId, event) -> {
            String address = Helper.getStringFromInput(etSearchLocation);
            if (!address.isEmpty()) {
                searchLocationByAddress(address);
                lvSuggestions.setVisibility(View.GONE);
            }
            return true;
        });
    }

    private void getSuggestions(String query) {
        if (query.length() < 3) {
            lvSuggestions.setVisibility(View.GONE);
            return;
        }

        try {
            // Clear previous results
            suggestionsList.clear();
            addressResults.clear();

            // Get suggestions from Geocoder
            List<Address> addresses = geocoder.getFromLocationName(query, 5);
            if (addresses != null && !addresses.isEmpty()) {
                for (Address address : addresses) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(address.getAddressLine(i));
                    }
                    String addressText = sb.toString();
                    suggestionsList.add(addressText);
                    addressResults.add(address);
                }

                // Add "Add new place" option if few results
                if (addresses.size() < 3) {
                    suggestionsList.add("➕ Add new place: \"" + query + "\"");
                }
            } else {
                // No results found, add option to create new place
                suggestionsList.add("➕ Add new place: \"" + query + "\"");
            }

            suggestionsAdapter.notifyDataSetChanged();
            lvSuggestions.setVisibility(View.VISIBLE);
        } catch (IOException e) {
            Log.e(TAG, "Error getting suggestions", e);
            lvSuggestions.setVisibility(View.GONE);
        }
    }

    private void initMap() {
        fetchAndShowAllPlaces();
        // Clear old data
        placeList.clear(); // clear in-memory list ///
        googleMap.clear(); // remove all markers from the map ///

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        geocoder = new Geocoder(requireContext(), Locale.getDefault());

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            googleMap.setMyLocationEnabled(true);

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM));
                    showNearbyPlaces(currentLatLng);
                }
            });

            googleMap.setOnMapClickListener(latLng -> {
                Place existingPlace = getPlaceIfExists(latLng);
                if (existingPlace != null) {
                    showPlaceBottomSheet(existingPlace);
                    return;
                } else {
                    try {
                        List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                        if (addressList != null && !addressList.isEmpty()) {
                            Address address = addressList.get(0);
                            String addressLine = address.getAddressLine(0);
                            String placeName = address.getFeatureName();

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
                    showPlaceBottomSheet(existingPlace);
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

    private Place findClosestPlace(LatLng searchLatLng) {
        if (placeList.isEmpty()) {
            return null;
        }

        Place closestPlace = null;
        float minDistance = Float.MAX_VALUE;
        float[] result = new float[1];

        for (Place place : placeList) {
            double lat = Double.parseDouble(place.getLatitude());
            double lng = Double.parseDouble(place.getLongitude());

            Location.distanceBetween(
                    searchLatLng.latitude, searchLatLng.longitude,
                    lat, lng,
                    result);

            if (result[0] < minDistance) {
                minDistance = result[0];
                closestPlace = place;
            }
        }

        // Only return if within reasonable distance (1000 meters)
        return minDistance < 1000 ? closestPlace : null;
    }

    private void showPlaceBottomSheet(Place place) {
        PlaceBottomSheetDialog bottomSheet = new PlaceBottomSheetDialog(place);
        bottomSheet.show(getChildFragmentManager(), "PlaceBottomSheet");
    }

    private void showNearbyPlaces(LatLng latLng) {
        googleMap.clear();
    }

    private void searchLocationByAddress(String address) {
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, SEARCH_ZOOM));

                // Find the closest place to the search result
                Place closestPlace = findClosestPlace(latLng);
                if (closestPlace != null) {
                    // Show the place details in a bottom sheet
                    showPlaceBottomSheet(closestPlace);
                } else {
                    // No close places found
                    if (isAdded() && getContext() != null) {
                        Helper.makeSnackBar(rlMap, "No places found near this location");
                    }
                }
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

        PlaceService placeService = LocationApiClient.getInstance().getPlaceService();
        Call<ResponseBody> call = placeService.getAllPlaces(token);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        JSONArray placesArray = jsonObject.getJSONObject("_data").getJSONArray("places");

                        // Clear existing places
                        placeList.clear();

                        for (int i = 0; i < placesArray.length(); i++) {
                            JSONObject placeObj = placesArray.getJSONObject(i);
                            String name = placeObj.getString("name");
                            String address = placeObj.getString("address");
                            String description = placeObj.optString("description", "");
                            String createdBy = placeObj.optString("createdBy", "");
                            String uuid = placeObj.optString("uuid", "");
                            double lat = placeObj.getDouble("latitude");
                            double lng = placeObj.getDouble("longitude");
                            String image = placeObj.getString("images");

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
                            place.setImage(image);

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
