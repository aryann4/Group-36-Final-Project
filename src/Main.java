package src;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // This is the standard way to launch a Swing window
        SwingUtilities.invokeLater(() -> {
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        });
    }
}