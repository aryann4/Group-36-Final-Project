package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class RepSupportFrame extends JFrame {
    private JTable questionTable, waitlistTable, trafficTable;
    private DefaultTableModel qModel, wModel, tModel;
    private JTextField flightSearchField, airportSearchField;

    public RepSupportFrame() {
        setTitle("Support Management - Q&A, Waitlists, & Traffic");
        setSize(1100, 650); 
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        // --- TAB 1: ANSWER QUESTIONS ---
        JPanel qPanel = new JPanel(new BorderLayout());
        qModel = new DefaultTableModel(new String[]{"ID", "User", "Question"}, 0);
        questionTable = new JTable(qModel);
        qPanel.add(new JScrollPane(questionTable), BorderLayout.CENTER);

        JButton answerBtn = new JButton("Answer Selected Question");
        qPanel.add(answerBtn, BorderLayout.SOUTH);
        tabs.addTab("Pending Questions", qPanel);

        // --- TAB 2: FLIGHT WAITLISTS ---
        JPanel wPanel = new JPanel(new BorderLayout());
        JPanel topW = new JPanel();
        topW.add(new JLabel("Enter Flight #:"));
        flightSearchField = new JTextField(10);
        topW.add(flightSearchField);
        JButton searchWBtn = new JButton("View Waitlist");
        topW.add(searchWBtn);
        wPanel.add(topW, BorderLayout.NORTH);

        wModel = new DefaultTableModel(new String[]{"First Name", "Last Name", "Class", "Joined Date"}, 0);
        waitlistTable = new JTable(wModel);
        wPanel.add(new JScrollPane(waitlistTable), BorderLayout.CENTER);
        tabs.addTab("Flight Waitlists", wPanel);

        // --- TAB 3: AIRPORT TRAFFIC ---
        JPanel tPanel = new JPanel(new BorderLayout());
        JPanel topT = new JPanel();
        topT.add(new JLabel("Airport Code (e.g. EWR):"));
        airportSearchField = new JTextField(5);
        topT.add(airportSearchField);
        JButton searchTBtn = new JButton("Generate Traffic Report");
        topT.add(searchTBtn);
        tPanel.add(topT, BorderLayout.NORTH);

        // UPDATED: More detailed column headers for date and time 
        String[] tCols = {"Flight #", "Airline", "From", "To", "Departure Date", "Arrival Date", "Departure Time", "Arrival Time"};
        tModel = new DefaultTableModel(tCols, 0);
        trafficTable = new JTable(tModel);
        tPanel.add(new JScrollPane(trafficTable), BorderLayout.CENTER);
        tabs.addTab("Airport Traffic", tPanel);

        add(tabs, BorderLayout.CENTER);

        // Listeners
        refreshQuestions();

        answerBtn.addActionListener(e -> {
            int row = questionTable.getSelectedRow();
            if (row == -1) return;
            int qaId = (int) qModel.getValueAt(row, 0);
            String ans = JOptionPane.showInputDialog(this, "Enter Response:");
            if (ans != null && !ans.trim().isEmpty()) {
                if (DatabaseHelper.answerQuestion(qaId, ans)) {
                    JOptionPane.showMessageDialog(this, "Response Sent!");
                    refreshQuestions();
                }
            }
        });

        searchWBtn.addActionListener(e -> {
            String fNum = flightSearchField.getText().trim();
            if (fNum.isEmpty()) return;
            refreshWaitlist(fNum);
        });

        searchTBtn.addActionListener(e -> {
            String code = airportSearchField.getText().trim().toUpperCase();
            if (code.isEmpty()) return;
            refreshTraffic(code);
        });
    }

    private void refreshQuestions() {
        qModel.setRowCount(0);
        try (ResultSet rs = DatabaseHelper.getPendingQuestions()) {
            while (rs.next()) {
                qModel.addRow(new Object[]{rs.getInt("qa_id"), rs.getString("author"), rs.getString("question_text")});
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void refreshWaitlist(String flightNum) {
        wModel.setRowCount(0);
        try (ResultSet rs = DatabaseHelper.getFlightWaitlist(flightNum)) {
            while (rs.next()) {
                wModel.addRow(new Object[]{
                    rs.getString("first_name"), 
                    rs.getString("last_name"), 
                    rs.getString("class"), 
                    rs.getTimestamp("added_date")
                });
            }
            if (wModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No passengers found on the waitlist for flight " + flightNum);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void refreshTraffic(String airportCode) {
        tModel.setRowCount(0);
        try (ResultSet rs = DatabaseHelper.getAirportTraffic(airportCode)) {
            while (rs.next()) {
                // UPDATED: Added arrival_date and renamed time pulls 
                tModel.addRow(new Object[]{
                    rs.getString("flight_number"),
                    rs.getString("airline_id"),
                    rs.getString("departure_airport"),
                    rs.getString("arrival_airport"),
                    rs.getDate("flight_date"),
                    rs.getDate("arrival_date"),
                    rs.getTime("departure_time"),
                    rs.getTime("arrival_time")
                });
            }
            if (tModel.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No flights found for airport: " + airportCode);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
}