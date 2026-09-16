package com.campusfix.model;

public class Admin extends User {

    public Admin(int userId, String name, String email, String password) {
        super(userId, name, email, password);
    }

    @Override
    public void showDashboard() {
        System.out.println("\n--- Admin Dashboard: " + name + " ---");
        System.out.println("1. View all complaints");
        System.out.println("2. Assign department / staff to a complaint");
        System.out.println("3. Change complaint priority");
        System.out.println("4. View analytics");
        System.out.println("5. Generate report / backup");
        System.out.println("6. Simulate concurrent complaint submissions (demo)");
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
