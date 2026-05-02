package src;

import javax.swing.*;
import java.awt.*;

public class AdminDashboard extends JFrame {
    private String adminUsername;

    public AdminDashboard(String username) {
        this.adminUsername = username;
        setTitle("Admin Portal - Manager: " + username);
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1, 10, 10));
        setLocationRelativeTo(null);

        JLabel welcomeLabel = new JLabel("Manager Control Panel", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 22));
        add(welcomeLabel);

        // Requirement: Add, Edit, and Delete info for customers and reps
        JButton userManageBtn = new JButton("User Management (Customers & Staff)");
        add(userManageBtn);

        // Requirement: Sales Reports & Revenue Analytics
        JButton reportsBtn = new JButton("Sales & Financial Reports");
        add(reportsBtn);

        JButton logoutBtn = new JButton("Log Out");
        add(logoutBtn);

        // --- Action Listeners ---

        // Launches the user management tool for Customers and Employees
        userManageBtn.addActionListener(e -> new UserManagementFrame().setVisible(true));

        // UPDATED: Now launches the full analytical reports module
        reportsBtn.addActionListener(e -> new AdminReportsFrame().setVisible(true));

        logoutBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
    }
}