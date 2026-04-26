package src;
import javax.swing.*;
import java.awt.*;

public class RegistrationFrame extends JFrame {
    private JTextField fNameField, lNameField, emailField, phoneField, addrField, dobField, userField;
    private JPasswordField passField;

    public RegistrationFrame() {
        setTitle("Create New Account");
        setSize(400, 500);
        setLayout(new GridLayout(10, 2, 10, 10));
        setLocationRelativeTo(null);

        // Form Fields
        add(new JLabel(" First Name:"));
        fNameField = new JTextField(); add(fNameField);

        add(new JLabel(" Last Name:"));
        lNameField = new JTextField(); add(lNameField);

        add(new JLabel(" Email:"));
        emailField = new JTextField(); add(emailField);

        add(new JLabel(" Phone:"));
        phoneField = new JTextField(); add(phoneField);

        add(new JLabel(" Address:"));
        addrField = new JTextField(); add(addrField);

        add(new JLabel(" DOB (YYYY-MM-DD):"));
        dobField = new JTextField(); add(dobField);

        add(new JLabel(" Username:"));
        userField = new JTextField(); add(userField);

        add(new JLabel(" Password:"));
        passField = new JPasswordField(); add(passField);

        JButton registerBtn = new JButton("Register");
        JButton backBtn = new JButton("Back to Login");

        add(registerBtn);
        add(backBtn);

        
        // Logic for the Register Button
        registerBtn.addActionListener(e -> {
            // 1. Gatekeeper Check: Ensure no fields are empty
            if (fNameField.getText().trim().isEmpty() || 
                lNameField.getText().trim().isEmpty() || 
                emailField.getText().trim().isEmpty() || 
                phoneField.getText().trim().isEmpty() || 
                addrField.getText().trim().isEmpty() || 
                dobField.getText().trim().isEmpty() || 
                userField.getText().trim().isEmpty() || 
                new String(passField.getPassword()).isEmpty()) {
                
                JOptionPane.showMessageDialog(this, "All fields are mandatory! Please fill in every box.");
                return; // Stops the code from reaching the database part below
            }

            // 2. If we passed the check, proceed to registration
            boolean success = DatabaseHelper.registerCustomer(
                fNameField.getText(), lNameField.getText(), emailField.getText(),
                phoneField.getText(), addrField.getText(), dobField.getText(),
                userField.getText(), new String(passField.getPassword())
            );

            if (success) {
                JOptionPane.showMessageDialog(this, "Account Created! You can now log in.");
                new LoginFrame().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Registration failed. Check your data format (DOB: YYYY-MM-DD).");
            }
        });
    }
}