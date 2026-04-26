package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class FlightSearchFrame extends JFrame {
    private JTextField fromField, toField, dateField, maxPriceField;
    private JCheckBox flexCheck;
    private JComboBox<String> sortBox, airlineFilter;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private String currentUsername;

    public FlightSearchFrame(String username) {
        this.currentUsername = username;
        setTitle("Search & Book Flights");
        setSize(950, 650);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // --- Top Panel: Search, Sort, and Filter ---
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridLayout(3, 1));

        JPanel p1 = new JPanel();
        p1.add(new JLabel("From:")); fromField = new JTextField(4); p1.add(fromField);
        p1.add(new JLabel("To:")); toField = new JTextField(4); p1.add(toField);
        p1.add(new JLabel("Date (YYYY-MM-DD):")); dateField = new JTextField("2026-04-26", 8); p1.add(dateField);
        flexCheck = new JCheckBox("Flexible (+/- 3 days)"); p1.add(flexCheck);
        topPanel.add(p1);

        JPanel p2 = new JPanel();
        p2.add(new JLabel("Max Price:")); maxPriceField = new JTextField(5); p2.add(maxPriceField);
        p2.add(new JLabel("Airline:")); airlineFilter = new JComboBox<>(new String[]{"All", "UA", "AA"}); p2.add(airlineFilter);
        topPanel.add(p2);

        JPanel p3 = new JPanel();
        p3.add(new JLabel("Sort By:"));
        sortBox = new JComboBox<>(new String[]{"None", "Price (Low to High)", "Departure Time", "Arrival Time", "Flight Duration"});
        p3.add(sortBox);
        JButton searchBtn = new JButton("Search Flights"); p3.add(searchBtn);
        topPanel.add(p3);
        add(topPanel, BorderLayout.NORTH);

        // --- Center: Results ---
        // Updated columns to match the 10 data points added in performSearch
        String[] columns = {"Flight #", "Airline ID", "Airline", "From", "To", "Dep. Date", "Arr. Date", "Departs", "Arrives", "Base Price"};
        tableModel = new DefaultTableModel(columns, 0);
        resultsTable = new JTable(tableModel);
        add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        // --- Bottom: Booking ---
        JButton bookBtn = new JButton("Book Selected Flight");
        add(bookBtn, BorderLayout.SOUTH);

        // --- LISTENERS ---
        searchBtn.addActionListener(e -> performSearch());

        bookBtn.addActionListener(e -> {
            int row = resultsTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a flight first.");
                return;
            }

            // --- EXTRACT DYNAMIC DATA FROM THE TABLE ---
            String fNum = (String) tableModel.getValueAt(row, 0);
            String aId = (String) tableModel.getValueAt(row, 1);
            
            // Route: From (Col 3) -> To (Col 4)
            String route = tableModel.getValueAt(row, 3) + " -> " + tableModel.getValueAt(row, 4);
            
            // Departure: Date (Col 5) @ Time (Col 7)
            String depInfo = tableModel.getValueAt(row, 5) + " @ " + tableModel.getValueAt(row, 7);
            
            // Arrival: Date (Col 6) @ Time (Col 8)
            String arrInfo = tableModel.getValueAt(row, 6) + " @ " + tableModel.getValueAt(row, 8);
            
            // Extracts and cleans the price (Col 9) for the dialog [cite: 36, 65]
            float bPrice = Float.parseFloat(((String) tableModel.getValueAt(row, 9)).replace("$", ""));

            int cId = DatabaseHelper.getCustomerId(currentUsername);

            // --- 2. GATEKEEPER: CAPACITY & WAITING LIST ---
            int totalLeft = DatabaseHelper.getSeatsRemaining(fNum, "Economy") + 
                            DatabaseHelper.getSeatsRemaining(fNum, "Business") + 
                            DatabaseHelper.getSeatsRemaining(fNum, "First");

            if (totalLeft <= 0) {
                if (DatabaseHelper.isAlreadyOnWaitingList(cId, fNum)) {
                    JOptionPane.showMessageDialog(this, "You are already on the waiting list for this flight.");
                    return;
                }
                int choice = JOptionPane.showConfirmDialog(this, "Flight is full. Join Waiting List?", "Full", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    DatabaseHelper.addToWaitingList(cId, fNum, aId, "Economy");
                    int pos = DatabaseHelper.getWaitlistPosition(cId, fNum);
                    JOptionPane.showMessageDialog(this, "Added to Waitlist! Your position is: " + pos);
                }
                return;
            }

            // --- 3. NORMAL FLOW: OPEN DIALOG WITH DYNAMIC DATA ---
            // Now calling constructor with 6 arguments
            BookingOptionsDialog dialog = new BookingOptionsDialog(this, fNum, route, depInfo, arrInfo, bPrice);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                boolean success = DatabaseHelper.bookFlight(
                    cId, fNum, aId, dialog.getSelectedClass(), 
                    dialog.isFlexible(), dialog.getTotalFare(), 
                    dialog.hasSpecialMeal(), dialog.getQuantity()
                );
                if (success) JOptionPane.showMessageDialog(this, "Booking confirmed!");
            }
        });
    }

    private void performSearch() {
        if (dateField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a date (YYYY-MM-DD) to search.");
            return;
        }

        tableModel.setRowCount(0);
        String from = fromField.getText().trim();
        String to = toField.getText().trim();
        String date = dateField.getText().trim();
        String maxPrice = maxPriceField.getText().trim();
        String airline = (String) airlineFilter.getSelectedItem();
        String sortBy = (String) sortBox.getSelectedItem();

        StringBuilder sql = new StringBuilder("SELECT f.*, a.name FROM Flight f JOIN Airline a ON f.airline_id = a.airline_id WHERE 1=1 ");
        if (!from.isEmpty()) sql.append(" AND f.departure_airport = '").append(from).append("'");
        if (!to.isEmpty()) sql.append(" AND f.arrival_airport = '").append(to).append("'");
        if (!airline.equals("All")) sql.append(" AND f.airline_id = '").append(airline).append("'");
        if (!maxPrice.isEmpty()) sql.append(" AND f.base_price <= ").append(maxPrice);

        if (flexCheck.isSelected()) {
            sql.append(" AND f.flight_date BETWEEN DATE_SUB('").append(date).append("', INTERVAL 3 DAY) AND DATE_ADD('").append(date).append("', INTERVAL 3 DAY) ");
        } else {
            sql.append(" AND f.flight_date = '").append(date).append("'");
        }

        if (sortBy.contains("Price")) {
            sql.append(" ORDER BY f.base_price ASC");
        } else if (sortBy.contains("Departure")) {
            sql.append(" ORDER BY f.flight_date ASC, f.departure_time ASC");
        } else if (sortBy.contains("Arrival")) {
            sql.append(" ORDER BY f.arrival_date ASC, f.arrival_time ASC");
        } else if (sortBy.contains("Duration")) {
            sql.append(" ORDER BY TIMESTAMPDIFF(MINUTE, CONCAT(f.flight_date, ' ', f.departure_time), CONCAT(f.arrival_date, ' ', f.arrival_time)) ASC");
        }

        try (Connection conn = DatabaseHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql.toString())) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("flight_number"), 
                    rs.getString("airline_id"), 
                    rs.getString("name"),
                    rs.getString("departure_airport"), 
                    rs.getString("arrival_airport"), 
                    rs.getDate("flight_date"),
                    rs.getDate("arrival_date"), 
                    rs.getTime("departure_time"), 
                    rs.getTime("arrival_time"), 
                    "$" + rs.getFloat("base_price")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
}