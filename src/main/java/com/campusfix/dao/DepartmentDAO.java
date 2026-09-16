package com.campusfix.dao;

import com.campusfix.exception.DepartmentNotFoundException;
import com.campusfix.model.Department;
import com.campusfix.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDAO {

    public List<Department> findAll() throws SQLException {
        String sql = "SELECT * FROM departments ORDER BY department_id";
        List<Department> results = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }
        return results;
    }

    public Department findById(int departmentId) throws SQLException, DepartmentNotFoundException {
        String sql = "SELECT * FROM departments WHERE department_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        throw new DepartmentNotFoundException("No department with ID " + departmentId);
    }

    /** Looks up a department by name - used by AssignmentService's category routing table. */
    public Department findByName(String name) throws SQLException, DepartmentNotFoundException {
        String sql = "SELECT * FROM departments WHERE department_name = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        throw new DepartmentNotFoundException("No department named " + name);
    }

    private Department mapRow(ResultSet rs) throws SQLException {
        return new Department(rs.getInt("department_id"), rs.getString("department_name"),
                rs.getString("description"));
    }
}
