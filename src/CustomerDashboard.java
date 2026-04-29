package src;
import javax.swing.*;
import java.awt.*;

public class CustomerDashboard extends JFrame {
    public CustomerDashboard(String username) {

        String creationDate = DatabaseHelper.getAccountCreationDate(username);

        setTitle("Customer Dashboard - " + username);
        setSize(500, 450); // Increased height slightly for the new button
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        JLabel welcomeLabel = new JLabel("Welcome, " + username + "!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        add(welcomeLabel, BorderLayout.NORTH);

        // Buttons for main features
        JPanel buttonPanel = new JPanel();
        // Updated to 4 rows to include the Q&A button
        buttonPanel.setLayout(new GridLayout(4, 1, 10, 10));

        JButton searchButton = new JButton("Search for Flights");
        JButton viewTicketsButton = new JButton("My Reservations");
        JButton qaBtn = new JButton("Help & Q&A");
        JButton logoutButton = new JButton("Logout");

        buttonPanel.add(searchButton);
        buttonPanel.add(viewTicketsButton);
        buttonPanel.add(qaBtn);
        buttonPanel.add(logoutButton);

        add(buttonPanel, BorderLayout.CENTER);

        JLabel footerLabel = new JLabel("Member since: " + creationDate, SwingConstants.RIGHT);
        add(footerLabel, BorderLayout.SOUTH);

        // Logout functionality
        logoutButton.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        // Search Screen link
        searchButton.addActionListener(e -> {
            new FlightSearchFrame(username).setVisible(true);
        });

        // My Reservations link
        viewTicketsButton.addActionListener(e -> {
            new MyReservationsFrame(username).setVisible(true);
        });

        // Help & Q&A link
        qaBtn.addActionListener(e -> {
            new QAFrame(username).setVisible(true);
        });
    }
}