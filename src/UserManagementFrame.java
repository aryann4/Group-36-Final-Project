package src;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UserManagementFrame extends JFrame {
    private JTable customerTable, employeeTable;
    private DefaultTableModel custModel, empModel;

    public UserManagementFrame() {
        setTitle("Admin Portal - User Management");
        setSize(1000, 600);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();

        JPanel custPanel = new JPanel(new BorderLayout());
        String[] custCols = {"ID", "First", "Last", "Email", "Phone", "Username", "Password", "Address"};
        custModel = new DefaultTableModel(custCols, 0);
        customerTable = new JTable(custModel);
        custPanel.add(new JScrollPane(customerTable), BorderLayout.CENTER);

        JPanel custBtns = new JPanel();
        JButton addCustBtn  = new JButton("Add New Customer");
        JButton editCustBtn = new JButton("Edit Selected Customer");
        JButton delCustBtn  = new JButton("Delete Selected Customer");
        custBtns.add(addCustBtn); custBtns.add(editCustBtn); custBtns.add(delCustBtn);
        custPanel.add(custBtns, BorderLayout.SOUTH);
        tabs.addTab("Customers", custPanel);

        JPanel empPanel = new JPanel(new BorderLayout());
        String[] empCols = {"ID", "First", "Last", "Email", "Phone", "Username", "Password", "Role"};
        empModel = new DefaultTableModel(empCols, 0);
        employeeTable = new JTable(empModel);
        empPanel.add(new JScrollPane(employeeTable), BorderLayout.CENTER);

        JPanel empBtns = new JPanel();
        JButton addEmpBtn = new JButton("Add New Employee");
        JButton editEmpBtn = new JButton("Edit Selected Employee");
        JButton delEmpBtn = new JButton("Delete Selected Employee");
        empBtns.add(addEmpBtn); empBtns.add(editEmpBtn); empBtns.add(delEmpBtn);
        empPanel.add(empBtns, BorderLayout.SOUTH);
        tabs.addTab("Employees", empPanel);

        add(tabs, BorderLayout.CENTER);

        refreshCustomers();
        refreshEmployees();

        addCustBtn.addActionListener(e -> {
            try {
                String f    = JOptionPane.showInputDialog("First Name:");
                if (f == null) return;
                String l    = JOptionPane.showInputDialog("Last Name:");
                String em   = JOptionPane.showInputDialog("Email:");
                String ph   = JOptionPane.showInputDialog("Phone:");
                String addr = JOptionPane.showInputDialog("Address:");
                String dob  = JOptionPane.showInputDialog("Date of Birth (YYYY-MM-DD):");
                String user = JOptionPane.showInputDialog("Username:");
                String pass = JOptionPane.showInputDialog("Password:");

                if (DatabaseHelper.registerCustomer(f, l, em, ph, addr, dob, user, pass)) {
                    JOptionPane.showMessageDialog(this, "Customer account created successfully!");
                    refreshCustomers();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to create account. Check for duplicate username or date format (YYYY-MM-DD).");
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
        });

        editCustBtn.addActionListener(e -> {
            int row = customerTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a customer to edit.");
                return;
            }
            try {
                int id = (int) custModel.getValueAt(row, 0);
                String f = JOptionPane.showInputDialog("First Name:", custModel.getValueAt(row, 1));
                String l = JOptionPane.showInputDialog("Last Name:", custModel.getValueAt(row, 2));
                String em = JOptionPane.showInputDialog("Email:", custModel.getValueAt(row, 3));
                String ph = JOptionPane.showInputDialog("Phone:", custModel.getValueAt(row, 4));
                String user = JOptionPane.showInputDialog("Username:", custModel.getValueAt(row, 5));
                String pass = JOptionPane.showInputDialog("Password:", custModel.getValueAt(row, 6));
                String addr = JOptionPane.showInputDialog("Address:", custModel.getValueAt(row, 7));

                if (DatabaseHelper.updateCustomer(id, f, l, em, ph, addr, user, pass)) {
                    JOptionPane.showMessageDialog(this, "Customer updated!");
                    refreshCustomers();
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error updating customer."); }
        });

        editEmpBtn.addActionListener(e -> {
            int row = employeeTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select an employee to edit.");
                return;
            }
            try {
                int id = (int) empModel.getValueAt(row, 0);
                String f = JOptionPane.showInputDialog("First Name:", empModel.getValueAt(row, 1));
                String l = JOptionPane.showInputDialog("Last Name:", empModel.getValueAt(row, 2));
                String em = JOptionPane.showInputDialog("Email:", empModel.getValueAt(row, 3));
                String ph = JOptionPane.showInputDialog("Phone:", empModel.getValueAt(row, 4));
                String user = JOptionPane.showInputDialog("Username:", empModel.getValueAt(row, 5));
                String pass = JOptionPane.showInputDialog("Password:", empModel.getValueAt(row, 6));
                String role = JOptionPane.showInputDialog("Role (admin/representative):", empModel.getValueAt(row, 7));

                if (DatabaseHelper.updateEmployee(id, f, l, em, ph, user, pass, role)) {
                    JOptionPane.showMessageDialog(this, "Employee updated.");
                    refreshEmployees();
                }
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error updating employee."); }
        });

        addEmpBtn.addActionListener(e -> {
            try {
                int id = Integer.parseInt(JOptionPane.showInputDialog("New Employee ID:"));
                String f = JOptionPane.showInputDialog("First Name:");
                String l = JOptionPane.showInputDialog("Last Name:");
                String em = JOptionPane.showInputDialog("Email:");
                String ph = JOptionPane.showInputDialog("Phone:");
                String user = JOptionPane.showInputDialog("Username:");
                String pass = JOptionPane.showInputDialog("Password:");
                String role = JOptionPane.showInputDialog("Role (admin/representative):");

                if (DatabaseHelper.addEmployee(id, f, l, em, ph, user, pass, role)) refreshEmployees();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error adding employee."); }
        });

        delCustBtn.addActionListener(e -> {
            int row = customerTable.getSelectedRow();
            if (row == -1) return;
            int id = (int) custModel.getValueAt(row, 0);
            if (DatabaseHelper.deleteCustomer(id)) refreshCustomers();
        });

        delEmpBtn.addActionListener(e -> {
            int row = employeeTable.getSelectedRow();
            if (row == -1) return;
            int id = (int) empModel.getValueAt(row, 0);
            if (DatabaseHelper.deleteEmployee(id)) refreshEmployees();
        });
    }

    private void refreshCustomers() {
        custModel.setRowCount(0);
        try (ResultSet rs = DatabaseHelper.getAllCustomers()) {
            while (rs.next()) {
                custModel.addRow(new Object[]{
                    rs.getInt("customer_id"), rs.getString("first_name"), rs.getString("last_name"),
                    rs.getString("email"), rs.getString("phone"), rs.getString("username"), 
                    rs.getString("password"), rs.getString("address")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void refreshEmployees() {
        empModel.setRowCount(0);
        try (ResultSet rs = DatabaseHelper.getAllEmployees()) {
            while (rs.next()) {
                empModel.addRow(new Object[]{
                    rs.getInt("employee_id"), rs.getString("first_name"), rs.getString("last_name"),
                    rs.getString("email"), rs.getString("phone"), rs.getString("username"), 
                    rs.getString("password"), rs.getString("role")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
}
