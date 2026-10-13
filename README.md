# 💰 Personal Expense Tracker & Multilingual Financial Assistant

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg?logo=openjdk)](https://openjdk.org/projects/jdk/17/)
[![MySQL 8.0](https://img.shields.io/badge/MySQL-8.0-blue.svg?logo=mysql)](https://www.mysql.com/)
[![Maven 3](https://img.shields.io/badge/Maven-3.x-red.svg?logo=apachemaven)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Supported-2496ED.svg?logo=docker)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Manifests-326CE5.svg?logo=kubernetes)](https://kubernetes.io/)
[![Jenkins CI/CD](https://img.shields.io/badge/Jenkins-CI%2FCD-D24939.svg?logo=jenkins)](https://www.jenkins.io/)

A full-stack personal finance and expense management application built with **Java 17**, **MySQL 8.0**, **JDBC**, **Maven**, and a responsive **HTML5/CSS3/Vanilla JavaScript** web interface.

The application combines financial bookkeeping (expense tracking, category analytics, Month-over-Month comparisons) with an intelligent **selectable monthly and weekly budget engine** and an **AI-assisted multilingual natural language expense parser** supporting English, Telugu, and Tanglish with Web Speech API voice input.

---

## 📌 Table of Contents

- [Real-World Problem & Solution](#-real-world-problem--solution)
- [Key Features](#-key-features)
- [Multilingual AI & Voice Assistant](#-multilingual-ai--voice-assistant)
- [Selectable Monthly & Weekly Budgeting](#-selectable-monthly--weekly-budgeting)
- [System Architecture](#-system-architecture)
- [Technology Stack](#-technology-stack)
- [REST API Reference](#-rest-api-reference)
- [Database Schema](#-database-schema)
- [Getting Started & Local Setup](#-getting-started--local-setup)
- [Containerization & CI/CD Pipeline](#-containerization--cicd-pipeline)
- [Screenshots & UI Guide](#-screenshots--ui-guide)

---

## 🎯 Real-World Problem & Solution

### The Problem
* **Friction in Manual Logging:** Traditional expense trackers force users through tedious, multi-input forms for every cup of coffee or grocery purchase, leading to user fatigue and abandoned tracking.
* **Rigid Budget Periods:** Monthly-only budgeting fails to catch fast-spending behavior early in the week. Users often realize they overspent only when the month is nearly over.
* **Language Barriers:** In multilingual regions, users frequently think and speak in a combination of regional languages and English (e.g., Telugu and Tanglish), making English-only forms feel unnatural.

### The Solution
* **Zero-Friction AI Parsing:** Speak or type naturally in English, Telugu, or Tanglish (*"Spent 2500 on groceries yesterday"* or *"ఈరోజు కూరగాయలకు 500 ఖర్చు చేశాను"*). The rule-based semantic NLP engine automatically resolves amounts, multipliers (`k`, `వేలు`, `lakh`), categories, dates, and intent.
* **Dual Monthly & Weekly Budget Tracking:** Track spending against both the current/future calendar months and ISO-8601 calendar weeks with visual pacing bars and real-time alerts.
* **Zero Heavy Framework Bloat:** Runs on Java's built-in lightweight `HttpServer` with zero Spring overhead, near-instant sub-second startup times, and pure vanilla frontend performance.

---

## ✨ Key Features

### 1. Modern Web Dashboard & Analytics
- **Live Summary Cards:** Total spending, transaction count, active category count, and top spending category.
- **Spending Distribution:** Visual category percentage breakdowns and insights.
- **Month-over-Month Comparison Engine:** Side-by-side comparison between the current month and the previous month, complete with net change amounts and percentage differences.

### 2. Full Expense CRUD Operations
- **Add / Update / Delete:** Fast modal and inline forms with validation for positive amounts, valid dates, and required fields.
- **Real-Time Search & Filtering:** Filter expenses instantly by category, description, or date directly on the client side.

### 3. Automated In-App Notifications & Alerts
- **Threshold Triggers:**
  - `ℹ️ Info`: Unset budget notice when a new month begins.
  - `ℹ️ Info`: 50% of budget reached.
  - `⚠️ Warning`: 80% of budget reached (*"Consider reducing spending"*).
  - `🚨 Alert`: 100% budget reached (*"Limit reached"*).
  - `🚨 Alert`: Budget exceeded by ₹X (*"Over budget by ₹X"*).
- **Pacing Alerts:** Weekly spending pace notices and category threshold alerts.
- **Notification Center:** Dedicated notification view with unread badges, read status toggling, and "Mark All as Read".

---

## 🎙️ Multilingual AI & Voice Assistant

The application includes a built-in semantic NLP engine in `AiParserService.java` that processes natural language and voice input:

### Supported Languages
1. **English:** *"Paid 1200 for electricity bill today"*
2. **Telugu (తెలుగు):** *"ఈరోజు కిరాణా సరుకులకు 2500 ఖర్చు చేశాను"*
3. **Tanglish (Telugu written in Roman English):** *"nanna lunch kosam 300 rupees spend chesanu"*

### Capabilities
* **Intent Detection:** Automatically distinguishes between an `EXPENSE` transaction and a `BUDGET` allocation plan (e.g., *"My salary is 45000 plan budget for rent and food"*).
* **Number & Multiplier Normalization:** Handles digits (`1500`), commas (`15,000`), abbreviations (`15k`), and Telugu number words (`15 వేలు` $\rightarrow$ 15,000, `2 లక్ష` $\rightarrow$ 200,000).
* **Relative Date Resolution:** Automatically maps *"today"* / *"ఈరోజు"*, *"yesterday"* / *"నిన్న"* ($-1$ day), and *"day before yesterday"* / *"మొన్న"* ($-2$ days) to exact ISO dates.
* **Category Classification:** Classifies text into Food, Rent, Education, Gym, Savings, Transportation, Utilities, Shopping, Healthcare, or Entertainment.
* **Interactive Confirmation Card:** Parsed results are displayed in a confirmation card before writing to MySQL, allowing the user to review, edit, or apply with one click.
* **Extensible Architecture:** Features hooks to easily connect external LLM API endpoints (`AI_API_URL`, `AI_API_KEY`) with automatic local fallback.

---

## 📊 Selectable Monthly & Weekly Budgeting

### 1. Monthly Budget Selector
- **Period Selection:** Select the current calendar month or any of the next 11 future months (`YYYY-MM`).
- **Independent Persistence:** Load, save, and update each month's budget independently in MySQL without overwriting other months.
- **Graceful Unset State:** When no budget is set for a month, the UI clearly displays `"No budget set"`, keeps the input field empty, and shows `—` for remaining balance and usage percentage.
- **Proportion Guidelines:** Enter monthly salary to calculate recommended 50/30/20 guideline allocations across custom category chips.

### 2. Weekly Budget Selector
- **ISO-8601 Support:** Select the current week or any of the next 15 future weeks (`YYYY-Www`).
- **Date Range Formatting:** Clearly displays the week number, year, and formatted Monday-to-Sunday date range (e.g., `Week 41, 2026 (05 Oct - 11 Oct, 2026)`).
- **Independent Persistence:** Saved independently in MySQL (`weekly_budget` table) with immediate visual feedback and dynamic progress bars.

---

## 🏗️ System Architecture

The project follows a clean **4-Tier Layered Architecture** with strict separation of concerns:

```
┌────────────────────────────────────────────────────────────────────────┐
│                         Presentation Layer                             │
│   Web UI (HTML5, CSS3, Vanilla JS)  │  Interactive CLI (Requirements)  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ HTTP / REST (Port 8080)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        API & Controller Layer                          │
│   StaticFileHandler  │ ExpenseApiHandler  │ BudgetApiHandler           │
│   NotificationApiHandler                  │ AiApiHandler               │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Business Calls
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         Service & Domain Layer                         │
│           ExpenseService           │         AiParserService           │
│  (Budget Engine, Calculations, MoM)│  (Multilingual NLP, Intent Engine)│
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Data Access
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        Data Access Layer (DAO)                         │
│               ExpenseDAO          │         DBConnection               │
│      (Parameterized JDBC Queries) │  (Secure Credential Resolution)    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ JDBC Driver (Port 3306)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           Database Layer                               │
│              MySQL 8.0 Database (Expense_Tracker)                      │
│   [expenses]  [monthly_budget]  [weekly_budget]  [in_app_notifications]│
└────────────────────────────────────────────────────────────────────────┘
```

---

## 💻 Technology Stack

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Backend** | Java 17 (LTS) | Built with standard Java, `java.net.HttpServer`, `java.time` |
| **Persistence** | JDBC 4.2 | Parameterized `PreparedStatement`, transaction handling |
| **Database** | MySQL 8.0 | InnoDB, `ON DUPLICATE KEY UPDATE`, relational constraints |
| **JSON Serialization** | Google Gson 2.11.0 | Lightweight request/response JSON mapping |
| **Frontend** | Vanilla HTML5 / CSS3 / ES6+ | Zero-dependency, modern CSS Grid/Flexbox, Web Speech API |
| **Build & Tooling** | Apache Maven 3.x | Build lifecycle, packaging, dependency management |
| **Containerization** | Docker | Runtime container based on `eclipse-temurin:17-jre` |
| **Orchestration** | Kubernetes | Deployment and Secret manifests (`k8s/deployment.yaml`) |
| **CI/CD** | Jenkins | Declarative Pipeline (`Jenkinsfile`) |

---

## 📡 REST API Reference

### Expenses API (`/api/expenses`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/expenses` | Retrieve all expenses |
| `POST` | `/api/expenses` | Create a new expense |
| `PUT` | `/api/expenses/{id}` | Update an existing expense by ID |
| `DELETE` | `/api/expenses/{id}` | Delete an expense by ID |
| `GET` | `/api/expenses/total` | Get overall total spending |
| `GET` | `/api/expenses/category-summary` | Get spending aggregated by category |
| `GET` | `/api/expenses/insights` | Get category percentage breakdown and top spending category |
| `GET` | `/api/expenses/monthly-comparison` | Month-over-Month comparison (`?current=YYYY-MM&previous=YYYY-MM`) |
| `GET` | `/api/expenses/weekly-pace` | Get weekly spending pace analysis for the month (`?month=YYYY-MM`) |

### Budget API (`/api/budget`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/budget/current?month=YYYY-MM` | Fetch monthly budget summary & spending for specified month |
| `POST` | `/api/budget/current` | Save/update monthly budget (`{"month": "YYYY-MM", "budget": 20000}`) |
| `DELETE` | `/api/budget/current?month=YYYY-MM` | Reset/delete monthly budget for specified month |
| `GET` | `/api/budget/weekly/current?week=YYYY-Www` | Fetch weekly budget summary & date range for specified ISO week |
| `POST` | `/api/budget/weekly/current` | Save/update weekly budget (`{"week": "YYYY-Www", "budget": 5000}`) |
| `DELETE` | `/api/budget/weekly/current?week=YYYY-Www` | Reset/delete weekly budget for specified ISO week |
| `GET` | `/api/budget/guidelines?month=YYYY-MM` | Fetch saved proportion guidelines |
| `POST` | `/api/budget/guidelines` | Compute and store proportion guidelines from salary |

### Notifications & AI Assistant
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/notifications` | Fetch all in-app notifications (triggers budget and pacing evaluation) |
| `POST` | `/api/notifications/{id}/read` | Mark a specific notification as read |
| `POST` | `/api/notifications/read-all` | Mark all notifications as read |
| `GET` | `/api/notifications/monthly?month=YYYY-MM` | Fetch monthly budget notifications for specified month |
| `POST` | `/api/notifications/monthly/evaluate?month=YYYY-MM` | Re-evaluate monthly budget alert thresholds |
| `POST` | `/api/ai/parse` | Parse natural language text into expense or budget intent |

---

## 🗄️ Database Schema

### Database Setup Script

Run the following SQL commands in MySQL to set up the database:

```sql
CREATE DATABASE IF NOT EXISTS Expense_Tracker;
USE Expense_Tracker;

-- 1. Expenses Table
CREATE TABLE IF NOT EXISTS expenses (
    Id INT AUTO_INCREMENT PRIMARY KEY,
    Amount DECIMAL(10,2) NOT NULL,
    Category VARCHAR(50) NOT NULL,
    Description VARCHAR(100) NOT NULL,
    date DATE NOT NULL
);

-- 2. Monthly Budget Table (automatically created by backend if not present)
CREATE TABLE IF NOT EXISTS monthly_budget (
    id INT AUTO_INCREMENT PRIMARY KEY,
    month_year VARCHAR(7) NOT NULL UNIQUE,     -- e.g. '2026-10'
    salary INT NOT NULL,
    allocations_json TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. Weekly Budget Table (automatically created by backend if not present)
CREATE TABLE IF NOT EXISTS weekly_budget (
    id INT AUTO_INCREMENT PRIMARY KEY,
    week_year VARCHAR(20) NOT NULL UNIQUE,     -- e.g. '2026-W41'
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    budget DECIMAL(10,2) NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. In-App Notifications Table (automatically created by backend if not present)
CREATE TABLE IF NOT EXISTS in_app_notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    notification_key VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(30) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Optional: Create dedicated application user
CREATE USER IF NOT EXISTS 'expense_app'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON Expense_Tracker.* TO 'expense_app'@'localhost';
FLUSH PRIVILEGES;
```

> **Note:** The application automatically verifies and initializes tables 2, 3, and 4 on startup via `expenseService.initDatabase()`.

---

## 🚀 Getting Started & Local Setup

### Prerequisites
- **Java Development Kit (JDK):** Version 17 or higher
- **Apache Maven:** Version 3.8+
- **MySQL Server:** Version 8.0+

### 1. Clone the Repository
```bash
git clone https://github.com/Sruthitattikayala/expense-tracker.git
cd expense-tracker
```

### 2. Configure Database Credentials
The application reads database connection settings securely from environment variables, with fallback to system properties.

**Windows PowerShell:**
```powershell
$env:DB_PASSWORD = "your_mysql_password"
# Optional (defaults to 'expense_app' and 'localhost'):
$env:DB_USER = "expense_app"
$env:DB_HOST = "localhost"
```

**Linux / macOS (Bash / Zsh):**
```bash
export DB_PASSWORD="your_mysql_password"
# Optional:
export DB_USER="expense_app"
export DB_HOST="localhost"
```

### 3. Build & Run

**Compile & Copy Static Resources:**
```powershell
mvn clean compile
```

**Run the Application:**
```powershell
mvn exec:java
```

**Run as Standalone Executable JAR:**
First build the JAR using Maven and configure MySQL credentials through environment variables as explained in Step 2:
```powershell
mvn clean package -DskipTests
java -jar target/expense-tracker-1.0-SNAPSHOT.jar
```

**Access the Web Interface:**
Open your browser and navigate to:
```
http://localhost:8080/
```

The web server starts on port `8080`, and the interactive terminal menu runs concurrently in the console.

---

## 🐳 Containerization & CI/CD Pipeline

### Docker Container
A `Dockerfile` is provided using the official `eclipse-temurin:17-jre` runtime:

```bash
# 1. Package the executable fat JAR
mvn clean package -DskipTests

# 2. Build the Docker image
docker build -t siri57/expense-tracker:1.2 .

# 3. Run the container, passing database credentials
docker run -d -p 8080:8080 \
  -e DB_HOST="host.docker.internal" \
  -e DB_USER="expense_app" \
  -e DB_PASSWORD="your_mysql_password" \
  --name expense-tracker-app \
  siri57/expense-tracker:1.2
```

### Kubernetes Deployment
Deployment manifests are located in the `k8s/` directory:

1. **Create the Secret for Database Password:**
   ```bash
   kubectl create secret generic expense-tracker-secret --from-literal=DB_PASSWORD="your_mysql_password"
   ```

2. **Apply the Deployment Manifest:**
   ```bash
   kubectl apply -f k8s/deployment.yaml
   ```

3. **Verify Deployment Status:**
   ```bash
   kubectl rollout status deployment/expense-tracker
   ```

### Jenkins CI/CD Pipeline
The included `Jenkinsfile` defines a declarative pipeline containing the following automated stages:
1. **Checkout:** Clones the latest source code from the Git repository.
2. **Compile:** Compiles Java classes using `mvn clean compile`.
3. **Test:** Runs unit and integration tests using `mvn test`.
4. **Package:** Packages the shaded executable JAR with `mvn package -DskipTests`.
5. **Archive Artifact:** Archives built JAR artifacts (`target/*.jar`) in Jenkins.
6. **Build Docker Image:** Builds the container image `siri57/expense-tracker:1.2`.
7. **Push Docker Image:** Authenticates and pushes the image to Docker Hub using Jenkins credentials (`docker-hub-credentials`).
8. **Deploy to Kubernetes:** Applies `k8s/deployment.yaml` and triggers a rollout restart on the Kubernetes cluster.

---

## 📸 Screenshots & UI Guide

> *To display live screenshots in your GitHub repository, place image files in a `docs/screenshots/` folder and link them below.*

| View | Description | Suggested Path |
| :--- | :--- | :--- |
| **Dashboard & Analytics** | Live summary cards, spending distribution, and Month-over-Month comparison | `docs/screenshots/dashboard.png` |
| **Selectable Budgeting** | Monthly and weekly budget selectors, progress bars, and unset state (`—`) | `docs/screenshots/budget.png` |
| **Multilingual Voice Assistant** | English, Telugu, and Tanglish natural language parsing with review cards | `docs/screenshots/assistant.png` |
| **Expense History & CRUD** | Search, filter, edit, and delete transactions with instant calculation updates | `docs/screenshots/history.png` |
| **In-App Notification Center** | Budget threshold alerts (50%, 80%, 100%, exceeded) and weekly pacing alerts | `docs/screenshots/notifications.png` |

---

## 👤 Author & Portfolio

**Sruthi Tattikayala**  
- GitHub: [@Sruthitattikayala](https://github.com/Sruthitattikayala)
- Project Repository: [expense-tracker](https://github.com/Sruthitattikayala/expense-tracker)
