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

        JButton supportBtn = new JButton("Customer Support (Q&A & Waitlists)");
        add(supportBtn);

        JButton manageBtn = new JButton("Manage Aircraft, Airports, & Flights");
        add(manageBtn);

        JButton actingBtn = new JButton("Acting for Users (Search & Book)");
        add(actingBtn);

        JButton logoutBtn = new JButton("Log Out");
        add(logoutBtn);

        supportBtn.addActionListener(e -> new RepSupportFrame().setVisible(true));
        
        manageBtn.addActionListener(e -> new RepManagementFrame().setVisible(true));

        actingBtn.addActionListener(e -> new FlightSearchFrame(repUsername, true).setVisible(true));

        logoutBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
    }
}