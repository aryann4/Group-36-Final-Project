================================================================================
  GROUP 36 — Online Travel Reservation System
  Youssef Hassan | Aryan Navin | Derick Robles
================================================================================

────────────────────────────────────────────────────────────────────────────────
  LOGIN CREDENTIALS
────────────────────────────────────────────────────────────────────────────────

  Admin
    Username : admin123
    Password : password123

  Customer Representative
    Username : rep123
    Password : password123

  Sample Customers (for testing)
    Username : an838      Password : password123
    Username : yhh10      Password : password123
    Username : dmr386     Password : password123

  New customers can also be created from the login screen using "Sign Up",
  or by an admin through the User Management panel.

────────────────────────────────────────────────────────────────────────────────
  STEP 1 — INSTALL MYSQL (if not already installed)
────────────────────────────────────────────────────────────────────────────────

  1. Install the Visual Studio Redistributable (Windows only):
       https://learn.microsoft.com/en-US/cpp/windows/latest-supported-vcredist

  2. Download MySQL Installer from:
       https://dev.mysql.com/downloads/workbench/

       - Windows : download "Windows (x86, 32 & 64 bit), MySQL Installer MSI"
       - macOS   : download "macOS 10.15 (x86, 64-bit), DMG Archive"

  3. Launch the installer and click "Add".

  4. From the Available Products panel select:
       - MySQL Server 8.0.21 (or above)
       - MySQL Workbench (if not already installed)
     Push both to the right panel and click Next > Execute.

  5. Follow the on-screen instructions. Keep all default settings.

  6. When prompted, set a root password. Write it down — you will need it.

────────────────────────────────────────────────────────────────────────────────
  STEP 2 — IMPORT THE DATABASE
────────────────────────────────────────────────────────────────────────────────

  1. Open MySQL Workbench and connect to your local server:
       Hostname : 127.0.0.1
       Port     : 3306
       Username : root
       Password : (the root password you set during installation)

  2. In the top menu go to:
       Server > Data Import

  3. Select "Import from Self-Contained File" and browse to:
       36_project.sql

  4. Under "Default Target Schema" click "New..." and create a schema named:
       TravelReservationDB

  5. Click "Start Import". Wait for it to finish with no errors.

────────────────────────────────────────────────────────────────────────────────
  STEP 3 — UPDATE THE DATABASE PASSWORD IN THE CODE
────────────────────────────────────────────────────────────────────────────────

  Open  src/DatabaseHelper.java  and find these lines near the top:

      private static final String URL  = "jdbc:mysql://localhost:3306/TravelReservationDB";
      private static final String USER = "root";
      private static final String PASSWORD = "group36rootpasswordPIDM";

  Change the PASSWORD value to match the root password you set in Step 1.
  Save the file.

────────────────────────────────────────────────────────────────────────────────
  STEP 4 — RUN THE PROJECT IN VS CODE
────────────────────────────────────────────────────────────────────────────────

  Requirements:
    - Java JDK 11 or above
    - VS Code with the "Extension Pack for Java" installed

  Steps:
  1. Open VS Code and select File > Open Folder, then open the project folder
     (the folder that contains the "src" and "lib" directories).

  2. VS Code will automatically detect the Java project.

  3. Open  src/Main.java

  4. Click the "Run" button (triangle) at the top right, or right-click
     Main.java and select "Run Java".

  The login window will appear. Use the credentials listed above.

────────────────────────────────────────────────────────────────────────────────
  STEP 5 — RUN USING THE JAR FILE (alternative)
────────────────────────────────────────────────────────────────────────────────

  A pre-built jar file is included as  36_project.jar

  Make sure MySQL is running, then open a terminal and run:

      java -cp "36_project.jar;lib/mysql-connector-j-9.7.0.jar" src.Main

  On macOS / Linux use a colon instead of semicolon:

      java -cp "36_project.jar:lib/mysql-connector-j-9.7.0.jar" src.Main

────────────────────────────────────────────────────────────────────────────────
  PROJECT NOTES
────────────────────────────────────────────────────────────────────────────────

  - All sample flight data uses dates in May 2026. When searching, use dates
    such as 2026-05-08 or enable the "Flexible (+/- 3 days)" checkbox to
    find nearby results.

  - Supported airport codes in the sample data:
      EWR, JFK, LAX, ORD, SFO, MIA

  - Supported airline codes in the sample data:
      UA (United Airlines), AA (American Airlines), DL (Delta),
      SW (Southwest Airlines), JB (JetBlue Airways)

  - Economy class tickets cannot be cancelled. Only Business and First class
    tickets are eligible for cancellation at no charge.

  - The waiting list alert appears as a popup when a customer logs in,
    if a seat has become available on a flight they are waitlisted for.

  - Indirect (1-stop) flights are generated automatically via a database
    view (IndirectFlights) that joins flights sharing a stopover airport.

================================================================================
