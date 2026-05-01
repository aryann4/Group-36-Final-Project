package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class RepManagementFrame extends JFrame {
    private JTable aircraftTable, airportTable, flightTable;
    private DefaultTableModel acModel, apModel, fModel;

    public RepManagementFrame() {
        setTitle("Infrastructure Management - Aircraft, Airports, & Flights");
        setSize(1100, 700);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        // --- TAB 1: AIRCRAFT MANAGEMENT ---
        JPanel acPanel = new JPanel(new BorderLayout());
        String[] acCols = {"ID", "Model", "Manufacturer", "Total Seats", "Econ", "Bus", "First", "Airline"};
        acModel = new DefaultTableModel(acCols, 0);
        aircraftTable = new JTable(acModel);
        acPanel.add(new JScrollPane(aircraftTable), BorderLayout.CENTER);

        JPanel acButtons = new JPanel();
        JButton addAcBtn = new JButton("Add Aircraft");
        JButton editAcBtn = new JButton("Edit Selected");
        JButton delAcBtn = new JButton("Delete Selected");
        acButtons.add(addAcBtn); acButtons.add(editAcBtn); acButtons.add(delAcBtn);
        acPanel.add(acButtons, BorderLayout.SOUTH);
        tabs.addTab("Aircraft", acPanel);

        // --- TAB 2: AIRPORT MANAGEMENT ---
        JPanel apPanel = new JPanel(new BorderLayout());
        String[] apCols = {"Code", "Name", "City", "Country"};
        apModel = new DefaultTableModel(apCols, 0);
        airportTable = new JTable(apModel);
        apPanel.add(new JScrollPane(airportTable), BorderLayout.CENTER);

        JPanel apButtons = new JPanel();
        JButton addApBtn = new JButton("Add Airport");
        JButton delApBtn = new JButton("Delete Selected");
        apButtons.add(addApBtn); apButtons.add(delApBtn);
        apPanel.add(apButtons, BorderLayout.SOUTH);
        tabs.addTab("Airports", apPanel);

        // --- TAB 3: FLIGHT MANAGEMENT ---
        JPanel fPanel = new JPanel(new BorderLayout());
        String[] fCols = {"Flight #", "Airline", "AC ID", "From", "To", "Dep Date", "Arr Date", "Dep Time", "Arr Time", "Type", "Price"};
        fModel = new DefaultTableModel(fCols, 0);
        flightTable = new JTable(fModel);
        fPanel.add(new JScrollPane(flightTable), BorderLayout.CENTER);

        JPanel fButtons = new JPanel();
        JButton addFBtn = new JButton("Add Flight");
        JButton delFBtn = new JButton("Delete Selected");
        fButtons.add(addFBtn); fButtons.add(delFBtn);
        fPanel.add(fButtons, BorderLayout.SOUTH);
        tabs.addTab("Flights", fPanel);

        add(tabs, BorderLayout.CENTER);

        // Initial Data Load
        refreshAircraft();
        refreshAirports();
        refreshFlights();

        // --- LISTENERS ---

        // Aircraft Actions
        addAcBtn.addActionListener(e -> {
            try {
                String idStr = JOptionPane.showInputDialog("Enter Aircraft ID:");
                if (idStr == null) return;
                int id = Integer.parseInt(idStr);
                
                String model = JOptionPane.showInputDialog("Model:");
                String man = JOptionPane.showInputDialog("Manufacturer:");
                int total = Integer.parseInt(JOptionPane.showInputDialog("Total Seats:"));
                int econ = Integer.parseInt(JOptionPane.showInputDialog("Economy Seats:"));
                int bus = Integer.parseInt(JOptionPane.showInputDialog("Business Seats:"));
                int first = Integer.parseInt(JOptionPane.showInputDialog("First Class Seats:"));
                String airId = JOptionPane.showInputDialog("Airline ID (UA/AA):");

                if (DatabaseHelper.addAircraft(id, model, man, total, econ, bus, first, airId)) {
                    JOptionPane.showMessageDialog(this, "Aircraft added successfully!");
                    refreshAircraft();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to add aircraft. Check if ID already exists or Airline ID is valid.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: Please enter numbers for IDs and capacities.");
            } catch (Exception ex) { 
                JOptionPane.showMessageDialog(this, "An unexpected error occurred.");
                ex.printStackTrace(); 
            }
        });

        // FIXED: Edit logic now retrieves existing data and pushes updates
        editAcBtn.addActionListener(e -> {
            int row = aircraftTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select an aircraft to edit.");
                return;
            }

            try {
                int id = (int) acModel.getValueAt(row, 0); // Primary key is locked
                String model = JOptionPane.showInputDialog("Update Model:", acModel.getValueAt(row, 1));
                String man = JOptionPane.showInputDialog("Update Manufacturer:", acModel.getValueAt(row, 2));
                int total = Integer.parseInt(JOptionPane.showInputDialog("Update Total Seats:", acModel.getValueAt(row, 3)));
                int econ = Integer.parseInt(JOptionPane.showInputDialog("Update Economy Seats:", acModel.getValueAt(row, 4)));
                int bus = Integer.parseInt(JOptionPane.showInputDialog("Update Business Seats:", acModel.getValueAt(row, 5)));
                int first = Integer.parseInt(JOptionPane.showInputDialog("Update First Class Seats:", acModel.getValueAt(row, 6)));
                String airId = JOptionPane.showInputDialog("Update Airline ID (UA/AA):", acModel.getValueAt(row, 7));

                if (DatabaseHelper.updateAircraft(id, model, man, total, econ, bus, first, airId)) {
                    JOptionPane.showMessageDialog(this, "Aircraft updated successfully!");
                    refreshAircraft();
                } else {
                    JOptionPane.showMessageDialog(this, "Update failed.");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error processing update. Ensure all number fields are correct.");
            }
        });

        delAcBtn.addActionListener(e -> {
            int row = aircraftTable.getSelectedRow();
            if (row == -1) return;
            int id = (int) acModel.getValueAt(row, 0);
            
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete Aircraft ID: " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (DatabaseHelper.deleteAircraft(id)) {
                    refreshAircraft();
                } else {
                    JOptionPane.showMessageDialog(this, "Cannot delete aircraft. It may be assigned to an existing flight.");
                }
            }
        });

        // Airport Actions
        addApBtn.addActionListener(e -> {
            String code = JOptionPane.showInputDialog("Airport Code (3 chars):");
            if (code == null) return;
            String name = JOptionPane.showInputDialog("Airport Name:");
            String city = JOptionPane.showInputDialog("City:");
            String country = JOptionPane.showInputDialog("Country:");
            if (DatabaseHelper.addAirport(code, name, city, country)) refreshAirports();
        });

        delApBtn.addActionListener(e -> {
            int row = airportTable.getSelectedRow();
            if (row == -1) return;
            String code = (String) apModel.getValueAt(row, 0);
            if (DatabaseHelper.deleteAirport(code)) refreshAirports();
        });

        // Flight Actions
        addFBtn.addActionListener(e -> {
            try {
                String fNum = JOptionPane.showInputDialog("Flight Number:");
                if (fNum == null) return;
                String aId = JOptionPane.showInputDialog("Airline ID:");
                int acId = Integer.parseInt(JOptionPane.showInputDialog("Aircraft ID:"));
                String dep = JOptionPane.showInputDialog("Dep Airport:");
                String arr = JOptionPane.showInputDialog("Arr Airport:");
                String dDate = JOptionPane.showInputDialog("Dep Date (YYYY-MM-DD):");
                String aDate = JOptionPane.showInputDialog("Arr Date (YYYY-MM-DD):");
                String dTime = JOptionPane.showInputDialog("Dep Time (HH:MM:SS):");
                String aTime = JOptionPane.showInputDialog("Arr Time (HH:MM:SS):");
                String type = JOptionPane.showInputDialog("Type (domestic/international):");
                float price = Float.parseFloat(JOptionPane.showInputDialog("Base Price:"));

                if (DatabaseHelper.addFlight(fNum, aId, acId, dep, arr, dDate, aDate, dTime, aTime, type, price)) {
                    refreshFlights();
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: Check your format (Dates: YYYY-MM-DD, Times: HH:MM:SS)."); }
        });

        delFBtn.addActionListener(e -> {
            int row = flightTable.getSelectedRow();
            if (row == -1) return;
            String fNum = (String) fModel.getValueAt(row, 0);
            String aId = (String) fModel.getValueAt(row, 1);
            if (DatabaseHelper.deleteFlight(fNum, aId)) refreshFlights();
        });
    }

    private void refreshAircraft() {
        acModel.setRowCount(0);
        try (ResultSet rs = DatabaseHelper.getAllAircraft()) {
            while (rs.next()) {
                acModel.addRow(new Object[]{
                    rs.getInt("aircraft_id"), rs.getString("model"), rs.getString("manufacturer"),
                    rs.getInt("total_seats"), rs.getInt("econ_capacity"), rs.getInt("bus_capacity"),
                    rs.getInt("first_capacity"), rs.getString("airline_id")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void refreshAirports() {
        apModel.setRowCount(0);
        try (ResultSet rs = DatabaseHelper.getAllAirports()) {
            while (rs.next()) {
                apModel.addRow(new Object[]{
                    rs.getString("airport_code"), rs.getString("name"), 
                    rs.getString("city"), rs.getString("country")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void refreshFlights() {
        fModel.setRowCount(0);
        try (ResultSet rs = DatabaseHelper.getAllFlightsRaw()) {
            while (rs.next()) {
                fModel.addRow(new Object[]{
                    rs.getString("flight_number"), rs.getString("airline_id"), rs.getInt("aircraft_id"),
                    rs.getString("departure_airport"), rs.getString("arrival_airport"),
                    rs.getDate("flight_date"), rs.getDate("arrival_date"),
                    rs.getTime("departure_time"), rs.getTime("arrival_time"),
                    rs.getString("flight_type"), rs.getFloat("base_price")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
}