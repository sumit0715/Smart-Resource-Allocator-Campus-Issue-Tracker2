package com.campusfix.model;

public class Student extends User {

    public Student(int userId, String name, String email, String password) {
        super(userId, name, email, password);
    }

    @Override
    public void showDashboard() {
        System.out.println("\n--- Student Dashboard: " + name + " ---");
        System.out.println("1. Submit a complaint");
        System.out.println("2. Track my complaints");
        System.out.println("3. View complaint history");
        System.out.println("4. Submit feedback on a resolved complaint");
        System.out.println("5. Cancel a pending complaint");
    }

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }
}
