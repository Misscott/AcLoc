package com.example.acloc.model;

import java.io.Serializable;

public class ReportType implements Serializable {
    private String uuid;
    private String name;
    private boolean selected;

    public ReportType() {
    }

    public ReportType(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.selected = false;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
