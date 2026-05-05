package src;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;

public class MyReservationsFrame extends JFrame {
    private DefaultTableModel model;
    private String username;

    public MyReservationsFrame(String username) {
        this.username = username;
        setTitle("My Reservations - " + username);
        setSize(850, 450); 
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        String[] columns = {"Ticket #", "Flight #", "Airline", "Route", "Dep. Date", "Arr. Date", "Class", "Qty", "Flex", "Fare", "Status"};
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
            if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a reservation first."); return; }

            int    ticketNum     = (int)    model.getValueAt(row, 0);
            String ticketClass   =          model.getValueAt(row, 6).toString().trim();
            String displayStatus =          model.getValueAt(row, 10).toString().trim();

            if (displayStatus.equalsIgnoreCase("completed")) {
                // Past flight — just remove from history view
                if (JOptionPane.showConfirmDialog(this,
                        "Remove completed flight record #" + ticketNum + " from your history?",
                        "Clear History", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    if (DatabaseHelper.cancelBooking(ticketNum)) refreshData();
                }

            } else if (ticketClass.equalsIgnoreCase("Economy")) {
                // Economy: two-step confirmation with mandatory fee disclosure
                int step1 = JOptionPane.showConfirmDialog(this,
                    "ECONOMY TICKET CANCELLATION POLICY\n\n" +
                    "Economy tickets are non-refundable without a cancellation fee.\n" +
                    "A $50.00 fee will be charged for Ticket #" + ticketNum + ".\n\n" +
                    "Do you understand and wish to proceed?",
                    "Cancellation Fee Required", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (step1 == JOptionPane.YES_OPTION) {
                    int step2 = JOptionPane.showConfirmDialog(this,
                        "FINAL CONFIRMATION\n\n" +
                        "Ticket #" + ticketNum + " will be permanently cancelled.\n" +
                        "A $50.00 cancellation fee has been recorded against this booking.\n\n" +
                        "Confirm cancellation?",
                        "Confirm Economy Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                    if (step2 == JOptionPane.YES_OPTION) {
                        if (DatabaseHelper.cancelBookingWithFee(ticketNum, 50.0f)) {
                            JOptionPane.showMessageDialog(this,
                                "Ticket #" + ticketNum + " cancelled.\nA $50.00 cancellation fee has been applied.",
                                "Cancellation Complete", JOptionPane.INFORMATION_MESSAGE);
                            refreshData();
                        }
                    }
                }

            } else {
                // Business / First — free cancellation
                if (JOptionPane.showConfirmDialog(this,
                        "Cancel Ticket #" + ticketNum + "?\n" +
                        ticketClass + " class tickets can be cancelled at no charge.",
                        "Confirm Cancellation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    if (DatabaseHelper.cancelBooking(ticketNum)) {
                        JOptionPane.showMessageDialog(this, "Ticket #" + ticketNum + " cancelled successfully.");
                        refreshData();
                    }
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
                Date arrDate = rs.getDate("arr_date");
                Time arrTime = rs.getTime("arr_time");
                
                String displayStatus = "active";
                if (arrDate != null && arrTime != null) {
                    LocalDateTime arrival = LocalDateTime.of(arrDate.toLocalDate(), arrTime.toLocalTime());
                    if (LocalDateTime.now().isAfter(arrival)) {
                        displayStatus = "completed";
                    }
                }

                model.addRow(new Object[]{
                    rs.getInt("ticket_number"),
                    rs.getString("flight_number"),
                    rs.getString("airline_name"),
                    rs.getString("from_airport") + " -> " + rs.getString("to_airport"),
                    rs.getDate("dep_date"), 
                    rs.getDate("arr_date"), 
                    rs.getString("class"),
                    rs.getInt("quantity"),
                    rs.getBoolean("is_flexible") ? "Yes" : "No",
                    "$" + rs.getFloat("total_fare"),
                    displayStatus 
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
