package src;

import javax.swing.*;
import java.awt.*;

public class BookingOptionsDialog extends JDialog {
    private JComboBox<String> classBox;
    private JCheckBox flexCheckBox, mealCheckBox;
    private JSpinner qtySpinner;
    private JLabel priceLabel, seatsLeftLabel;
    private boolean confirmed = false;
    private String flightNum;
    
    private final float BASE_PRICE = 200.00f;
    private final float BUSINESS_UPCHARGE = 300.00f;
    private final float FIRST_UPCHARGE = 1000.00f;
    private final float FLEX_FEE = 50.00f;
    private final float BOOKING_FEE = 25.00f;

    public BookingOptionsDialog(Frame parent, String flightNum) {
        super(parent, "Booking Customization - " + flightNum, true);
        this.flightNum = flightNum;
        
        // Increased height and set rows to 0 (unlimited) to prevent cutting off
        setSize(450, 550);
        setLayout(new GridLayout(0, 1, 10, 10));
        setLocationRelativeTo(parent);

        // 1. Availability Header
        seatsLeftLabel = new JLabel("Checking availability...", SwingConstants.CENTER);
        seatsLeftLabel.setForeground(Color.BLUE);
        add(seatsLeftLabel);

        // 2. Class Selection
        add(new JLabel("  Select Your Class:"));
        String[] classes = {
            "Economy ($225.00 base)", 
            "Business (+$300.00)", 
            "First (+$1000.00)"
        };
        classBox = new JComboBox<>(classes);
        add(classBox);

        // 3. Quantity Selector
        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        qtyPanel.add(new JLabel("  Quantity of Tickets:"));
        qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        qtyPanel.add(qtySpinner);
        add(qtyPanel);

        // 4. Special Meal
        mealCheckBox = new JCheckBox("  Request Special Meal (No extra charge)");
        add(mealCheckBox);

        // 5. Flexibility
        flexCheckBox = new JCheckBox("  Flexible Ticket (+$50.00 fee)");
        add(flexCheckBox);

        // 6. Pricing Summary
        priceLabel = new JLabel("Total Fare: $0.00", SwingConstants.CENTER);
        priceLabel.setFont(new Font("Arial", Font.BOLD, 18));
        priceLabel.setForeground(new Color(0, 128, 0)); // Dark Green
        add(priceLabel);

        // 7. Action Buttons
        JPanel buttonPanel = new JPanel();
        JButton confirmBtn = new JButton("Confirm & Book");
        JButton cancelBtn = new JButton("Cancel");
        buttonPanel.add(confirmBtn);
        buttonPanel.add(cancelBtn);
        add(buttonPanel);

        // Listeners
        classBox.addActionListener(e -> updateUI());
        flexCheckBox.addActionListener(e -> updateUI());
        qtySpinner.addChangeListener(e -> updateUI());

        updateUI();

        confirmBtn.addActionListener(e -> {
            confirmed = true;
            setVisible(false);
        });

        cancelBtn.addActionListener(e -> {
            confirmed = false;
            setVisible(false);
        });
    }

    private void updateUI() {
        int qty = (int) qtySpinner.getValue();
        String rawClass = (String) classBox.getSelectedItem();
        String cleanClass = rawClass.split(" ")[0]; 
        
        int left = DatabaseHelper.getSeatsRemaining(flightNum, cleanClass);
        seatsLeftLabel.setText("Seats Remaining in " + cleanClass + ": " + left);

        float perTicket = BASE_PRICE + BOOKING_FEE;
        if (cleanClass.equals("Business")) perTicket += BUSINESS_UPCHARGE;
        if (cleanClass.equals("First")) perTicket += FIRST_UPCHARGE;
        if (flexCheckBox.isSelected()) perTicket += FLEX_FEE;
        
        priceLabel.setText(String.format("Total for %d Ticket(s): $%.2f", qty, perTicket * qty));
    }

    public boolean isConfirmed() { return confirmed; }
    public String getSelectedClass() { return ((String) classBox.getSelectedItem()).split(" ")[0]; }
    public boolean isFlexible() { return flexCheckBox.isSelected(); }
    public boolean hasSpecialMeal() { return mealCheckBox.isSelected(); }
    public int getQuantity() { return (int) qtySpinner.getValue(); }
    public float getTotalFare() {
        // Recalculate for final DB entry
        float perTicket = BASE_PRICE + BOOKING_FEE;
        String cleanClass = getSelectedClass();
        if (cleanClass.equals("Business")) perTicket += BUSINESS_UPCHARGE;
        if (cleanClass.equals("First")) perTicket += FIRST_UPCHARGE;
        if (isFlexible()) perTicket += FLEX_FEE;
        return perTicket * (int) qtySpinner.getValue();
    }
}