package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class FlightSearchFrame extends JFrame {
    private JTextField fromField, toField, dateField, returnDateField, maxPriceField;
    private JTextField targetUserField, editCustUserField;
    private JComboBox<String> stopsFilter;
    private JTextField depTimeFromField, depTimeToField, arrTimeFromField, arrTimeToField;
    private JCheckBox flexCheck;
    private JRadioButton oneWayRadio, roundTripRadio;
    private JComboBox<String> sortBox, airlineFilter;
    private JTable resultsTable, editTicketsTable;
    private DefaultTableModel tableModel, editTableModel;
    private String currentUsername;
    private boolean isRepresentative;

    private boolean isSelectingReturn = false;
    private Object[] outboundLegData = null;

    public FlightSearchFrame(String username, boolean isRepresentative) {
        this.currentUsername = username;
        this.isRepresentative = isRepresentative;
        setTitle(isRepresentative ? "Acting for Users - Search & Book" : "Search & Book Flights");
        setSize(1150, 800);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        if (isRepresentative) {
            JTabbedPane mainTabs = new JTabbedPane();
            mainTabs.addTab("New Reservation", createSearchPanel());
            mainTabs.addTab("Edit Existing Reservation", createEditPanel());
            add(mainTabs, BorderLayout.CENTER);
        } else {
            add(createSearchPanel(), BorderLayout.CENTER);
        }
    }

    private JPanel createSearchPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        int rows = isRepresentative ? 5 : 4;
        JPanel topPanel = new JPanel(new GridLayout(rows, 1));


        if (isRepresentative) {
            JPanel repPanel = new JPanel();
            repPanel.setBackground(new Color(230, 240, 255));
            repPanel.add(new JLabel("Acting for Customer (Enter Username):"));
            targetUserField = new JTextField(12);
            repPanel.add(targetUserField);
            topPanel.add(repPanel);
        }


        JPanel p1 = new JPanel();
        oneWayRadio   = new JRadioButton("One-Way", true);
        roundTripRadio = new JRadioButton("Round-Trip");
        ButtonGroup tripGroup = new ButtonGroup();
        tripGroup.add(oneWayRadio); tripGroup.add(roundTripRadio);
        p1.add(oneWayRadio); p1.add(roundTripRadio);
        p1.add(new JLabel("From:")); fromField = new JTextField(4); p1.add(fromField);
        p1.add(new JLabel("To:"));   toField   = new JTextField(4); p1.add(toField);
        p1.add(new JLabel("Dep. Date:")); dateField = new JTextField("2026-05-08", 8); p1.add(dateField);
        JLabel retLbl = new JLabel("Ret. Date:");
        returnDateField = new JTextField("2026-05-12", 8);
        returnDateField.setEnabled(false);
        p1.add(retLbl); p1.add(returnDateField);
        flexCheck = new JCheckBox("Flexible (+/- 3 days)"); p1.add(flexCheck);
        topPanel.add(p1);


        JPanel p2 = new JPanel();
        p2.add(new JLabel("Max Price:")); maxPriceField = new JTextField(5); p2.add(maxPriceField);
        p2.add(new JLabel("Airline:"));
        airlineFilter = new JComboBox<>(new String[]{"All", "UA", "AA", "DL", "SW", "JB"}); p2.add(airlineFilter);
        p2.add(new JLabel("Stops:"));
        stopsFilter = new JComboBox<>(new String[]{"Any Stops", "Non-stop only", "1 Stop only"}); p2.add(stopsFilter);
        topPanel.add(p2);


        JPanel p3 = new JPanel();
        p3.add(new JLabel("Dep. Time From (HH:MM):")); depTimeFromField = new JTextField("00:00", 5); p3.add(depTimeFromField);
        p3.add(new JLabel("To:"));                     depTimeToField   = new JTextField("23:59", 5); p3.add(depTimeToField);
        p3.add(new JLabel("  Arr. Time From:"));        arrTimeFromField = new JTextField("00:00", 5); p3.add(arrTimeFromField);
        p3.add(new JLabel("To:"));                     arrTimeToField   = new JTextField("23:59", 5); p3.add(arrTimeToField);
        topPanel.add(p3);


        JPanel p4 = new JPanel();
        p4.add(new JLabel("Sort By:"));
        sortBox = new JComboBox<>(new String[]{"None", "Price (Low to High)", "Departure Time", "Arrival Time", "Flight Duration"});
        p4.add(sortBox);
        JButton searchBtn = new JButton("Search Flights"); p4.add(searchBtn);
        topPanel.add(p4);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        String[] columns = {"Flight(s) #", "Airline ID", "Airline", "From", "To", "Dep. Date", "Arr. Date", "Departs", "Arrives", "Stops", "Price"};
        tableModel  = new DefaultTableModel(columns, 0);
        resultsTable = new JTable(tableModel);
        mainPanel.add(new JScrollPane(resultsTable), BorderLayout.CENTER);

        JButton bookBtn = new JButton(isRepresentative ? "Confirm Reservation for Customer" : "Book Selected Flight");
        mainPanel.add(bookBtn, BorderLayout.SOUTH);

        roundTripRadio.addActionListener(e -> returnDateField.setEnabled(true));
        oneWayRadio.addActionListener(e -> {
            returnDateField.setEnabled(false);
            isSelectingReturn = false;
            outboundLegData   = null;
        });
        searchBtn.addActionListener(e -> performSearch());
        bookBtn.addActionListener(e -> handleBooking());

        return mainPanel;
    }

    private JPanel createEditPanel() {
        JPanel editPanel = new JPanel(new BorderLayout());
        JPanel top = new JPanel();
        top.add(new JLabel("Customer Username:"));
        editCustUserField = new JTextField(12); top.add(editCustUserField);
        JButton loadBtn = new JButton("View Customer Tickets"); top.add(loadBtn);
        editPanel.add(top, BorderLayout.NORTH);

        String[] cols = {"Ticket #", "Flight", "Airline", "Class", "Current Total", "Seat"};
        editTableModel  = new DefaultTableModel(cols, 0);
        editTicketsTable = new JTable(editTableModel);
        editPanel.add(new JScrollPane(editTicketsTable), BorderLayout.CENTER);

        JButton updateBtn = new JButton("Update Selected Reservation");
        editPanel.add(updateBtn, BorderLayout.SOUTH);

        loadBtn.addActionListener(e -> refreshEditTable());
        updateBtn.addActionListener(e -> {
            int row = editTicketsTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select a reservation to edit."); return; }

            int tNum      = (int)   editTableModel.getValueAt(row, 0);
            String oldCls = (String) editTableModel.getValueAt(row, 3);
            float oldPrice = (float) editTableModel.getValueAt(row, 4);

            String[] classes = {"Economy", "Business", "First"};
            String newClass  = (String) JOptionPane.showInputDialog(this, "Select New Class:",
                    "Edit Reservation", JOptionPane.QUESTION_MESSAGE, null, classes, oldCls);
            if (newClass == null) return;

            String newSeat = JOptionPane.showInputDialog("New Seat Number:", editTableModel.getValueAt(row, 5));
            if (newSeat == null) return;

            float newPrice = oldPrice;
            if (oldCls.equalsIgnoreCase("Economy") && !newClass.equalsIgnoreCase("Economy")) {
                newPrice += 50.0f;
                JOptionPane.showMessageDialog(this, "Upgrade from Economy. A $50 change fee has been applied.");
            }

            if (DatabaseHelper.updateTicketDetails(tNum, newClass, newSeat, newPrice)) {
                JOptionPane.showMessageDialog(this, "Reservation updated successfully!");
                refreshEditTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update database.");
            }
        });
        return editPanel;
    }

    private void refreshEditTable() {
        editTableModel.setRowCount(0);
        String user = editCustUserField.getText().trim();
        if (user.isEmpty()) return;
        try (ResultSet rs = DatabaseHelper.getCustomerTickets(user)) {
            while (rs.next()) {
                editTableModel.addRow(new Object[]{
                    rs.getInt("ticket_number"), rs.getString("flight_number"),
                    rs.getString("airline_id"), rs.getString("class"),
                    rs.getFloat("total_fare"),  rs.getString("seat_number")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void handleBooking() {
        int row = resultsTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Please select a flight leg first."); return; }

        int cId;
        if (isRepresentative) {
            String targetUser = targetUserField.getText().trim();
            if (targetUser.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter a Customer Username."); return; }
            cId = DatabaseHelper.getCustomerId(targetUser);
            if (cId == -1) { JOptionPane.showMessageDialog(this, "Invalid Customer Username."); return; }
        } else {
            cId = DatabaseHelper.getCustomerId(currentUsername);
        }

        if (roundTripRadio.isSelected() && !isSelectingReturn) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Outbound flight selected! Search for your return flight now?",
                "Outbound Confirmed", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                outboundLegData = new Object[tableModel.getColumnCount()];
                for (int i = 0; i < tableModel.getColumnCount(); i++) outboundLegData[i] = tableModel.getValueAt(row, i);
                isSelectingReturn = true;
                fromField.setText((String) outboundLegData[4]);
                toField.setText((String) outboundLegData[3]);
                airlineFilter.setSelectedItem((String) outboundLegData[1]);
                performSearch();
                return;
            }
        }

        String fNum  = (String) tableModel.getValueAt(row, 0);
        String aId   = (String) tableModel.getValueAt(row, 1);
        float basePrice = Float.parseFloat(((String) tableModel.getValueAt(row, 10)).replace("$", ""));

        String finalFlightList = fNum;
        float  finalBasePrice  = basePrice;
        String routeDesc = tableModel.getValueAt(row, 3) + " -> " + tableModel.getValueAt(row, 4);

        String ticketType = "one-way";

        if (roundTripRadio.isSelected() && outboundLegData != null) {
            finalFlightList = outboundLegData[0] + "," + fNum;
            float outboundPrice = Float.parseFloat(((String) outboundLegData[10]).replace("$", ""));
            finalBasePrice = outboundPrice + basePrice;
            routeDesc  = outboundLegData[3] + " <-> " + outboundLegData[4] + " (Round Trip)";
            ticketType = "round-trip";
        }

        String depInfo = tableModel.getValueAt(row, 5) + " @ " + tableModel.getValueAt(row, 7);
        String arrInfo = tableModel.getValueAt(row, 6) + " @ " + tableModel.getValueAt(row, 8);

        BookingOptionsDialog dialog = new BookingOptionsDialog(this, finalFlightList, routeDesc, depInfo, arrInfo, finalBasePrice);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            String selectedClass = dialog.getSelectedClass();
            int available = DatabaseHelper.getSeatsRemaining(finalFlightList, selectedClass);

            if (available > 0) {
                boolean success = DatabaseHelper.bookFlight(
                    cId, finalFlightList, aId, selectedClass,
                    dialog.isFlexible(), dialog.getTotalFare(),
                    dialog.hasSpecialMeal(), dialog.getQuantity(), ticketType
                );
                if (success) {
                    JOptionPane.showMessageDialog(this, "Reservation Successful!");
                    isSelectingReturn = false; outboundLegData = null;
                    performSearch();
                }
            } else {
                if (DatabaseHelper.isAlreadyOnWaitingList(cId, finalFlightList)) {
                    JOptionPane.showMessageDialog(this, "Customer is already on the waiting list for this trip.");
                } else {
                    int join = JOptionPane.showConfirmDialog(this,
                        "This flight is full in " + selectedClass + ".\nAdd customer to the Waiting List?",
                        "Flight Full", JOptionPane.YES_NO_OPTION);
                    if (join == JOptionPane.YES_OPTION) {
                        DatabaseHelper.addToWaitingList(cId, finalFlightList, aId, selectedClass);
                        int pos = DatabaseHelper.getWaitlistPosition(cId, finalFlightList);
                        JOptionPane.showMessageDialog(this, "Added to Waitlist. Position: " + pos);
                        isSelectingReturn = false; outboundLegData = null;
                        performSearch();
                    }
                }
            }
        }
    }

    private void performSearch() {
        if (dateField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a departure date.");
            return;
        }
        tableModel.setRowCount(0);
        String from    = fromField.getText().trim();
        String to      = toField.getText().trim();
        String date    = isSelectingReturn ? returnDateField.getText().trim() : dateField.getText().trim();
        String airline = (String) airlineFilter.getSelectedItem();
        String maxPrice = maxPriceField.getText().trim();
        String sortBy  = (String) sortBox.getSelectedItem();


        String stopsChoice  = (String) stopsFilter.getSelectedItem();
        String depFrom      = depTimeFromField.getText().trim();
        String depTo        = depTimeToField.getText().trim();
        String arrFrom      = arrTimeFromField.getText().trim();
        String arrTo        = arrTimeToField.getText().trim();

        if (!depFrom.isEmpty() && depFrom.length() == 5) depFrom += ":00";
        if (!depTo.isEmpty()   && depTo.length()   == 5) depTo   += ":00";
        if (!arrFrom.isEmpty() && arrFrom.length() == 5) arrFrom += ":00";
        if (!arrTo.isEmpty()   && arrTo.length()   == 5) arrTo   += ":00";

        String fromCond  = from.isEmpty() ? "1=1" : "departure_airport = '" + from + "'";
        String toCond    = to.isEmpty()   ? "1=1" : "arrival_airport = '"   + to   + "'";
        String fromCondV = from.isEmpty() ? "1=1" : "i.dep = '"  + from + "'";
        String toCondV   = to.isEmpty()   ? "1=1" : "i.dest = '" + to   + "'";

        String dateCond;
        if (flexCheck.isSelected()) {
            dateCond = "BETWEEN DATE_SUB('" + date + "', INTERVAL 3 DAY) AND DATE_ADD('" + date + "', INTERVAL 3 DAY)";
        } else {
            dateCond = "= '" + date + "'";
        }

        StringBuilder sql = new StringBuilder(
            "SELECT * FROM (" +
            "SELECT f.flight_number AS flights, f.airline_id, a.name, f.departure_airport, f.arrival_airport, " +
            "f.flight_date, f.arrival_date, f.departure_time, f.arrival_time, 0 AS stops, f.base_price " +
            "FROM Flight f JOIN Airline a ON f.airline_id = a.airline_id " +
            "WHERE " + fromCond + " AND " + toCond + " AND f.flight_date " + dateCond +
            " UNION " +
            "SELECT CONCAT(i.f1_num,',',i.f2_num) AS flights, i.air1, a.name, i.dep, i.dest, " +
            "i.date1, i.date2_arr, i.dep_t1, i.arr_t2, 1 AS stops, i.total_price AS base_price " +
            "FROM IndirectFlights i JOIN Airline a ON i.air1 = a.airline_id " +
            "WHERE " + fromCondV + " AND " + toCondV + " AND i.dep != i.dest AND i.date1 " + dateCond +
            ") AS combined_results WHERE 1=1 "
        );


        if (!airline.equals("All"))   sql.append(" AND airline_id = '").append(airline).append("'");
        if (!maxPrice.isEmpty())       sql.append(" AND base_price <= ").append(maxPrice);


        if (stopsChoice.equals("Non-stop only")) sql.append(" AND stops = 0");
        if (stopsChoice.equals("1 Stop only"))   sql.append(" AND stops = 1");

        if (!depFrom.isEmpty()) sql.append(" AND departure_time >= '").append(depFrom).append("'");
        if (!depTo.isEmpty())   sql.append(" AND departure_time <= '").append(depTo).append("'");


        if (!arrFrom.isEmpty()) sql.append(" AND arrival_time >= '").append(arrFrom).append("'");
        if (!arrTo.isEmpty())   sql.append(" AND arrival_time <= '").append(arrTo).append("'");

 
        if      (sortBy.contains("Price"))     sql.append(" ORDER BY base_price ASC");
        else if (sortBy.contains("Departure")) sql.append(" ORDER BY flight_date ASC, departure_time ASC");
        else if (sortBy.contains("Arrival"))   sql.append(" ORDER BY arrival_date ASC, arrival_time ASC");
        else if (sortBy.contains("Duration"))  sql.append(" ORDER BY TIMESTAMPDIFF(MINUTE, CONCAT(flight_date,' ',departure_time), CONCAT(arrival_date,' ',arrival_time)) ASC");

        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql.toString())) {
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
