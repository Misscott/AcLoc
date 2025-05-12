package com.example.acloc.model;

import java.io.Serializable;

public class Favorite implements Serializable {
    String uuid, active, placeUuid, placeName, placeAddress, placeLat, placeLng, placeDescription, userUsername;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getActive() {
        return active;
    }

    public void setActive(String active) {
        this.active = active;
    }

    public String getPlaceUuid() {
        return placeUuid;
    }

    public void setPlaceUuid(String placeUuid) {
        this.placeUuid = placeUuid;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getPlaceAddress() {
        return placeAddress;
    }

    public void setPlaceAddress(String placeAddress) {
        this.placeAddress = placeAddress;
    }

    public String getPlaceLat() {
        return placeLat;
    }

    public void setPlaceLat(String placeLat) {
        this.placeLat = placeLat;
    }

    public String getPlaceLng() {
        return placeLng;
    }

    public void setPlaceLng(String placeLng) {
        this.placeLng = placeLng;
    }

    public String getPlaceDescription() {
        return placeDescription;
    }

    public void setPlaceDescription(String placeDescription) {
        this.placeDescription = placeDescription;
    }

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }
}
