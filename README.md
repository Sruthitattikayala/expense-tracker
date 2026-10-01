# Expense Tracker (Console Application)

A robust, console-based personal expense management application developed in Java and backed by MySQL. Built with a clean 4-tier layered architecture, this application demonstrates object-oriented programming (OOP), the Data Access Object (DAO) pattern, secure parameterized SQL using JDBC, automated project lifecycle management via Maven, and CI pipeline automation via Jenkins.

---

## Features

1. **Add Expense**  
   Record new expenses with amount, category, description, and date. Includes input validation for positive amounts, non-empty text, and valid calendar dates.
2. **View Expenses**  
   Displays all stored expenses retrieved directly from MySQL.
3. **Delete Expense**  
   Removes a specific expense by ID from the database using parameterized queries.
4. **Update Expense**  
   Modifies amount, category, description, and date for an existing record.
5. **Total Expense**  
   Calculates the overall sum of all recorded expenses using SQL's `SUM()` aggregate function.
6. **Category Summary**  
   Aggregates total spending grouped by category using SQL `GROUP BY`.
7. **Spending Insights**  
   Calculates and displays the percentage distribution of spending across all categories.

---

## Technology Stack

* **Language:** Java 17+
* **Build & Dependency Management:** Apache Maven
* **Database:** MySQL Server 8.0
* **Persistence:** Java Database Connectivity (JDBC) with MySQL Connector/J 8.3.0
* **Continuous Integration:** Jenkins (Declarative Pipeline)
* **Architecture:** 4-Tier Layered Architecture (UI $\rightarrow$ Service $\rightarrow$ DAO $\rightarrow$ Database)

---

## Architecture Overview

```
                      ┌─────────────────────────┐
                      │    Requirements.java    │  (Presentation / UI Layer)
                      └────────────┬────────────┘
                                   │ delegates
                                   ▼
                      ┌─────────────────────────┐
                      │   ExpenseService.java   │  (Business / Service Layer)
                      └────────────┬────────────┘
                                   │ calls
                                   ▼
┌──────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐
│   Expense.java   │◄─┤     ExpenseDAO.java     │─►│    DBConnection.java    │
│  (Entity Model)  │  │  (Data Access Layer)    │  │  (Connection Factory)   │
└──────────────────┘  └────────────┬────────────┘  └────────────┬────────────┘
                                   │ JDBC                       │
                                   ▼                            ▼
                      ┌──────────────────────────────────────────────┐
                      │        MySQL (Expense_Tracker.expenses)      │
                      └──────────────────────────────────────────────┘
```

* **`Requirements.java`**: Handles console menus, user input, input validation retries, and delegates tasks to the service layer. Contains zero SQL.
* **`ExpenseService.java`**: Coordinates business logic and acts as an abstraction between the UI and data layer.
* **`ExpenseDAO.java`**: Manages all database CRUD operations and executes parameterized queries (`PreparedStatement`, `ResultSet`, `try-with-resources`).
* **`DBConnection.java`**: Centralized connection provider for MySQL reading credentials securely from environment variables.
* **`Expense.java`**: Plain Java entity representing the Expense model.

---

## Database Setup

1. **Create the Database and Table:**
   ```sql
   CREATE DATABASE Expense_Tracker;
   USE Expense_Tracker;

   CREATE TABLE expenses (
       Id INT PRIMARY KEY,
       Amount DECIMAL(10,2) NOT NULL,
       Category VARCHAR(50) NOT NULL,
       Description VARCHAR(50) NOT NULL,
       date DATE NOT NULL
   );
   ```

2. **Configure Database Credentials:**
   Set the `DB_PASSWORD` environment variable on your machine before running:
   * **PowerShell (current session):** `$env:DB_PASSWORD="your_password"`
   * **PowerShell (permanent):** `[System.Environment]::SetEnvironmentVariable('DB_PASSWORD', 'your_password', 'User')`

---

## Build & Run Instructions

Ensure **Java 17+** and **Maven** are installed on your machine.

### 1. Compile the Project
```bash
mvn clean compile
```

### 2. Run Tests
```bash
mvn test
```

### 3. Package into a JAR
```bash
mvn clean package
```

### 4. Run the Application via Maven
```bash
mvn exec:java
```

---

## Jenkins CI Pipeline Setup

This repository includes a `Jenkinsfile` for automated Continuous Integration (CI).

### Prerequisites on Jenkins:
1. Jenkins installed and running.
2. Java 17 (JDK) and Maven configured under **Manage Jenkins** $\rightarrow$ **Tools**.
3. Git Plugin and Pipeline Plugin installed.

### Steps to Run the Pipeline:
1. Open Jenkins and click **New Item**.
2. Enter a project name (e.g. `expense-tracker-ci`) and select **Pipeline**, then click **OK**.
3. Under the **Pipeline** configuration section:
   * **Definition:** Select `Pipeline script from SCM`
   * **SCM:** Select `Git`
   * **Repository URL:** `https://github.com/Sruthitattikayala/expense-tracker.git`
   * **Branch Specifier:** `*/main`
   * **Script Path:** `Jenkinsfile`
4. Click **Save**.
5. Click **Build Now** to trigger the build.
6. When the build succeeds, download the generated artifact (`expense-tracker-1.0-SNAPSHOT.jar`) directly from the build summary page.

---

## Project Structure

```text
Project/
├── pom.xml
├── Jenkinsfile
├── README.md
├── .gitignore
├── src/
│   └── main/
│       ├── java/
│       │   ├── DBConnection.java
│       │   ├── Expense.java
│       │   ├── ExpenseDAO.java
│       │   ├── ExpenseService.java
│       │   └── Requirements.java
│       └── resources/
└── target/
```
