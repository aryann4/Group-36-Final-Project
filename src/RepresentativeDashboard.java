package src;
import javax.swing.*;
import java.awt.*;

public class RepresentativeDashboard extends JFrame {
    private String repUsername;

    public RepresentativeDashboard(String username) {
        this.repUsername = username;
        setTitle("Employee Portal - Representative: " + username);
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(5, 1, 10, 10));
        setLocationRelativeTo(null);

        JLabel welcomeLabel = new JLabel("Representative Control Panel", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        add(welcomeLabel);

        // Requirement: Reply to questions & Retrieve Waitlists [cite: 111, 113]
        JButton supportBtn = new JButton("Customer Support (Q&A & Waitlists)");
        add(supportBtn);

        // Requirement: Add, Edit, Delete Infrastructure [cite: 110]
        JButton manageBtn = new JButton("Manage Aircraft, Airports, & Flights");
        add(manageBtn);

        // Requirement: Make/Edit reservations for users [cite: 108, 109]
        JButton actingBtn = new JButton("Acting for Users (Search & Book)");
        add(actingBtn);

        JButton logoutBtn = new JButton("Log Out");
        add(logoutBtn);

        // --- Action Listeners ---

        // Launches the Q&A and Waitlist support window [cite: 111, 113]
        supportBtn.addActionListener(e -> new RepSupportFrame().setVisible(true));
        
        // Launches the infrastructure management console [cite: 110]
        manageBtn.addActionListener(e -> new RepManagementFrame().setVisible(true));

        // UPDATED: Launches FlightSearchFrame in "Representative Mode" [cite: 108, 109, 238]
        actingBtn.addActionListener(e -> new FlightSearchFrame(repUsername, true).setVisible(true));

        logoutBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
    }
}