package com.example.acloc.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Report implements Serializable {
    String uuid, fkUser, fkPlace, description, createdBy;
    String placeName, placeUuid;
    int reportRating;
    String image;
    private List<String> reportTypeUuids = new ArrayList<>();
    private List<String> reportTypeNames = new ArrayList<>();


    public List<String> getReportTypeUuids() {
        return reportTypeUuids;
    }

    public void setReportTypeUuids(List<String> reportTypeUuids) {
        this.reportTypeUuids = reportTypeUuids;
    }

    public List<String> getReportTypeNames() {
        return reportTypeNames;
    }

    public void setReportTypeNames(List<String> reportTypeNames) {
        this.reportTypeNames = reportTypeNames;
    }
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getFkUser() {
        return fkUser;
    }

    public void setFkUser(String fkUser) {
        this.fkUser = fkUser;
    }

    public String getFkPlace() {
        return fkPlace;
    }

    public void setFkPlace(String fkPlace) {
        this.fkPlace = fkPlace;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public int getReportRating() {
        return reportRating;
    }

    public void setReportRating(int reportRating) {
        this.reportRating = reportRating;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getPlaceUuid() {
        return placeUuid;
    }

    public void setPlaceUuid(String placeUuid) {
        this.placeUuid = placeUuid;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    /**
     * Establishes report type UUIDs from a JsonElement
     * Can parse both JSON arrays and comma-separated strings
     */
    public void setReportTypeUuidsFromJsonElement(JsonElement element) {
        reportTypeUuids.clear();

        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonArray()) {
            // Json Array
            JsonArray uuidsArray = element.getAsJsonArray();
            for (JsonElement uuidElement : uuidsArray) {
                reportTypeUuids.add(uuidElement.getAsString());
            }
        } else if (element.isJsonPrimitive()) {
            // Comma separated strings
            String uuidsString = element.getAsString();
            if (uuidsString != null && !uuidsString.trim().isEmpty()) {
                String[] uuidArray = uuidsString.split(",");
                for (String uuid : uuidArray) {
                    if (!uuid.trim().isEmpty()) {
                        reportTypeUuids.add(uuid.trim());
                    }
                }
            }
        }
    }

    /**
     * Establishes report type names from a JsonElement
     * Can parse both JSON arrays and comma-separated strings
     */
    public void setReportTypeNamesFromJsonElement(JsonElement element) {
        reportTypeNames.clear();

        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonArray()) {
            // Json Array case
            JsonArray namesArray = element.getAsJsonArray();
            for (JsonElement nameElement : namesArray) {
                reportTypeNames.add(nameElement.getAsString());
            }
        } else if (element.isJsonPrimitive()) {
            // Comma separated string case
            String namesString = element.getAsString();
            if (namesString != null && !namesString.trim().isEmpty()) {
                String[] nameArray = namesString.split(",");
                for (String name : nameArray) {
                    if (!name.trim().isEmpty()) {
                        reportTypeNames.add(name.trim());
                    }
                }
            }
        }
    }
}
