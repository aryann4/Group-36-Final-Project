package src;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QAFrame extends JFrame {
    private JTextField searchField;
    private JTable qaTable;
    private DefaultTableModel tableModel;
    private String username;

    public QAFrame(String username) {
        this.username = username;
        setTitle("Q&A System - Help Desk");
        setSize(800, 500);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // --- Top Panel: Search Bar ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Search FAQs by Keyword:"));
        searchField = new JTextField(25);
        JButton searchBtn = new JButton("Search");
        topPanel.add(searchField);
        topPanel.add(searchBtn);
        add(topPanel, BorderLayout.NORTH);

        // --- Center Panel: Results Table ---
        String[] columns = {"Question", "Answer"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Table is read-only
            }
        };
        qaTable = new JTable(tableModel);
        qaTable.setRowHeight(40); // Taller rows for readability
        add(new JScrollPane(qaTable), BorderLayout.CENTER);

        // --- Bottom Panel: Action Buttons ---
        JPanel bottomPanel = new JPanel();
        JButton postBtn = new JButton("Post a New Question");
        JButton refreshBtn = new JButton("Refresh List");
        JButton closeBtn = new JButton("Close");
        bottomPanel.add(postBtn);
        bottomPanel.add(refreshBtn);
        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- Event Listeners ---

        searchBtn.addActionListener(e -> performSearch());
        refreshBtn.addActionListener(e -> performSearch());

        postBtn.addActionListener(e -> {
            String question = JOptionPane.showInputDialog(this, "Type your question for a representative:");
            if (question != null && !question.trim().isEmpty()) {
                int cId = DatabaseHelper.getCustomerId(username);
                if (DatabaseHelper.postQuestion(cId, question.trim())) {
                    JOptionPane.showMessageDialog(this, "Question posted! Our representatives will answer soon.");
                    performSearch();
                } else {
                    JOptionPane.showMessageDialog(this, "Error posting question. Please try again.");
                }
            }
        });

        closeBtn.addActionListener(e -> dispose());

        // Initial Load
        performSearch();
    }

    private void performSearch() {
        tableModel.setRowCount(0);
        String keyword = searchField.getText().trim();
        
        try (ResultSet rs = DatabaseHelper.getQAEntries(keyword)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getString("question_text"),
                    rs.getString("answer_text")
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error while fetching Q&A.");
        }
    }
}