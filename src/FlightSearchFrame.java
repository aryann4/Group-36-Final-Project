package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class FlightSearchFrame extends JFrame {
    private JTextField fromField, toField;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private String currentUsername;

    public FlightSearchFrame(String username) {
        this.currentUsername = username;
        setTitle("Search & Book Flights");
        setSize(800, 550); // Slightly wider for better column spacing
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Top Panel: Search Inputs
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("From:"));
        fromField = new JTextField(5);
        topPanel.add(fromField);
        topPanel.add(new JLabel("To:"));
        toField = new JTextField(5);
        topPanel.add(toField);
        JButton searchBtn = new JButton("Search");
        topPanel.add(searchBtn);
        add(topPanel, BorderLayout.NORTH);

        // Center: Results Table
        String[] columns = {"Flight #", "Airline ID", "Airline Name", "From", "To", "Date", "Departs", "Arrives"};
        tableModel = new DefaultTableModel(columns, 0);
        resultsTable = new JTable(tableModel);
        add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        // Bottom: Booking Action
        JButton bookBtn = new JButton("Book Selected Flight");
        add(bookBtn, BorderLayout.SOUTH);

        // --- LISTENERS ---

        searchBtn.addActionListener(e -> performSearch());

        bookBtn.addActionListener(e -> {
            int row = resultsTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a flight from the table first.");
                return;
            }

            // 1. Extract Flight Info from the selected row
            String fNum = (String) tableModel.getValueAt(row, 0);
            String aId = (String) tableModel.getValueAt(row, 1);
            int cId = DatabaseHelper.getCustomerId(currentUsername);

            // 2. Duplicate Check / Re-booking Warning
            if (DatabaseHelper.isAlreadyBooked(cId, fNum, aId)) {
                int rebookChoice = JOptionPane.showConfirmDialog(this, 
                    "You already have an active reservation for Flight " + fNum + ". \n" +
                    "Would you like to add more seats to your booking?", 
                    "Existing Booking Found", JOptionPane.YES_NO_OPTION);
                
                if (rebookChoice != JOptionPane.YES_OPTION) return;
            }

            // 3. Gatekeeper: Is the flight full?
            // We check total capacity across all classes for a baseline
            int totalLeft = DatabaseHelper.getSeatsRemaining(fNum, "Economy") + 
                            DatabaseHelper.getSeatsRemaining(fNum, "Business") + 
                            DatabaseHelper.getSeatsRemaining(fNum, "First");

            if (totalLeft <= 0) {
                // Prevent duplicate waiting list entries
                if (DatabaseHelper.isAlreadyOnWaitingList(cId, fNum)) {
                    JOptionPane.showMessageDialog(this, "You are already on the waiting list for this flight.");
                    return;
                }

                int choice = JOptionPane.showConfirmDialog(this, 
                    "Flight " + fNum + " is fully booked. Join the Waiting List?", 
                    "Flight Full", JOptionPane.YES_NO_OPTION);
                
                if (choice == JOptionPane.YES_OPTION) {
                    DatabaseHelper.addToWaitingList(cId, fNum, aId, "Economy");
                    // Fetch the real-time position
                    int pos = DatabaseHelper.getWaitlistPosition(cId, fNum);
                    JOptionPane.showMessageDialog(this, "Added to Waiting List! You are currently at position: " + pos);
                }
                return;
            }

            // 4. Normal Booking Flow
            BookingOptionsDialog dialog = new BookingOptionsDialog(this, fNum);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                // Pass all 8 parameters to the updated bookFlight (including Qty and Meal)
                boolean success = DatabaseHelper.bookFlight(
                    cId, 
                    fNum, 
                    aId, 
                    dialog.getSelectedClass(), 
                    dialog.isFlexible(), 
                    dialog.getTotalFare(), 
                    dialog.hasSpecialMeal(), 
                    dialog.getQuantity()
                );

                if (success) {
                    JOptionPane.showMessageDialog(this, "Success! Your reservation has been updated/confirmed.");
                } else {
                    JOptionPane.showMessageDialog(this, "An error occurred during booking. Please try again.");
                }
            }
        });
    }

    private void performSearch() {
        tableModel.setRowCount(0);
        
        // This query joins Flight and Airline to show the full Airline Name
        String query = "SELECT f.flight_number, a.name, f.departure_airport, f.arrival_airport, " +
                       "f.departure_time, f.arrival_time, f.airline_id " + 
                       "FROM Flight f JOIN Airline a ON f.airline_id = a.airline_id " +
                       "WHERE (? = '' OR f.departure_airport = ?) " +
                       "AND (? = '' OR f.arrival_airport = ?)";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            String from = fromField.getText().trim();
            String to = toField.getText().trim();
            
            stmt.setString(1, from); stmt.setString(2, from);
            stmt.setString(3, to);   stmt.setString(4, to);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("flight_number"), 
                    rs.getString("airline_id"), 
                    rs.getString("name"), // Airline Name
                    rs.getString("departure_airport"), 
                    rs.getString("arrival_airport"), 
                    "2026-04-26", // Placeholder for project date
                    rs.getString("departure_time"), 
                    rs.getString("arrival_time")
                });
            }
        } catch (SQLException ex) { 
            ex.printStackTrace(); 
            JOptionPane.showMessageDialog(this, "Search Error: " + ex.getMessage());
        }
    }
}