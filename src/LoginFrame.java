package src;
import javax.swing.*;
import java.awt.event.*;

public class LoginFrame extends JFrame {
    private JTextField userField;
    private JPasswordField passField;
    private JButton loginButton;

    public LoginFrame() {
        // Basic window setup
        setTitle("Travel System Login");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);
        setLocationRelativeTo(null); // Centers the window on your screen

        // Username Label and Textbox
        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(30, 30, 80, 25);
        add(userLabel);

        userField = new JTextField();
        userField.setBounds(120, 30, 160, 25);
        add(userField);

        // Password Label and Textbox
        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(30, 70, 80, 25);
        add(passLabel);

        passField = new JPasswordField();
        passField.setBounds(120, 70, 160, 25);
        add(passField);

        // Login Button
        loginButton = new JButton("Login");
        loginButton.setBounds(120, 110, 80, 25);
        add(loginButton);

        // Sign Up Button
        JButton signUpButton = new JButton("Sign Up");
        signUpButton.setBounds(210, 110, 80, 25); 
        add(signUpButton);

        signUpButton.addActionListener(e -> {
            new RegistrationFrame().setVisible(true);
            dispose();
        });

        // Updated Login Logic for Multiple Roles
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String user = userField.getText();
                String pass = new String(passField.getPassword());
                
                // 1. Check if the user is a Customer
                if (DatabaseHelper.validateLogin(user, pass)) {
                    JOptionPane.showMessageDialog(null, "Customer Login Successful!");
                    new CustomerDashboard(user).setVisible(true);
                    LoginFrame.this.dispose(); 
                } 
                // 2. Check if the user is an Employee
                else if (DatabaseHelper.validateEmployeeLogin(user, pass)) {
                    String role = DatabaseHelper.getEmployeeRole(user);
                    JOptionPane.showMessageDialog(null, "Employee Login Successful! Role: " + role);
                    
                    if ("admin".equalsIgnoreCase(role)) {
                        // This will open the Admin Dashboard once created
                        new AdminDashboard(user).setVisible(true);
                    } else if ("representative".equalsIgnoreCase(role)) {
                        // ACTIVATED: Launches the Representative Dashboard instead of just a popup
                        new RepresentativeDashboard(user).setVisible(true);
                    }
                    LoginFrame.this.dispose(); 
                } 
                // 3. Invalid credentials
                else {
                    JOptionPane.showMessageDialog(null, "Invalid Username or Password.");
                }
            }
        });
    }
}