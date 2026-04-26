package src;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class MyReservationsFrame extends JFrame {
    private DefaultTableModel model;
    private String username;

    public MyReservationsFrame(String username) {
        this.username = username;
        setTitle("My Reservations - " + username);
        setSize(750, 450); // Increased width slightly for the Route column
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // Added "Route" to the columns list
        String[] columns = {"Ticket #", "Flight #", "Airline", "Route", "Date", "Class", "Qty", "Flex", "Fare", "Status"};
        model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int row = table.getSelectedRow();
                int ticketID = (int) model.getValueAt(row, 0);

                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                         "SELECT s.seat_number, s.special_meal, t.quantity " +
                         "FROM Ticket t JOIN Ticket_Segment s ON t.ticket_number = s.ticket_number " +
                         "WHERE t.ticket_number = ?")) {
                    
                    stmt.setInt(1, ticketID);
                    ResultSet rs = stmt.executeQuery();
                    
                    java.util.List<String> allSeats = new java.util.ArrayList<>();
                    int qty = 0;
                    boolean meal = false;

                    while (rs.next()) {
                        allSeats.add(rs.getString("seat_number"));
                        qty = rs.getInt("quantity");
                        meal = rs.getBoolean("special_meal");
                    }

                    if (!allSeats.isEmpty()) {
                        JOptionPane.showMessageDialog(this, 
                            "--- Official Ticket Details ---\n" +
                            "Ticket ID: " + ticketID + "\n" +
                            "Quantity: " + qty + " Ticket(s)\n" +
                            "Assigned Seats: " + String.join(", ", allSeats) + "\n" + 
                            "Special Meal: " + (meal ? "Yes" : "No"),
                            "Reservation Info", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (SQLException ex) { 
                    ex.printStackTrace(); 
                }
            }
        });

        JPanel bottomPanel = new JPanel();
        JButton refreshBtn = new JButton("Refresh List");
        JButton cancelBtn = new JButton("Cancel Selected Booking");
        JButton closeBtn = new JButton("Close");
        bottomPanel.add(refreshBtn); bottomPanel.add(cancelBtn); bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        refreshData();

        refreshBtn.addActionListener(e -> refreshData());
        cancelBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int ticketNum = (int) model.getValueAt(row, 0);
                if (JOptionPane.showConfirmDialog(this, "Cancel Ticket #" + ticketNum + "?") == JOptionPane.YES_OPTION) {
                    if (DatabaseHelper.cancelBooking(ticketNum)) refreshData();
                }
            }
        });
        closeBtn.addActionListener(e -> dispose());
    }

    private void refreshData() {
        model.setRowCount(0);
        int customerId = DatabaseHelper.getCustomerId(username);
        try (ResultSet rs = DatabaseHelper.getCustomerTickets(customerId)) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("ticket_number"),
                    rs.getString("flight_number"),
                    rs.getString("airline_name"), // Fetched via the new join [cite: 26]
                    rs.getString("from_airport") + " -> " + rs.getString("to_airport"), // Displays the route [cite: 35]
                    rs.getDate("flight_date"),
                    rs.getString("class"),
                    rs.getInt("quantity"),
                    rs.getBoolean("is_flexible") ? "Yes" : "No",
                    "$" + rs.getFloat("total_fare"),
                    rs.getString("status")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}