# Expense Tracker (Console Application)

A robust, console-based personal expense management application developed in Java and backed by MySQL. Built with a clean 4-tier layered architecture, this application demonstrates object-oriented programming (OOP), the Data Access Object (DAO) pattern, secure parameterized SQL using JDBC, and automated project lifecycle management via Maven.

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
* **`DBConnection.java`**: Centralized connection provider for MySQL.
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
   * Open `src/main/java/DBConnection.java`.
   * Set your local MySQL `URL`, `USER`, and `PASSWORD` before running.
   *(Note: Never commit real database passwords to a public repository).*

---

## Build & Run Instructions

Ensure **Java 17+** and **Maven** are installed on your machine.

### 1. Compile the Project
```bash
mvn clean compile
```

### 2. Package into a JAR
```bash
mvn clean package
```

### 3. Run the Application via Maven
```bash
mvn exec:java
```

---

## Project Structure

```text
Project/
├── pom.xml
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
