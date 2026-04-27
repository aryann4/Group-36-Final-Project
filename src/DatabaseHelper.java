package src;

import java.sql.*;
import java.util.Random;

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

    // 3. Get Customer ID (Needed to link a ticket to a user)
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
     * UPDATED BOOKING ENGINE: Now allows merging/stacking for ALL flight types,
     * including multi-leg journeys and Round-Trips.
     */
    public static boolean bookFlight(int customerId, String flightNums, String airlineId, String seatClass, boolean isFlex, float totalFare, boolean meal, int qtyToAdd) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            int finalTicketNum = -1;

            // 1. STACKING CHECK: Now allowed for multi-leg journeys too
            String findMatchSQL = "SELECT t.ticket_number FROM Ticket t " +
                                  "JOIN Ticket_Segment s ON t.ticket_number = s.ticket_number " +
                                  "WHERE t.customer_id = ? AND t.status = 'active' " +
                                  "AND s.class = ? AND t.is_flexible = ? AND s.special_meal = ? " +
                                  "GROUP BY t.ticket_number " +
                                  "HAVING GROUP_CONCAT(DISTINCT s.flight_number ORDER BY s.sequence_number SEPARATOR ',') = ?";
            
            try (PreparedStatement matchStmt = conn.prepareStatement(findMatchSQL)) {
                matchStmt.setInt(1, customerId);
                matchStmt.setString(2, seatClass);
                matchStmt.setBoolean(3, isFlex);
                matchStmt.setBoolean(4, meal);
                matchStmt.setString(5, flightNums.replace(" ", "")); // Standardize comma separation
                ResultSet rs = matchStmt.executeQuery();
                if (rs.next()) finalTicketNum = rs.getInt("ticket_number");
            }

            if (finalTicketNum != -1) {
                // OPTION A: Match found! Update quantity and fare
                String updateTicketSQL = "UPDATE Ticket SET quantity = quantity + ?, total_fare = total_fare + ? WHERE ticket_number = ?";
                try (PreparedStatement uStmt = conn.prepareStatement(updateTicketSQL)) {
                    uStmt.setInt(1, qtyToAdd);
                    uStmt.setFloat(2, totalFare); 
                    uStmt.setInt(3, finalTicketNum);
                    uStmt.executeUpdate();
                }
            } else {
                // OPTION B: No match. Create new ticket.
                finalTicketNum = (int)(Math.random() * 900000) + 100000;
                String insertTicketSQL = "INSERT INTO Ticket (ticket_number, customer_id, total_fare, purchase_datetime, status, is_flexible, quantity) VALUES (?, ?, ?, NOW(), 'active', ?, ?)";
                try (PreparedStatement iStmt = conn.prepareStatement(insertTicketSQL)) {
                    iStmt.setInt(1, finalTicketNum);
                    iStmt.setInt(2, customerId);
                    iStmt.setFloat(3, totalFare);
                    iStmt.setBoolean(4, isFlex);
                    iStmt.setInt(5, qtyToAdd);
                    iStmt.executeUpdate();
                }
            }

            // 3. Add individual seat segments
            String[] legs = flightNums.split(",");
            int nextSeq = 1;
            
            String seqCheckSQL = "SELECT COALESCE(MAX(sequence_number), 0) FROM Ticket_Segment WHERE ticket_number = ?";
            try (PreparedStatement seqStmt = conn.prepareStatement(seqCheckSQL)) {
                seqStmt.setInt(1, finalTicketNum);
                ResultSet rs = seqStmt.executeQuery();
                if (rs.next()) nextSeq = rs.getInt(1) + 1;
            }

            String segmentSQL = "INSERT INTO Ticket_Segment (ticket_number, sequence_number, flight_number, airline_id, flight_date, class, special_meal, seat_number, from_airport, to_airport) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement sStmt = conn.prepareStatement(segmentSQL)) {
                for (String fNum : legs) {
                    fNum = fNum.trim();
                    String depAir = "", arrAir = "";
                    Date fDate = null;
                    try (PreparedStatement ps = conn.prepareStatement("SELECT departure_airport, arrival_airport, flight_date FROM Flight WHERE flight_number = ?")) {
                        ps.setString(1, fNum);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            depAir = rs.getString(1);
                            arrAir = rs.getString(2);
                            fDate = rs.getDate(3);
                        }
                    }

                    for (int i = 0; i < qtyToAdd; i++) {
                        String seat = (new Random().nextInt(30) + 1) + "" + (char)('A' + new Random().nextInt(6));
                        sStmt.setInt(1, finalTicketNum);
                        sStmt.setInt(2, nextSeq++);
                        sStmt.setString(3, fNum);
                        sStmt.setString(4, airlineId);
                        sStmt.setDate(5, fDate); 
                        sStmt.setString(6, seatClass);
                        sStmt.setBoolean(7, meal);
                        sStmt.setString(8, seat);
                        sStmt.setString(9, depAir);
                        sStmt.setString(10, arrAir);
                        sStmt.addBatch();
                    }
                }
                sStmt.executeBatch();
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
        // This query pulls the first flight_date as dep_date 
        // and joins with the Flight table to find the final arrival_date for arr_date
        String query = "SELECT t.ticket_number, " +
                    "GROUP_CONCAT(DISTINCT s.flight_number ORDER BY s.sequence_number SEPARATOR ', ') AS flight_number, " +
                    "MAX(a.name) AS airline_name, " +
                    "(SELECT s2.flight_date FROM Ticket_Segment s2 WHERE s2.ticket_number = t.ticket_number ORDER BY s2.sequence_number ASC LIMIT 1) AS dep_date, " +
                    "(SELECT f.arrival_date FROM Ticket_Segment s3 JOIN Flight f ON s3.flight_number = f.flight_number WHERE s3.ticket_number = t.ticket_number ORDER BY s3.sequence_number DESC LIMIT 1) AS arr_date, " +
                    "(SELECT from_airport FROM Ticket_Segment s2 WHERE s2.ticket_number = t.ticket_number ORDER BY sequence_number ASC LIMIT 1) AS from_airport, " +
                    "(SELECT to_airport FROM Ticket_Segment s2 WHERE s2.ticket_number = t.ticket_number ORDER BY sequence_number DESC LIMIT 1) AS to_airport, " +
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

    // 5. Check for existing active booking
    public static boolean isAlreadyBooked(int customerId, String flightNum, String airlineId) {
        String sql = "SELECT COUNT(*) FROM Ticket t JOIN Ticket_Segment s ON t.ticket_number = s.ticket_number " +
                    "WHERE t.customer_id = ? AND s.flight_number = ? AND s.airline_id = ? AND t.status = 'active'";
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

    // 6. Cancel Booking (Transactional delete from both Ticket and Segment tables)
    public static boolean cancelBooking(int ticketNum) {
        String deleteSegment = "DELETE FROM Ticket_Segment WHERE ticket_number = ?";
        String deleteTicket = "DELETE FROM Ticket WHERE ticket_number = ?";
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); 
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

    // 7. Register New Customer
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
            stmt.setString(6, dob); 
            stmt.setString(7, user);
            stmt.setString(8, pass);
            return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
    }

    // 8. Fetch Account Creation Date
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
        return "Unknown";
    }

    // 9. Add Customer to Flight Waiting List
    public static void addToWaitingList(int customerId, String flightNum, String airlineId, String seatClass) {
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

    /**
     * 10. Calculate remaining capacity for a specific flight class
     * UPDATED: Now checks every flight leg in a comma-separated list and is NULL-safe
     */
    public static int getSeatsRemaining(String flightNums, String seatClass) {
        String[] legs = flightNums.split(",");
        int minSeats = Integer.MAX_VALUE;

        for (String fNum : legs) {
            fNum = fNum.trim();
            String capCol = seatClass.equalsIgnoreCase("Economy") ? "econ_capacity" : 
                            seatClass.equalsIgnoreCase("Business") ? "bus_capacity" : "first_capacity";
            
            // Using COALESCE to fix negative number issue if capacity is NULL
            String query = "SELECT (COALESCE(a." + capCol + ", 0) - (SELECT COUNT(*) FROM Ticket_Segment s " +
                        "WHERE s.flight_number = f.flight_number AND s.class = ?)) as left_count " +
                        "FROM Flight f JOIN Aircraft a ON f.aircraft_id = a.aircraft_id WHERE f.flight_number = ?";
            
            try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, seatClass);
                stmt.setString(2, fNum);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    int seatsOnThisLeg = rs.getInt("left_count");
                    if (seatsOnThisLeg < minSeats) minSeats = seatsOnThisLeg;
                } else {
                    return 0; // Flight not found
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
        return (minSeats == Integer.MAX_VALUE) ? 0 : minSeats;
    }


    // 11. Check if user is already on waitlist
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

    // 12. Determine dynamic waitlist position
    public static int getWaitlistPosition(int customerId, String flightNum) {
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
        return 1; 
    }
}