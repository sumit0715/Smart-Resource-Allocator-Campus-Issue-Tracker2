package com.campusfix.model;

public class Department {

    private final int departmentId;
    private final String name;
    private final String description;

    public Department(int departmentId, String name, String description) {
        this.departmentId = departmentId;
        this.name = name;
        this.description = description;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return departmentId + " - " + name;
    }
}
