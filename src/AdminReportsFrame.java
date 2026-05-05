package src;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminReportsFrame extends JFrame {
    private JTable reportTable;
    private DefaultTableModel tableModel;
    private JTextField monthField, yearField, flightSearchField, fNameField, lNameField;

    public AdminReportsFrame() {
        setTitle("Manager Analytics & Reports");
        setSize(1000, 700);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        JPanel searchPanel = new JPanel(new BorderLayout());
        JPanel searchControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        searchControls.add(new JLabel("Month (1-12):"));
        monthField = new JTextField(2); searchControls.add(monthField);
        searchControls.add(new JLabel("Year:"));
        yearField = new JTextField(4); searchControls.add(yearField);
        JButton salesBtn = new JButton("Get Monthly Sales");
        searchControls.add(salesBtn);
        
        searchControls.add(new JSeparator(SwingConstants.VERTICAL));

        searchControls.add(new JLabel("Flight #:"));
        flightSearchField = new JTextField(6); searchControls.add(flightSearchField);
        JButton flightResBtn = new JButton("Find Reservations");
        searchControls.add(flightResBtn);

        searchControls.add(new JSeparator(SwingConstants.VERTICAL));

        searchControls.add(new JLabel("First:"));
        fNameField = new JTextField(6); searchControls.add(fNameField);
        searchControls.add(new JLabel("Last:"));
        lNameField = new JTextField(6); searchControls.add(lNameField);
        JButton nameResBtn = new JButton("Search by Name");
        searchControls.add(nameResBtn);

        searchPanel.add(searchControls, BorderLayout.NORTH);
        tabs.addTab("Sales & Lookups", searchPanel);

        JPanel revPanel = new JPanel(new FlowLayout());
        JButton revAirBtn = new JButton("Revenue by Airline");
        JButton revFltBtn = new JButton("Revenue by Flight");
        JButton revCustBtn = new JButton("Revenue by Customer");
        revPanel.add(revAirBtn); revPanel.add(revFltBtn); revPanel.add(revCustBtn);
        tabs.addTab("Revenue Analytics", revPanel);

        JPanel topPanel = new JPanel(new FlowLayout());
        JButton topCustBtn = new JButton("Find Top Customer");
        JButton topFltBtn = new JButton("Find Top 5 Flights");
        topPanel.add(topCustBtn); topPanel.add(topFltBtn);
        tabs.addTab("Leaderboards", topPanel);

        add(tabs, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        reportTable = new JTable(tableModel);
        add(new JScrollPane(reportTable), BorderLayout.CENTER);

        salesBtn.addActionListener(e -> {
            try {
                int m = Integer.parseInt(monthField.getText());
                int y = Integer.parseInt(yearField.getText());
                updateTable(DatabaseHelper.getMonthlySales(m, y), 
                            new String[]{"Ticket #", "Customer ID", "Total Fare", "Purchase Date"});
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Enter valid Month/Year."); }
        });

        flightResBtn.addActionListener(e -> {
            try {
                updateTable(DatabaseHelper.getReservationsByFlight(flightSearchField.getText().trim()), 
                            new String[]{"Ticket #", "First Name", "Last Name", "Class"});
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        nameResBtn.addActionListener(e -> {
            try {
                updateTable(DatabaseHelper.getReservationsByCustomer(fNameField.getText().trim(), lNameField.getText().trim()), 
                            new String[]{"Ticket #", "Flight #", "Airline", "Total Fare"});
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        revAirBtn.addActionListener(e -> {
            try {
                updateTable(DatabaseHelper.getRevenueByAirline(), new String[]{"Airline ID", "Total Revenue"});
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        revFltBtn.addActionListener(e -> {
            try {
                updateTable(DatabaseHelper.getRevenueByFlight(), new String[]{"Flight #", "Total Revenue"});
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        revCustBtn.addActionListener(e -> {
            try {
                updateTable(DatabaseHelper.getRevenueByCustomer(), new String[]{"First Name", "Last Name", "Total Revenue"});
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        topCustBtn.addActionListener(e -> {
            try {
                updateTable(DatabaseHelper.getTopCustomer(), new String[]{"First Name", "Last Name", "Total Spent"});
            } catch (SQLException ex) { ex.printStackTrace(); }
        });

        topFltBtn.addActionListener(e -> {
            try {
                updateTable(DatabaseHelper.getTopFlights(), new String[]{"Flight #", "Ticket Sales Count"});
            } catch (SQLException ex) { ex.printStackTrace(); }
        });
    }

    private void updateTable(ResultSet rs, String[] columns) throws SQLException {
        tableModel.setColumnIdentifiers(columns);
        tableModel.setRowCount(0);
        int colCount = columns.length;
        while (rs.next()) {
            Object[] row = new Object[colCount];
            for (int i = 0; i < colCount; i++) {
                row[i] = rs.getObject(i + 1);
            }
            tableModel.addRow(row);
        }
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data found for this selection.");
        }
    }
}