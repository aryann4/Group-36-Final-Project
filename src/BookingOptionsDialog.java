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
    
    private float flightBasePrice;
    private String route, depDetails, arrDetails;
    
    private final float BUSINESS_UPCHARGE = 300.00f;
    private final float FIRST_UPCHARGE = 1000.00f;
    private final float FLEX_FEE = 50.00f;
    private final float BOOKING_FEE = 25.00f;

    // Updated constructor to accept 6 parameters to match the call from SearchFrame
    public BookingOptionsDialog(Frame parent, String flightNum, String route, String depDetails, String arrDetails, float bPrice) {
        super(parent, "Booking Customization - " + flightNum, true);
        this.flightNum = flightNum;
        this.route = route;
        this.depDetails = depDetails;
        this.arrDetails = arrDetails;
        this.flightBasePrice = bPrice;
        
        setSize(500, 700); // Increased height to fit new labels
        setLayout(new GridLayout(0, 1, 10, 10));
        setLocationRelativeTo(parent);

        // --- Header info to affirm selection ---
        JPanel header = new JPanel(new GridLayout(3, 1));
        JLabel routeLbl = new JLabel("Route: " + route, SwingConstants.CENTER);
        JLabel depLbl = new JLabel("Departure: " + depDetails, SwingConstants.CENTER);
        JLabel arrLbl = new JLabel("Arrival: " + arrDetails, SwingConstants.CENTER);
        routeLbl.setFont(new Font("Arial", Font.BOLD, 14));
        header.add(routeLbl);
        header.add(depLbl);
        header.add(arrLbl);
        add(header);

        // 1. Availability Header
        seatsLeftLabel = new JLabel("Checking availability...", SwingConstants.CENTER);
        seatsLeftLabel.setForeground(Color.BLUE);
        add(seatsLeftLabel);

        // 2. Class Selection
        add(new JLabel("  Select Your Class:"));
        String[] classes = {
            "Economy (Base: $" + flightBasePrice + ")", 
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

        // --- New Disclaimer Label ---
        JLabel feeLabel = new JLabel("* A non-refundable $25.00 booking fee is included in the total.", SwingConstants.CENTER);
        feeLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        add(feeLabel);

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

        float perTicket = flightBasePrice + BOOKING_FEE;
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
        float perTicket = flightBasePrice + BOOKING_FEE;
        String cleanClass = getSelectedClass();
        if (cleanClass.equals("Business")) perTicket += BUSINESS_UPCHARGE;
        if (cleanClass.equals("First")) perTicket += FIRST_UPCHARGE;
        if (isFlexible()) perTicket += FLEX_FEE;
        return perTicket * (int) qtySpinner.getValue();
    }
}