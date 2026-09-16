package com.campusfix.model;

public class Staff extends User {

    private Integer departmentId;

    public Staff(int userId, String name, String email, String password, Integer departmentId) {
        super(userId, name, email, password);
        this.departmentId = departmentId;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    @Override
    public void showDashboard() {
        System.out.println("\n--- Staff Dashboard: " + name + " ---");
        System.out.println("1. View complaints assigned to me");
        System.out.println("2. Accept an assigned complaint");
        System.out.println("3. Put a complaint on hold / resume it");
        System.out.println("4. Resolve a complaint");
        System.out.println("5. View complaint history");
    }

    @Override
    public Role getRole() {
        return Role.STAFF;
    }
}
