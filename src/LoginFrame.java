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

        // Add this near your loginButton
        JButton signUpButton = new JButton("Sign Up");
        signUpButton.setBounds(210, 110, 80, 25); // Adjusted position to be next to Login
        add(signUpButton);

        // Add this listener at the bottom of the constructor
        signUpButton.addActionListener(e -> {
            new RegistrationFrame().setVisible(true);
            dispose();
        });


        // This is the logic that runs when you click the button
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String user = userField.getText();
                String pass = new String(passField.getPassword());
                
                // We call the method you just wrote in DatabaseHelper!
                if (DatabaseHelper.validateLogin(user, pass)) {
                    JOptionPane.showMessageDialog(null, "Login Successful!");
                    // Later, we will add code here to open the main search menu
                    new CustomerDashboard(user).setVisible(true);
                    LoginFrame.this.dispose(); // This closes the login window
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid Username or Password.");
                }
            }
        });
    }
}