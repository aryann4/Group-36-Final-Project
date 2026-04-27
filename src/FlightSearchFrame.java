package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class FlightSearchFrame extends JFrame {
    private JTextField fromField, toField, dateField, returnDateField, maxPriceField;
    private JCheckBox flexCheck;
    private JRadioButton oneWayRadio, roundTripRadio;
    private JComboBox<String> sortBox, airlineFilter;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private String currentUsername;

    private boolean isSelectingReturn = false;
    private Object[] outboundLegData = null;

    public FlightSearchFrame(String username) {
        this.currentUsername = username;
        setTitle("Search & Book Flights");
        setSize(1100, 650);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        JPanel p1 = new JPanel();
        oneWayRadio = new JRadioButton("One-Way", true);
        roundTripRadio = new JRadioButton("Round-Trip");
        ButtonGroup tripGroup = new ButtonGroup();
        tripGroup.add(oneWayRadio); tripGroup.add(roundTripRadio);
        p1.add(oneWayRadio); p1.add(roundTripRadio);

        p1.add(new JLabel("From:")); fromField = new JTextField(4); p1.add(fromField);
        p1.add(new JLabel("To:")); toField = new JTextField(4); p1.add(toField);
        p1.add(new JLabel("Dep. Date:")); dateField = new JTextField("2026-04-26", 8); p1.add(dateField);
        
        JLabel retLbl = new JLabel("Ret. Date:");
        returnDateField = new JTextField("2026-04-30", 8);
        returnDateField.setEnabled(false); 
        p1.add(retLbl); p1.add(returnDateField);
        
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

        String[] columns = {"Flight(s) #", "Airline ID", "Airline", "From", "To", "Dep. Date", "Arr. Date", "Departs", "Arrives", "Stops", "Price"};
        tableModel = new DefaultTableModel(columns, 0);
        resultsTable = new JTable(tableModel);
        add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        JButton bookBtn = new JButton("Book Selected Flight");
        add(bookBtn, BorderLayout.SOUTH);

        roundTripRadio.addActionListener(e -> returnDateField.setEnabled(true));
        oneWayRadio.addActionListener(e -> {
            returnDateField.setEnabled(false);
            isSelectingReturn = false; 
            outboundLegData = null;
        });

        searchBtn.addActionListener(e -> performSearch());

        bookBtn.addActionListener(e -> {
            int row = resultsTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a flight leg first.");
                return;
            }

            // Case 1: Handle Round-Trip Outbound Selection
            if (roundTripRadio.isSelected() && !isSelectingReturn) {
                int confirm = JOptionPane.showConfirmDialog(this, "Outbound flight selected! Search for your return flight now?", "Outbound Confirmed", JOptionPane.YES_NO_OPTION);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    outboundLegData = new Object[tableModel.getColumnCount()];
                    for (int i = 0; i < tableModel.getColumnCount(); i++) {
                        outboundLegData[i] = tableModel.getValueAt(row, i);
                    }
                    isSelectingReturn = true;
                    // Auto-flip for consistency
                    fromField.setText((String) outboundLegData[4]); 
                    toField.setText((String) outboundLegData[3]);   
                    airlineFilter.setSelectedItem((String) outboundLegData[1]);
                    performSearch(); 
                    return;
                } else {
                    // Reset to one-way logic if user says No
                    isSelectingReturn = false;
                    outboundLegData = null;
                }
            }

            // Case 2: Finalizing Selection (One-Way or Return leg)
            String fNum = (String) tableModel.getValueAt(row, 0);
            String aId = (String) tableModel.getValueAt(row, 1);
            float currentPrice = Float.parseFloat(((String) tableModel.getValueAt(row, 10)).replace("$", ""));

            String finalFlightList = fNum;
            float finalBasePrice = currentPrice;
            String routeDesc = tableModel.getValueAt(row, 3) + " -> " + tableModel.getValueAt(row, 4);

            if (roundTripRadio.isSelected() && outboundLegData != null) {
                finalFlightList = outboundLegData[0] + "," + fNum;
                float outboundPrice = Float.parseFloat(((String) outboundLegData[10]).replace("$", ""));
                finalBasePrice = outboundPrice + currentPrice;
                routeDesc = outboundLegData[3] + " <-> " + outboundLegData[4] + " (Round Trip)";
            }

            String depInfo = tableModel.getValueAt(row, 5) + " @ " + tableModel.getValueAt(row, 7);
            String arrInfo = tableModel.getValueAt(row, 6) + " @ " + tableModel.getValueAt(row, 8);

            BookingOptionsDialog dialog = new BookingOptionsDialog(this, finalFlightList, routeDesc, depInfo, arrInfo, finalBasePrice);
            dialog.setVisible(true);

            if (dialog.isConfirmed()) {
                int cId = DatabaseHelper.getCustomerId(currentUsername);
                boolean success = DatabaseHelper.bookFlight(
                    cId, finalFlightList, aId, dialog.getSelectedClass(), 
                    dialog.isFlexible(), dialog.getTotalFare(), 
                    dialog.hasSpecialMeal(), dialog.getQuantity()
                );
                
                if (success) {
                    JOptionPane.showMessageDialog(this, "Trip Successfully Booked!");
                    isSelectingReturn = false;
                    outboundLegData = null;
                    performSearch(); 
                }
            }
        });
    }

    private void performSearch() {
        if (dateField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a departure date.");
            return;
        }

        tableModel.setRowCount(0);
        String from = fromField.getText().trim();
        String to = toField.getText().trim();
        String date = isSelectingReturn ? returnDateField.getText().trim() : dateField.getText().trim();
        
        String airline = (String) airlineFilter.getSelectedItem();
        String maxPrice = maxPriceField.getText().trim();
        String sortBy = (String) sortBox.getSelectedItem();

        String fromCond = from.isEmpty() ? "1=1" : "departure_airport = '" + from + "'";
        String toCond = to.isEmpty() ? "1=1" : "arrival_airport = '" + to + "'";
        String fromCondV = from.isEmpty() ? "1=1" : "i.dep = '" + from + "'";
        String toCondV = to.isEmpty() ? "1=1" : "i.dest = '" + to + "'";

        StringBuilder sql = new StringBuilder(
            "SELECT * FROM (" +
            "SELECT f.flight_number AS flights, f.airline_id, a.name, f.departure_airport, f.arrival_airport, " +
            "f.flight_date, f.arrival_date, f.departure_time, f.arrival_time, 0 AS stops, f.base_price " +
            "FROM Flight f JOIN Airline a ON f.airline_id = a.airline_id " +
            "WHERE " + fromCond + " AND " + toCond + " " +
            "AND f.flight_date " + (flexCheck.isSelected() ? "BETWEEN DATE_SUB('" + date + "', INTERVAL 3 DAY) AND DATE_ADD('" + date + "', INTERVAL 3 DAY)" : "= '" + date + "'") +
            " UNION " +
            "SELECT CONCAT(i.f1_num, ',', i.f2_num) AS flights, i.air1, a.name, i.dep, i.dest, " +
            "i.date1, i.date2_arr, i.dep_t1, i.arr_t2, 1 AS stops, i.total_price AS base_price " +
            "FROM IndirectFlights i JOIN Airline a ON i.air1 = a.airline_id " +
            "WHERE " + fromCondV + " AND " + toCondV + " AND i.dep != i.dest " +
            "AND i.date1 " + (flexCheck.isSelected() ? "BETWEEN DATE_SUB('" + date + "', INTERVAL 3 DAY) AND DATE_ADD('" + date + "', INTERVAL 3 DAY)" : "= '" + date + "'") +
            ") as combined_results WHERE 1=1 "
        );

        if (!airline.equals("All")) sql.append(" AND airline_id = '").append(airline).append("'");
        if (!maxPrice.isEmpty()) sql.append(" AND base_price <= ").append(maxPrice);

        if (sortBy.contains("Price")) sql.append(" ORDER BY base_price ASC");
        else if (sortBy.contains("Departure")) sql.append(" ORDER BY flight_date ASC, departure_time ASC");
        else if (sortBy.contains("Arrival")) sql.append(" ORDER BY arrival_date ASC, arrival_time ASC");
        else if (sortBy.contains("Duration")) {
            sql.append(" ORDER BY TIMESTAMPDIFF(MINUTE, CONCAT(flight_date, ' ', departure_time), CONCAT(arrival_date, ' ', arrival_time)) ASC");
        }

        try (Connection conn = DatabaseHelper.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql.toString())) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("flights"), rs.getString("airline_id"), rs.getString("name"),
                    rs.getString("departure_airport"), rs.getString("arrival_airport"), 
                    rs.getDate("flight_date"), rs.getDate("arrival_date"), 
                    rs.getTime("departure_time"), rs.getTime("arrival_time"), 
                    rs.getInt("stops") == 0 ? "Non-stop" : "1 Stop",
                    "$" + rs.getFloat("base_price")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
}