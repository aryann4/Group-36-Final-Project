package src;

import java.sql.*;
import java.time.LocalDateTime;

public class DatabaseHelper {
    // Database credentials
    private static final String URL = "jdbc:mysql://localhost:3306/TravelReservationDB";
    private static final String USER = "root";
    private static final String PASSWORD = "group36rootpasswordPIDM";

    // 1. Get a connection to the database
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 2. Validate Login (Essential for LoginFrame)
    public static boolean validateLogin(String username, String password) {
        String query = "SELECT * FROM Customer WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next(); 
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 3. Flexible Search (Works even if boxes are empty)
    public static ResultSet searchFlights(String from, String to) throws SQLException {
        String query = "SELECT f.flight_number, f.airline_id, a.name, f.departure_time, f.arrival_time " +
                       "FROM Flight f JOIN Airline a ON f.airline_id = a.airline_id " +
                       "WHERE (? = '' OR f.departure_airport = ?) " +
                       "AND (? = '' OR f.arrival_airport = ?)";
        
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(query);
        
        stmt.setString(1, from);
        stmt.setString(2, from);
        stmt.setString(3, to);
        stmt.setString(4, to);
        
        return stmt.executeQuery();
    }

    // 4. Get Customer ID (Needed to link a ticket to a user)
    public static int getCustomerId(String username) {
        String query = "SELECT customer_id FROM Customer WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("customer_id");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * SMART BOOKING: Merges identical tickets or creates new ones.
     * Also saves airport routes to prevent "null -> null" errors.
     */
    public static boolean bookFlight(int customerId, String flightNum, String airlineId, String seatClass, boolean isFlex, float additionalFare, boolean meal, int qtyToAdd) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 1. Fetch Airport Codes to ensure "Route" is saved correctly
            String depAir = "", arrAir = "";
            String routeSQL = "SELECT departure_airport, arrival_airport FROM Flight WHERE flight_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(routeSQL)) {
                ps.setString(1, flightNum);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    depAir = rs.getString(1);
                    arrAir = rs.getString(2);
                }
            }

            // 2. Check for an existing ACTIVE ticket with EXACT matching customizations [cite: 137, 138, 141]
            String findMatchSQL = "SELECT t.ticket_number FROM Ticket t " +
                                  "JOIN Ticket_Segment s ON t.ticket_number = s.ticket_number " +
                                  "WHERE t.customer_id = ? AND s.flight_number = ? AND s.airline_id = ? " +
                                  "AND s.class = ? AND t.is_flexible = ? AND s.special_meal = ? " +
                                  "AND t.status = 'active' LIMIT 1";
            
            int existingTicketNum = -1;
            try (PreparedStatement matchStmt = conn.prepareStatement(findMatchSQL)) {
                matchStmt.setInt(1, customerId);
                matchStmt.setString(2, flightNum);
                matchStmt.setString(3, airlineId);
                matchStmt.setString(4, seatClass);
                matchStmt.setBoolean(5, isFlex);
                matchStmt.setBoolean(6, meal);
                ResultSet rs = matchStmt.executeQuery();
                if (rs.next()) existingTicketNum = rs.getInt("ticket_number");
            }

            int finalTicketNum;
            if (existingTicketNum != -1) {
                // OPTION A: Match found. Update quantity and fare. [cite: 137]
                finalTicketNum = existingTicketNum;
                String updateTicketSQL = "UPDATE Ticket SET quantity = quantity + ?, total_fare = total_fare + ? WHERE ticket_number = ?";
                try (PreparedStatement uStmt = conn.prepareStatement(updateTicketSQL)) {
                    uStmt.setInt(1, qtyToAdd);
                    uStmt.setFloat(2, additionalFare);
                    uStmt.setInt(3, finalTicketNum);
                    uStmt.executeUpdate();
                }
            } else {
                // OPTION B: No match. Create new ticket. [cite: 137, 138]
                finalTicketNum = (int)(Math.random() * 900000) + 100000;
                String insertTicketSQL = "INSERT INTO Ticket (ticket_number, customer_id, total_fare, purchase_datetime, status, is_flexible, quantity) VALUES (?, ?, ?, NOW(), 'active', ?, ?)";
                try (PreparedStatement iStmt = conn.prepareStatement(insertTicketSQL)) {
                    iStmt.setInt(1, finalTicketNum);
                    iStmt.setInt(2, customerId);
                    iStmt.setFloat(3, additionalFare);
                    iStmt.setBoolean(4, isFlex);
                    iStmt.setInt(5, qtyToAdd);
                    iStmt.executeUpdate();
                }
            }

            // 3. Add the individual seat segments [cite: 140, 141]
            int startSeq = 1;
            String seqSQL = "SELECT MAX(sequence_number) FROM Ticket_Segment WHERE ticket_number = ?";
            try (PreparedStatement seqStmt = conn.prepareStatement(seqSQL)) {
                seqStmt.setInt(1, finalTicketNum);
                ResultSet rs = seqStmt.executeQuery();
                if (rs.next()) startSeq = rs.getInt(1) + 1;
            }

            String segmentSQL = "INSERT INTO Ticket_Segment (ticket_number, sequence_number, flight_number, airline_id, flight_date, class, special_meal, seat_number, from_airport, to_airport) VALUES (?, ?, ?, ?, CURDATE(), ?, ?, ?, ?, ?)";
            try (PreparedStatement sStmt = conn.prepareStatement(segmentSQL)) {
                for (int i = 0; i < qtyToAdd; i++) {
                    String seat = (new java.util.Random().nextInt(30) + 1) + "" + (char)('A' + new java.util.Random().nextInt(6));
                    sStmt.setInt(1, finalTicketNum);
                    sStmt.setInt(2, startSeq + i);
                    sStmt.setString(3, flightNum);
                    sStmt.setString(4, airlineId);
                    sStmt.setString(5, seatClass);
                    sStmt.setBoolean(6, meal);
                    sStmt.setString(7, seat);
                    sStmt.setString(8, depAir);
                    sStmt.setString(9, arrAir);
                    sStmt.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }


    public static ResultSet getCustomerTickets(int customerId) throws SQLException {
        // This query joins with the Airline table to get the full name [cite: 25, 26]
        // It also selects from_airport and to_airport to fix the "null -> null" issue [cite: 35]
        String query = "SELECT t.ticket_number, MAX(s.flight_number) AS flight_number, " +
                    "MAX(a.name) AS airline_name, MAX(s.flight_date) AS flight_date, " +
                    "MAX(s.from_airport) AS from_airport, MAX(s.to_airport) AS to_airport, " +
                    "MAX(s.class) AS class, t.total_fare, t.status, t.is_flexible, t.quantity " +
                    "FROM Ticket t " +
                    "JOIN Ticket_Segment s ON t.ticket_number = s.ticket_number " +
                    "JOIN Airline a ON s.airline_id = a.airline_id " +
                    "WHERE t.customer_id = ? " +
                    "GROUP BY t.ticket_number";
        
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, customerId);
        return stmt.executeQuery();
    }

    public static boolean isAlreadyBooked(int customerId, String flightNum, String airlineId) {
        String sql = "SELECT COUNT(*) FROM Ticket t " +
                    "JOIN Ticket_Segment s ON t.ticket_number = s.ticket_number " +
                    "WHERE t.customer_id = ? AND s.flight_number = ? " +
                    "AND s.airline_id = ? AND t.status = 'active'";
        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.setString(2, flightNum);
            stmt.setString(3, airlineId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // 2. Cancel Booking (Deletes the record from both Ticket and Segment tables)
    public static boolean cancelBooking(int ticketNum) {
        String deleteSegment = "DELETE FROM Ticket_Segment WHERE ticket_number = ?";
        String deleteTicket = "DELETE FROM Ticket WHERE ticket_number = ?";
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); // Transactional delete
            try (PreparedStatement sStmt = conn.prepareStatement(deleteSegment)) {
                sStmt.setInt(1, ticketNum);
                sStmt.executeUpdate();
            }
            try (PreparedStatement tStmt = conn.prepareStatement(deleteTicket)) {
                tStmt.setInt(1, ticketNum);
                tStmt.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean registerCustomer(String fName, String lName, String email, String phone, String addr, String dob, String user, String pass) {
        String sql = "INSERT INTO Customer (first_name, last_name, email, phone, address, dob, username, password, account_creation_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURDATE())";
        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fName);
            stmt.setString(2, lName);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            stmt.setString(5, addr);
            stmt.setString(6, dob); // Format: YYYY-MM-DD
            stmt.setString(7, user);
            stmt.setString(8, pass);
            return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
    }

    public static String getAccountCreationDate(String username) {
        String query = "SELECT account_creation_date FROM Customer WHERE username = ?";
        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("account_creation_date");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown"; // Fallback if something goes wrong
    }


    public static void addToWaitingList(int customerId, String flightNum, String airlineId, String seatClass) {
        // We changed the SQL to use 'added_date' to match the Java error you got
        String sql = "INSERT INTO Waiting_List (customer_id, flight_number, airline_id, class, added_date) " +
                    "VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.setString(2, flightNum);
            stmt.setString(3, airlineId);
            stmt.setString(4, seatClass);
            stmt.executeUpdate();
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
    }


    // 1. Calculate REAL seats remaining by counting existing tickets
    public static int getSeatsRemaining(String flightNum, String seatClass) {
        // Map class names to your Aircraft table column names
        String capCol = seatClass.equalsIgnoreCase("Economy") ? "econ_capacity" : 
                        seatClass.equalsIgnoreCase("Business") ? "bus_capacity" : "first_capacity";
                        
        String query = "SELECT (a." + capCol + " - (SELECT COUNT(*) FROM Ticket_Segment s " +
                    "WHERE s.flight_number = f.flight_number AND s.class = ?)) as left_count " +
                    "FROM Flight f JOIN Aircraft a ON f.aircraft_id = a.aircraft_id WHERE f.flight_number = ?";
        
        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, seatClass);
            stmt.setString(2, flightNum);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("left_count");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }


    public static boolean isAlreadyOnWaitingList(int customerId, String flightNum) {
        String sql = "SELECT COUNT(*) FROM Waiting_List WHERE customer_id = ? AND flight_number = ?";
        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.setString(2, flightNum);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }


    public static int getWaitlistPosition(int customerId, String flightNum) {
        // This counts how many people are ahead of this customer based on the date they joined
        String sql = "SELECT COUNT(*) + 1 FROM Waiting_List " +
                    "WHERE flight_number = ? AND added_date < " +
                    "(SELECT added_date FROM Waiting_List WHERE customer_id = ? AND flight_number = ?)";
        
        try (Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, flightNum);
            stmt.setInt(2, customerId);
            stmt.setString(3, flightNum);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 1; // Default to 1 if they are the only ones there
    }

}