package com.campusfix.dao;

import com.campusfix.exception.UserNotFoundException;
import com.campusfix.model.Admin;
import com.campusfix.model.Role;
import com.campusfix.model.Staff;
import com.campusfix.model.Student;
import com.campusfix.model.User;
import com.campusfix.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User findByEmailAndPassword(String email, String password) throws SQLException, UserNotFoundException {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        throw new UserNotFoundException("No account matches that email/password combination.");
    }

    public User findById(int userId) throws SQLException, UserNotFoundException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        throw new UserNotFoundException("No user with ID " + userId);
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<User> findByRole(Role role) throws SQLException {
        String sql = "SELECT * FROM users WHERE role = ?";
        List<User> results = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    public User save(User user) throws SQLException {
        String sql = "INSERT INTO users (name, email, password, role, department_id) VALUES (?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole().name());
            if (user instanceof Staff staff && staff.getDepartmentId() != null) {
                ps.setInt(5, staff.getDepartmentId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return rebuildWithId(user, keys.getInt(1));
                }
            }
        }
        return user;
    }

    private User rebuildWithId(User user, int newId) {
        return switch (user.getRole()) {
            case STUDENT -> new Student(newId, user.getName(), user.getEmail(), user.getPassword());
            case ADMIN -> new Admin(newId, user.getName(), user.getEmail(), user.getPassword());
            case STAFF -> new Staff(newId, user.getName(), user.getEmail(), user.getPassword(),
                    ((Staff) user).getDepartmentId());
        };
    }

    private User mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        Role role = Role.valueOf(rs.getString("role"));
        return switch (role) {
            case STUDENT -> new Student(id, name, email, password);
            case ADMIN -> new Admin(id, name, email, password);
            case STAFF -> {
                int deptId = rs.getInt("department_id");
                Integer dept = rs.wasNull() ? null : deptId;
                yield new Staff(id, name, email, password, dept);
            }
        };
    }
}
