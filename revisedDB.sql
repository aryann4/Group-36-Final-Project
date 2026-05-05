CREATE DATABASE IF NOT EXISTS TravelReservationDB;
USE TravelReservationDB;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS QA_Entry;
DROP TABLE IF EXISTS Waiting_List;
DROP TABLE IF EXISTS Ticket_Segment;
DROP TABLE IF EXISTS Ticket;
DROP TABLE IF EXISTS Employee;
DROP TABLE IF EXISTS Customer;
DROP TABLE IF EXISTS Flight_Days;
DROP TABLE IF EXISTS Flight;
DROP TABLE IF EXISTS Airline_Airport;
DROP TABLE IF EXISTS Aircraft;
DROP TABLE IF EXISTS Airport;
DROP TABLE IF EXISTS Airline;
SET FOREIGN_KEY_CHECKS = 1;


CREATE TABLE Airline (
    airline_id CHAR(2) PRIMARY KEY,
    name VARCHAR(100)
);

CREATE TABLE Airport (
    airport_code CHAR(3) PRIMARY KEY,
    name VARCHAR(100),
    city VARCHAR(100),
    country VARCHAR(100)
);

CREATE TABLE Aircraft (
    aircraft_id INT PRIMARY KEY,
    model VARCHAR(100),
    manufacturer VARCHAR(100),
    total_seats INT,
    econ_capacity INT, 
    bus_capacity INT,
    first_capacity INT,
    airline_id CHAR(2),
    FOREIGN KEY (airline_id) REFERENCES Airline (airline_id)
);

CREATE TABLE Airline_Airport (
    airline_id CHAR(2),
    airport_code CHAR(3),
    PRIMARY KEY (airline_id, airport_code),
    FOREIGN KEY (airline_id) REFERENCES Airline(airline_id),
    FOREIGN KEY (airport_code) REFERENCES Airport(airport_code)
);

CREATE TABLE Flight (
    flight_number VARCHAR(10),
    airline_id CHAR(2),
    aircraft_id INT,
    departure_airport CHAR(3),
    arrival_airport CHAR(3),
    flight_date DATE,
    arrival_date DATE,
    departure_time TIME,
    arrival_time TIME,
    flight_type VARCHAR(15),
    base_price FLOAT,
    PRIMARY KEY (flight_number, airline_id),
    FOREIGN KEY (airline_id) REFERENCES Airline(airline_id),
    FOREIGN KEY (aircraft_id) REFERENCES Aircraft(aircraft_id),
    FOREIGN KEY (departure_airport) REFERENCES Airport(airport_code),
    FOREIGN KEY (arrival_airport) REFERENCES Airport(airport_code)
);

CREATE TABLE Flight_Days (
    flight_number VARCHAR(10),
    airline_id CHAR(2),
    day_of_week VARCHAR(15),
    PRIMARY KEY (flight_number, airline_id, day_of_week),
    FOREIGN KEY (flight_number, airline_id) REFERENCES Flight (flight_number, airline_id)
);

CREATE TABLE Customer (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    address VARCHAR(255),
    dob DATE,
    username VARCHAR(50) UNIQUE,
    password VARCHAR(255),
    account_creation_date DATE
);

CREATE TABLE Employee (
    employee_id INT PRIMARY KEY,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    username VARCHAR(50) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(25)
);

CREATE TABLE Ticket (
    ticket_number INT PRIMARY KEY,
    customer_id INT,
    total_fare FLOAT,
    purchase_datetime DATETIME,
    status VARCHAR(15), 
    is_flexible BOOLEAN,
    quantity INT DEFAULT 1, 
    FOREIGN KEY (customer_id) REFERENCES Customer(customer_id)
);

CREATE TABLE Ticket_Segment (
    ticket_number INT,
    sequence_number INT,
    flight_number VARCHAR(10),
    airline_id CHAR(2),
    flight_date DATE,
    class VARCHAR(10),
    special_meal BOOLEAN, 
    seat_number VARCHAR(10),
    from_airport CHAR(3),
    to_airport CHAR(3),
    PRIMARY KEY (ticket_number, sequence_number),
    FOREIGN KEY (ticket_number) REFERENCES Ticket(ticket_number),
    FOREIGN KEY (flight_number, airline_id) REFERENCES Flight(flight_number, airline_id),
    FOREIGN KEY (from_airport) REFERENCES Airport(airport_code),
    FOREIGN KEY (to_airport) REFERENCES Airport(airport_code)
);

CREATE TABLE Waiting_List (
    waitlist_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    flight_number VARCHAR(50),
    airline_id VARCHAR(5),
    class VARCHAR(20),
    added_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES Customer(customer_id)
);

CREATE TABLE QA_Entry (
    qa_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    question_text TEXT NOT NULL,
    answer_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES Customer(customer_id)
);

CREATE OR REPLACE VIEW IndirectFlights AS
SELECT 
    f1.flight_number AS f1_num, f1.airline_id AS air1, f1.departure_airport AS dep, f1.arrival_airport AS stopover, 
    f1.departure_time AS dep_t1, f1.arrival_time AS arr_t1, f1.flight_date AS date1,
    f2.flight_number AS f2_num, f2.airline_id AS air2, f2.arrival_airport AS dest, 
    f2.departure_time AS dep_t2, f2.arrival_time AS arr_t2, f2.arrival_date AS date2_arr,
    (f1.base_price + f2.base_price) AS total_price
FROM Flight f1
JOIN Flight f2 ON f1.arrival_airport = f2.departure_airport
WHERE f1.airline_id = f2.airline_id 
AND (f2.flight_date > f1.arrival_date OR (f2.flight_date = f1.arrival_date AND f2.departure_time > f1.arrival_time));

INSERT IGNORE INTO Airport (airport_code, name, city, country) VALUES
('EWR', 'Newark Liberty International', 'Newark', 'USA'),
('JFK', 'John F. Kennedy International', 'New York', 'USA'),
('ORD', 'O''Hare International', 'Chicago', 'USA'),
('LAX', 'Los Angeles International', 'Los Angeles', 'USA'),
('SFO', 'San Francisco International', 'San Francisco', 'USA'),
('MIA', 'Miami International', 'Miami', 'USA');

INSERT IGNORE INTO Airline (airline_id, name) VALUES 
('UA', 'United Airlines'),
('AA', 'American Airlines'),
('DL', 'Delta'),
('SW', 'Southwest Airlines'),
('JB', 'JetBlue Airways');

INSERT IGNORE INTO Aircraft (aircraft_id, model, manufacturer, total_seats, econ_capacity, bus_capacity, first_capacity, airline_id) VALUES 
(1, '737', 'Boeing', 200, 150, 30, 20, 'UA'),
(2, '747', 'Boeing', 200, 150, 30, 20, 'AA'),
(3, 'A320', 'Airbus', 150, 100, 30, 20, 'UA'),
(99, 'Test Plane', 'Boeing', 0, 1, 0, 0, 'UA');

INSERT IGNORE INTO Flight (flight_number, airline_id, aircraft_id, departure_airport, arrival_airport, departure_time, arrival_time, flight_type, base_price, flight_date, arrival_date) VALUES
('UA101', 'UA', 1, 'EWR', 'ORD', '08:00:00', '10:30:00', 'domestic', 150.00, '2026-04-26', '2026-04-26'),
('UA303', 'UA', 3, 'ORD', 'LAX', '12:30:00', '15:00:00', 'domestic', 200.00, '2026-04-26', '2026-04-26'),
('AA404', 'AA', 2, 'JFK', 'LAX', '22:00:00', '01:00:00', 'domestic', 450.00, '2026-04-26', '2026-04-27'),
('UA505', 'UA', 3, 'EWR', 'LAX', '10:00:00', '16:00:00', 'domestic', 300.00, '2026-04-24', '2026-04-24'),
('DL606', 'DL', 2, 'JFK', 'ORD', '09:00:00', '10:30:00', 'domestic', 550.00, '2026-04-29', '2026-04-29'),
('UA707', 'UA', 1, 'EWR', 'SFO', '12:00:00', '15:45:00', 'domestic', 890.00, '2026-04-26', '2026-04-26'),
('AA808', 'AA', 2, 'JFK', 'MIA', '05:30:00', '08:30:00', 'domestic', 1500.00, '2026-04-25', '2026-04-25'),
('SW909', 'SW', 3, 'EWR', 'ORD', '18:00:00', '20:15:00', 'domestic', 210.00, '2026-04-26', '2026-04-26'),
('AA111', 'AA', 2, 'JFK', 'LAX', '14:00:00', '19:00:00', 'domestic', 320.00, '2026-04-27', '2026-04-27'),
('UA999', 'UA', 1, 'LAX', 'EWR', '10:00:00', '18:00:00', 'domestic', 300.00, '2026-04-30', '2026-04-30'),
('UA888', 'UA', 1, 'LAX', 'ORD', '07:00:00', '09:00:00', 'domestic', 100.00, '2026-04-30', '2026-04-30'),
('UA777', 'UA', 1, 'ORD', 'EWR', '14:00:00', '16:30:00', 'domestic', 120.00, '2026-04-30', '2026-04-30'),
('UA000', 'UA', 99, 'EWR', 'LAX', '12:00:00', '15:00:00', 'domestic', 100.00, '2026-04-26', '2026-04-26');

INSERT IGNORE INTO Customer (first_name, last_name, email, username, password, account_creation_date) VALUES 
('Aryan', 'Navin', 'test@rutgers.edu', 'an838', 'password123', '2026-04-26'), 
('Youssef', 'Hassan', 'test@rutgers.edu', 'yhh10', 'password123', '2026-04-25'), 
('Derick', 'Robles', 'test@rutgers.edu', 'dmr386', 'password123', '2026-04-26');

INSERT IGNORE INTO QA_Entry (question_text, answer_text) VALUES 
('What is the baggage allowance for Economy?', 'Standard allowance is one carry-on and one checked bag.'),
('How do I cancel my booking?', 'You can cancel through the "My Reservations" dashboard. Note that Economy tickets incur a $50 fee.'),
('Do you offer special meals?', 'Yes, you can request special meals during the booking process at no extra charge.'),
('What happens if I am on a waiting list?', 'You will receive an alert if a seat becomes available. Your position depends on when you joined.');


INSERT INTO Employee (employee_id, first_name, last_name, username, password, role) VALUES 
(1, 'Test', 'Rep', 'rep123', 'password123', 'representative'), 
(2, 'System', 'Admin', 'admin123', 'password123', 'admin');

INSERT INTO Flight_Days VALUES ('UA101', 'UA', 'Monday'), ('UA101', 'UA', 'Wednesday');