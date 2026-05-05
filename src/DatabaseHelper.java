package src;

import java.sql.*;
import java.util.Random;

public class DatabaseHelper {
    private static final String URL = "jdbc:mysql://localhost:3306/TravelReservationDB";
    private static final String USER = "root";
    private static final String PASSWORD = "group36rootpasswordPIDM";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static boolean validateLogin(String username, String password) {
        String query = "SELECT * FROM Customer WHERE username = ? AND password = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username); stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static int getCustomerId(String username) {
        String query = "SELECT customer_id FROM Customer WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("customer_id");
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    // ----------------------------------------------------------------
    // bookFlight — ticketType = "one-way" | "round-trip"
    // ----------------------------------------------------------------
    public static boolean bookFlight(int customerId, String flightNums, String airlineId,
            String seatClass, boolean isFlex, float totalFare, boolean meal,
            int qtyToAdd, String ticketType) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            int finalTicketNum = -1;
            String findMatchSQL = "SELECT t.ticket_number FROM Ticket t " +
                "JOIN Ticket_Segment s ON t.ticket_number = s.ticket_number " +
                "WHERE t.customer_id = ? AND t.status = 'active' " +
                "AND s.class = ? AND t.is_flexible = ? AND s.special_meal = ? " +
                "GROUP BY t.ticket_number " +
                "HAVING GROUP_CONCAT(DISTINCT s.flight_number ORDER BY s.sequence_number SEPARATOR ',') = ?";
            try (PreparedStatement matchStmt = conn.prepareStatement(findMatchSQL)) {
                matchStmt.setInt(1, customerId); matchStmt.setString(2, seatClass);
                matchStmt.setBoolean(3, isFlex); matchStmt.setBoolean(4, meal);
                matchStmt.setString(5, flightNums.replace(" ", ""));
                ResultSet rs = matchStmt.executeQuery();
                if (rs.next()) finalTicketNum = rs.getInt("ticket_number");
            }

            if (finalTicketNum != -1) {
                String updateTicketSQL = "UPDATE Ticket SET quantity = quantity + ?, total_fare = total_fare + ? WHERE ticket_number = ?";
                try (PreparedStatement uStmt = conn.prepareStatement(updateTicketSQL)) {
                    uStmt.setInt(1, qtyToAdd); uStmt.setFloat(2, totalFare); uStmt.setInt(3, finalTicketNum);
                    uStmt.executeUpdate();
                }
            } else {
                finalTicketNum = (int)(Math.random() * 900000) + 100000;
                String insertTicketSQL = "INSERT INTO Ticket (ticket_number, customer_id, total_fare, " +
                    "purchase_datetime, status, is_flexible, quantity, booking_fee, ticket_type) " +
                    "VALUES (?, ?, ?, NOW(), 'active', ?, ?, 25.00, ?)";
                try (PreparedStatement iStmt = conn.prepareStatement(insertTicketSQL)) {
                    iStmt.setInt(1, finalTicketNum); iStmt.setInt(2, customerId);
                    iStmt.setFloat(3, totalFare); iStmt.setBoolean(4, isFlex);
                    iStmt.setInt(5, qtyToAdd); iStmt.setString(6, ticketType);
                    iStmt.executeUpdate();
                }
            }

            String[] legs = flightNums.split(",");
            int nextSeq = 1;
            String seqCheckSQL = "SELECT COALESCE(MAX(sequence_number), 0) FROM Ticket_Segment WHERE ticket_number = ?";
            try (PreparedStatement seqStmt = conn.prepareStatement(seqCheckSQL)) {
                seqStmt.setInt(1, finalTicketNum);
                ResultSet rs = seqStmt.executeQuery();
                if (rs.next()) nextSeq = rs.getInt(1) + 1;
            }

            String segmentSQL = "INSERT INTO Ticket_Segment (ticket_number, sequence_number, flight_number, " +
                "airline_id, flight_date, class, special_meal, seat_number, from_airport, to_airport) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement sStmt = conn.prepareStatement(segmentSQL)) {
                for (String fNum : legs) {
                    fNum = fNum.trim();
                    String depAir = "", arrAir = "";
                    Date fDate = null;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT departure_airport, arrival_airport, flight_date FROM Flight WHERE flight_number = ?")) {
                        ps.setString(1, fNum);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) { depAir = rs.getString(1); arrAir = rs.getString(2); fDate = rs.getDate(3); }
                    }
                    for (int i = 0; i < qtyToAdd; i++) {
                        String seat = (new Random().nextInt(30) + 1) + "" + (char)('A' + new Random().nextInt(6));
                        sStmt.setInt(1, finalTicketNum); sStmt.setInt(2, nextSeq++);
                        sStmt.setString(3, fNum); sStmt.setString(4, airlineId);
                        sStmt.setDate(5, fDate); sStmt.setString(6, seatClass);
                        sStmt.setBoolean(7, meal); sStmt.setString(8, seat);
                        sStmt.setString(9, depAir); sStmt.setString(10, arrAir);
                        sStmt.addBatch();
                    }
                }
                sStmt.executeBatch();
            }
            removeFromWaitingList(customerId, flightNums);
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace(); return false;
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public static ResultSet getCustomerTickets(int customerId) throws SQLException {
        String query = "SELECT t.ticket_number, " +
            "GROUP_CONCAT(DISTINCT s.flight_number ORDER BY s.sequence_number SEPARATOR ', ') AS flight_number, " +
            "MAX(a.name) AS airline_name, " +
            "(SELECT s2.flight_date FROM Ticket_Segment s2 WHERE s2.ticket_number = t.ticket_number ORDER BY s2.sequence_number ASC LIMIT 1) AS dep_date, " +
            "(SELECT f.arrival_date FROM Ticket_Segment s3 JOIN Flight f ON s3.flight_number = f.flight_number WHERE s3.ticket_number = t.ticket_number ORDER BY s3.sequence_number DESC LIMIT 1) AS arr_date, " +
            "(SELECT f.arrival_time FROM Ticket_Segment s3 JOIN Flight f ON s3.flight_number = f.flight_number WHERE s3.ticket_number = t.ticket_number ORDER BY s3.sequence_number DESC LIMIT 1) AS arr_time, " +
            "(SELECT from_airport FROM Ticket_Segment s2 WHERE s2.ticket_number = t.ticket_number ORDER BY sequence_number ASC LIMIT 1) AS from_airport, " +
            "(SELECT to_airport FROM Ticket_Segment s2 WHERE s2.ticket_number = t.ticket_number ORDER BY sequence_number DESC LIMIT 1) AS to_airport, " +
            "MAX(s.class) AS class, t.total_fare, t.status, t.is_flexible, t.quantity, " +
            "COALESCE(t.ticket_type,'one-way') AS ticket_type, COALESCE(t.booking_fee,25.00) AS booking_fee " +
            "FROM Ticket t " +
            "JOIN Ticket_Segment s ON t.ticket_number = s.ticket_number " +
            "JOIN Airline a ON s.airline_id = a.airline_id " +
            "WHERE t.customer_id = ? AND t.status NOT IN ('cancelled','cancelled_with_fee') " +
            "GROUP BY t.ticket_number";
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, customerId);
        return stmt.executeQuery();
    }

    public static boolean isAlreadyBooked(int customerId, String flightNum, String airlineId) {
        String sql = "SELECT COUNT(*) FROM Ticket t JOIN Ticket_Segment s ON t.ticket_number = s.ticket_number " +
            "WHERE t.customer_id = ? AND s.flight_number = ? AND s.airline_id = ? AND t.status = 'active'";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId); stmt.setString(2, flightNum); stmt.setString(3, airlineId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    /** Cancel a business/first class ticket — no fee, hard delete. */
    public static boolean cancelBooking(int ticketNum) {
        String del1 = "DELETE FROM Ticket_Segment WHERE ticket_number = ?";
        String del2 = "DELETE FROM Ticket WHERE ticket_number = ?";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement s1 = conn.prepareStatement(del1);
                 PreparedStatement s2 = conn.prepareStatement(del2)) {
                s1.setInt(1, ticketNum); s1.executeUpdate();
                s2.setInt(1, ticketNum); s2.executeUpdate();
            }
            conn.commit(); return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /**
     * Cancel an Economy ticket with a $50 fee.
     * Marks the ticket as 'cancelled_with_fee' (audit trail) and removes segments.
     */
    public static boolean cancelBookingWithFee(int ticketNum, float fee) {
        String mark = "UPDATE Ticket SET status='cancelled_with_fee', " +
                      "total_fare = GREATEST(total_fare - ?, 0) WHERE ticket_number = ?";
        String del  = "DELETE FROM Ticket_Segment WHERE ticket_number = ?";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement s1 = conn.prepareStatement(mark);
                 PreparedStatement s2 = conn.prepareStatement(del)) {
                s1.setFloat(1, fee); s1.setInt(2, ticketNum); s1.executeUpdate();
                s2.setInt(1, ticketNum); s2.executeUpdate();
            }
            conn.commit(); return true;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static boolean registerCustomer(String fName, String lName, String email, String phone,
            String addr, String dob, String user, String pass) {
        String sql = "INSERT INTO Customer (first_name, last_name, email, phone, address, dob, username, password, account_creation_date) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURDATE())";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fName); stmt.setString(2, lName); stmt.setString(3, email);
            stmt.setString(4, phone); stmt.setString(5, addr); stmt.setString(6, dob);
            stmt.setString(7, user); stmt.setString(8, pass);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static String getAccountCreationDate(String username) {
        String query = "SELECT account_creation_date FROM Customer WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("account_creation_date");
        } catch (SQLException e) { e.printStackTrace(); }
        return "Unknown";
    }

    public static void addToWaitingList(int customerId, String flightNum, String airlineId, String seatClass) {
        String sql = "INSERT INTO Waiting_List (customer_id, flight_number, airline_id, class, added_date) VALUES (?, ?, ?, ?, NOW())";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId); stmt.setString(2, flightNum);
            stmt.setString(3, airlineId); stmt.setString(4, seatClass);
            stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static int getSeatsRemaining(String flightNums, String seatClass) {
        String[] legs = flightNums.split(",");
        int minSeats = Integer.MAX_VALUE;
        for (String fNum : legs) {
            fNum = fNum.trim();
            String capCol = seatClass.equalsIgnoreCase("Economy") ? "econ_capacity" :
                            seatClass.equalsIgnoreCase("Business") ? "bus_capacity" : "first_capacity";
            String query = "SELECT (COALESCE(a." + capCol + ", 0) - (SELECT COUNT(*) FROM Ticket_Segment s " +
                "WHERE s.flight_number = f.flight_number AND s.class = ?)) as left_count " +
                "FROM Flight f JOIN Aircraft a ON f.aircraft_id = a.aircraft_id WHERE f.flight_number = ?";
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, seatClass); stmt.setString(2, fNum);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) { int s = rs.getInt("left_count"); if (s < minSeats) minSeats = s; }
                else { return 0; }
            } catch (SQLException e) { e.printStackTrace(); }
        }
        return (minSeats == Integer.MAX_VALUE) ? 0 : minSeats;
    }

    public static boolean isAlreadyOnWaitingList(int customerId, String flightNum) {
        String sql = "SELECT COUNT(*) FROM Waiting_List WHERE customer_id = ? AND flight_number = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId); stmt.setString(2, flightNum);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    public static int getWaitlistPosition(int customerId, String flightNum) {
        String sql = "SELECT COUNT(*) + 1 FROM Waiting_List WHERE flight_number = ? AND added_date < " +
                    "(SELECT added_date FROM Waiting_List WHERE customer_id = ? AND flight_number = ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, flightNum); stmt.setInt(2, customerId); stmt.setString(3, flightNum);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 1;
    }

    public static ResultSet getQAEntries(String keyword) throws SQLException {
        String query = "SELECT question_text, COALESCE(answer_text, 'Pending Response...') AS answer_text " +
                       "FROM QA_Entry WHERE question_text LIKE ? OR answer_text LIKE ? ORDER BY created_at DESC";
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(query);
        String p = "%" + keyword + "%"; stmt.setString(1, p); stmt.setString(2, p);
        return stmt.executeQuery();
    }

    public static boolean postQuestion(int customerId, String question) {
        String sql = "INSERT INTO QA_Entry (customer_id, question_text) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId); stmt.setString(2, question);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static String checkWaitlistAlerts(int customerId) {
        String query = "SELECT flight_number, airline_id, class FROM Waiting_List WHERE customer_id = ?";
        StringBuilder available = new StringBuilder();
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String fNum = rs.getString("flight_number");
                String aId  = rs.getString("airline_id");
                String sc   = rs.getString("class");
                if (getSeatsRemaining(fNum, sc) > 0) {
                    if (available.length() > 0) available.append("\n");
                    available.append("- ").append(aId).append(" ").append(fNum).append(" (").append(sc).append(")");
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return available.toString();
    }

    public static void removeFromWaitingList(int customerId, String flightNum) {
        String sql = "DELETE FROM Waiting_List WHERE customer_id = ? AND flight_number = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId); stmt.setString(2, flightNum); stmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static boolean validateEmployeeLogin(String username, String password) {
        String query = "SELECT * FROM Employee WHERE username = ? AND password = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username); stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery(); return rs.next();
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static String getEmployeeRole(String username) {
        String query = "SELECT role FROM Employee WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("role");
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public static ResultSet getPendingQuestions() throws SQLException {
        String query = "SELECT q.qa_id, COALESCE(c.username, 'System') as author, q.question_text " +
                       "FROM QA_Entry q LEFT JOIN Customer c ON q.customer_id = c.customer_id " +
                       "WHERE q.answer_text IS NULL ORDER BY q.created_at ASC";
        Connection conn = getConnection();
        return conn.prepareStatement(query).executeQuery();
    }

    public static boolean answerQuestion(int qaId, String answerText) {
        String sql = "UPDATE QA_Entry SET answer_text = ? WHERE qa_id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, answerText); stmt.setInt(2, qaId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public static ResultSet getFlightWaitlist(String flightNum) throws SQLException {
        String query = "SELECT c.first_name, c.last_name, w.class, w.added_date " +
                       "FROM Waiting_List w JOIN Customer c ON w.customer_id = c.customer_id " +
                       "WHERE w.flight_number = ? ORDER BY w.added_date ASC";
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, flightNum);
        return stmt.executeQuery();
    }

    public static ResultSet getAirportTraffic(String airportCode) throws SQLException {
        String query = "SELECT flight_number, airline_id, departure_airport, arrival_airport, " +
                       "flight_date, arrival_date, departure_time, arrival_time " +
                       "FROM Flight WHERE departure_airport = ? OR arrival_airport = ? " +
                       "ORDER BY flight_date ASC, departure_time ASC";
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, airportCode); stmt.setString(2, airportCode);
        return stmt.executeQuery();
    }

    // ---- Aircraft CRUD ----
    public static ResultSet getAllAircraft() throws SQLException {
        return getConnection().createStatement().executeQuery("SELECT * FROM Aircraft ORDER BY aircraft_id");
    }
    public static boolean addAircraft(int id, String model, String man, int total, int econ, int bus, int first, String airId) {
        String sql = "INSERT INTO Aircraft (aircraft_id, model, manufacturer, total_seats, econ_capacity, bus_capacity, first_capacity, airline_id) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1,id);stmt.setString(2,model);stmt.setString(3,man);stmt.setInt(4,total);
            stmt.setInt(5,econ);stmt.setInt(6,bus);stmt.setInt(7,first);stmt.setString(8,airId);
            return stmt.executeUpdate()>0;
        } catch(SQLException e){e.printStackTrace();return false;}
    }
    public static boolean updateAircraft(int id,String model,String man,int total,int econ,int bus,int first,String airId){
        String sql="UPDATE Aircraft SET model=?,manufacturer=?,total_seats=?,econ_capacity=?,bus_capacity=?,first_capacity=?,airline_id=? WHERE aircraft_id=?";
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement(sql)){
            stmt.setString(1,model);stmt.setString(2,man);stmt.setInt(3,total);stmt.setInt(4,econ);
            stmt.setInt(5,bus);stmt.setInt(6,first);stmt.setString(7,airId);stmt.setInt(8,id);
            return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }
    public static boolean deleteAircraft(int id){
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement("DELETE FROM Aircraft WHERE aircraft_id=?")){
            stmt.setInt(1,id);return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }

    // ---- Airport CRUD ----
    public static ResultSet getAllAirports() throws SQLException {
        return getConnection().createStatement().executeQuery("SELECT * FROM Airport ORDER BY airport_code");
    }
    public static boolean addAirport(String code, String name, String city, String country) {
        String sql="INSERT INTO Airport (airport_code,name,city,country) VALUES (?,?,?,?)";
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement(sql)){
            stmt.setString(1,code);stmt.setString(2,name);stmt.setString(3,city);stmt.setString(4,country);
            return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }
    /** Update name/city/country of an airport (airport_code PK cannot change). */
    public static boolean updateAirport(String code, String name, String city, String country) {
        String sql="UPDATE Airport SET name=?,city=?,country=? WHERE airport_code=?";
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement(sql)){
            stmt.setString(1,name);stmt.setString(2,city);stmt.setString(3,country);stmt.setString(4,code);
            return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }
    public static boolean deleteAirport(String code){
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement("DELETE FROM Airport WHERE airport_code=?")){
            stmt.setString(1,code);return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }

    // ---- Flight CRUD ----
    public static ResultSet getAllFlightsRaw() throws SQLException {
        return getConnection().createStatement().executeQuery("SELECT * FROM Flight ORDER BY flight_date DESC, departure_time ASC");
    }
    public static boolean addFlight(String fNum,String aId,int acId,String dep,String arr,
            String fDate,String aDate,String dTime,String aTime,String type,float price){
        String sql="INSERT INTO Flight (flight_number,airline_id,aircraft_id,departure_airport,arrival_airport," +
                   "flight_date,arrival_date,departure_time,arrival_time,flight_type,base_price) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement(sql)){
            stmt.setString(1,fNum);stmt.setString(2,aId);stmt.setInt(3,acId);stmt.setString(4,dep);
            stmt.setString(5,arr);stmt.setDate(6,Date.valueOf(fDate));stmt.setDate(7,Date.valueOf(aDate));
            stmt.setTime(8,Time.valueOf(dTime));stmt.setTime(9,Time.valueOf(aTime));
            stmt.setString(10,type);stmt.setFloat(11,price);
            return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }
    /** Edit an existing flight. PK (flight_number, airline_id) cannot change. */
    public static boolean updateFlight(String fNum,String aId,int acId,String dep,String arr,
            String fDate,String aDate,String dTime,String aTime,String type,float price){
        String sql="UPDATE Flight SET aircraft_id=?,departure_airport=?,arrival_airport=?," +
                   "flight_date=?,arrival_date=?,departure_time=?,arrival_time=?," +
                   "flight_type=?,base_price=? WHERE flight_number=? AND airline_id=?";
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement(sql)){
            stmt.setInt(1,acId);stmt.setString(2,dep);stmt.setString(3,arr);
            stmt.setDate(4,Date.valueOf(fDate));stmt.setDate(5,Date.valueOf(aDate));
            stmt.setTime(6,Time.valueOf(dTime));stmt.setTime(7,Time.valueOf(aTime));
            stmt.setString(8,type);stmt.setFloat(9,price);
            stmt.setString(10,fNum);stmt.setString(11,aId);
            return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }
    public static boolean deleteFlight(String fNum,String aId){
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement("DELETE FROM Flight WHERE flight_number=? AND airline_id=?")){
            stmt.setString(1,fNum);stmt.setString(2,aId);return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }

    // ---- Customer / Employee CRUD (admin) ----
    public static ResultSet getAllCustomers() throws SQLException {
        return getConnection().createStatement().executeQuery("SELECT * FROM Customer ORDER BY last_name,first_name");
    }
    public static boolean updateCustomer(int id,String fName,String lName,String email,String phone,String addr,String user,String pass){
        String sql="UPDATE Customer SET first_name=?,last_name=?,email=?,phone=?,address=?,username=?,password=? WHERE customer_id=?";
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement(sql)){
            stmt.setString(1,fName);stmt.setString(2,lName);stmt.setString(3,email);stmt.setString(4,phone);
            stmt.setString(5,addr);stmt.setString(6,user);stmt.setString(7,pass);stmt.setInt(8,id);
            return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }
    public static boolean deleteCustomer(int id){
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement("DELETE FROM Customer WHERE customer_id=?")){
            stmt.setInt(1,id);return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }
    public static ResultSet getAllEmployees() throws SQLException {
        return getConnection().createStatement().executeQuery("SELECT * FROM Employee ORDER BY role,last_name");
    }
    public static boolean addEmployee(int id,String fName,String lName,String email,String phone,String user,String pass,String role){
        String sql="INSERT INTO Employee (employee_id,first_name,last_name,email,phone,username,password,role) VALUES (?,?,?,?,?,?,?,?)";
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement(sql)){
            stmt.setInt(1,id);stmt.setString(2,fName);stmt.setString(3,lName);stmt.setString(4,email);
            stmt.setString(5,phone);stmt.setString(6,user);stmt.setString(7,pass);stmt.setString(8,role);
            return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }
    public static boolean updateEmployee(int id,String fName,String lName,String email,String phone,String user,String pass,String role){
        String sql="UPDATE Employee SET first_name=?,last_name=?,email=?,phone=?,username=?,password=?,role=? WHERE employee_id=?";
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement(sql)){
            stmt.setString(1,fName);stmt.setString(2,lName);stmt.setString(3,email);stmt.setString(4,phone);
            stmt.setString(5,user);stmt.setString(6,pass);stmt.setString(7,role);stmt.setInt(8,id);
            return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }
    public static boolean deleteEmployee(int id){
        try(Connection conn=getConnection();PreparedStatement stmt=conn.prepareStatement("DELETE FROM Employee WHERE employee_id=?")){
            stmt.setInt(1,id);return stmt.executeUpdate()>0;
        }catch(SQLException e){e.printStackTrace();return false;}
    }

    // ---- Admin Reports ----
    public static ResultSet getMonthlySales(int month,int year) throws SQLException {
        PreparedStatement stmt=getConnection().prepareStatement(
            "SELECT ticket_number,customer_id,total_fare,purchase_datetime FROM Ticket WHERE MONTH(purchase_datetime)=? AND YEAR(purchase_datetime)=?");
        stmt.setInt(1,month);stmt.setInt(2,year);return stmt.executeQuery();
    }
    public static ResultSet getReservationsByFlight(String flightNum) throws SQLException {
        PreparedStatement stmt=getConnection().prepareStatement(
            "SELECT t.ticket_number,c.first_name,c.last_name,ts.class FROM Ticket t " +
            "JOIN Customer c ON t.customer_id=c.customer_id " +
            "JOIN Ticket_Segment ts ON t.ticket_number=ts.ticket_number WHERE ts.flight_number=?");
        stmt.setString(1,flightNum);return stmt.executeQuery();
    }
    public static ResultSet getReservationsByCustomer(String firstName,String lastName) throws SQLException {
        PreparedStatement stmt=getConnection().prepareStatement(
            "SELECT t.ticket_number,ts.flight_number,ts.airline_id,t.total_fare FROM Ticket t " +
            "JOIN Customer c ON t.customer_id=c.customer_id " +
            "JOIN Ticket_Segment ts ON t.ticket_number=ts.ticket_number WHERE c.first_name LIKE ? AND c.last_name LIKE ?");
        stmt.setString(1,"%"+firstName+"%");stmt.setString(2,"%"+lastName+"%");return stmt.executeQuery();
    }
    public static ResultSet getRevenueByFlight() throws SQLException {
        return getConnection().createStatement().executeQuery(
            "SELECT flight_number,SUM(total_fare) as revenue FROM Ticket t JOIN Ticket_Segment ts ON t.ticket_number=ts.ticket_number GROUP BY flight_number");
    }
    public static ResultSet getRevenueByAirline() throws SQLException {
        return getConnection().createStatement().executeQuery(
            "SELECT airline_id,SUM(total_fare) as revenue FROM Ticket t JOIN Ticket_Segment ts ON t.ticket_number=ts.ticket_number GROUP BY airline_id");
    }
    public static ResultSet getRevenueByCustomer() throws SQLException {
        return getConnection().createStatement().executeQuery(
            "SELECT c.first_name,c.last_name,SUM(t.total_fare) as revenue FROM Ticket t JOIN Customer c ON t.customer_id=c.customer_id GROUP BY c.customer_id");
    }
    public static ResultSet getTopCustomer() throws SQLException {
        return getConnection().createStatement().executeQuery(
            "SELECT c.first_name,c.last_name,SUM(t.total_fare) as revenue FROM Ticket t JOIN Customer c ON t.customer_id=c.customer_id GROUP BY c.customer_id ORDER BY revenue DESC LIMIT 1");
    }
    public static ResultSet getTopFlights() throws SQLException {
        return getConnection().createStatement().executeQuery(
            "SELECT flight_number,COUNT(*) as sales FROM Ticket_Segment GROUP BY flight_number ORDER BY sales DESC LIMIT 5");
    }
    public static ResultSet getCustomerTickets(String username) throws SQLException {
        PreparedStatement stmt=getConnection().prepareStatement(
            "SELECT t.ticket_number,ts.flight_number,ts.airline_id,ts.class,t.total_fare,ts.seat_number " +
            "FROM Ticket t JOIN Ticket_Segment ts ON t.ticket_number=ts.ticket_number " +
            "JOIN Customer c ON t.customer_id=c.customer_id WHERE c.username=? AND t.status='active'");
        stmt.setString(1,username);return stmt.executeQuery();
    }
    public static boolean updateTicketDetails(int ticketNum,String newClass,String newSeat,float newTotal){
        try(Connection conn=getConnection()){
            conn.setAutoCommit(false);
            try(PreparedStatement s1=conn.prepareStatement("UPDATE Ticket_Segment SET class=?,seat_number=? WHERE ticket_number=?");
                PreparedStatement s2=conn.prepareStatement("UPDATE Ticket SET total_fare=? WHERE ticket_number=?")){
                s1.setString(1,newClass);s1.setString(2,newSeat);s1.setInt(3,ticketNum);s1.executeUpdate();
                s2.setFloat(1,newTotal);s2.setInt(2,ticketNum);s2.executeUpdate();
                conn.commit();return true;
            }catch(SQLException ex){conn.rollback();ex.printStackTrace();return false;}
        }catch(SQLException e){e.printStackTrace();return false;}
    }
}
