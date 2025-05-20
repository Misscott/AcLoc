package com.example.acloc.model;

import java.io.Serializable;

public class Report implements Serializable {
    private String uuid, fkUser, fkPlace, fkReportType, description, created, createdBy;
    private String placeName, placeUuid;
    private int reportRating;

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

    public String getFkReportType() {
        return fkReportType;
    }

    public void setFkReportType(String fkReportType) {
        this.fkReportType = fkReportType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    public String getCreationDate(){
        return created;
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
    public String getCreatedDate(){
        return created;
    }
}
