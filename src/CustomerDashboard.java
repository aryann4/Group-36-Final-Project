package src;
import javax.swing.*;
import java.awt.*;

public class CustomerDashboard extends JFrame {
    public CustomerDashboard(String username) {
        String creationDate = DatabaseHelper.getAccountCreationDate(username);
        int customerId = DatabaseHelper.getCustomerId(username);

        setTitle("Customer Dashboard - " + username);
        setSize(500, 450); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        JLabel welcomeLabel = new JLabel("Welcome, " + username + "!", SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
        add(welcomeLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
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

        String alerts = DatabaseHelper.checkWaitlistAlerts(customerId);
        if (alerts != null && !alerts.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "WAITLIST ALERT!\nSeats have become available for the following flights:\n" + alerts + 
                "\n\nGo to 'Search for Flights' to book your seat now!", 
                "Flight Availability Notification", 
                JOptionPane.INFORMATION_MESSAGE);
        }

        logoutButton.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        searchButton.addActionListener(e -> {
            new FlightSearchFrame(username, false).setVisible(true);
        });

        viewTicketsButton.addActionListener(e -> {
            new MyReservationsFrame(username).setVisible(true);
        });

        qaBtn.addActionListener(e -> {
            new QAFrame(username).setVisible(true);
        });
    }
}