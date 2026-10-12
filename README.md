# Personal Finance Tracker API

A REST API for personal finance management, built with Spring Boot. It allows users to track income and expenses, set monthly budgets, and generate financial reports.

## Features

- **Authentication & Authorization** — JWT-based login with email verification, and role/permission-based access control (USER/ADMIN roles with CREATE/VIEW authorities)
- **Transactions** — Record income and expenses, and browse history filtered by date range and category
- **Budgets** — Set per-category monthly budgets with validation against business rules
- **Categories** — Predefined and custom categories with icons, searchable by type
- **Dashboard** — Summary of income, expenses, and balance, with a category breakdown chart and recent transactions
- **Reports** — Category and monthly spending summaries, exportable as PDF or delivered by email
- **Notifications** — Configurable preferences for budget alerts, tips, and daily reminders

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot (Web, Data JPA, Security, Validation, Mail)
- **Database:** MySQL 8.0+
- **Auth:** JWT (jjwt)
- **Reporting:** OpenPDF (PDF export), JFreeChart (charts)
- **Testing:** JUnit 5, Mockito, MockMvc, H2
- **API Docs:** springdoc-openapi (Swagger UI)
- **Build Tool:** Maven, Docker Compose (MySQL)

## Project Structure

| Package | Responsibility |
|---|---|
| `controller` | Handles HTTP requests |
| `service` | Business logic |
| `repo` | Database interaction |
| `entity` | Database entities |
| `dto` | Data Transfer Objects |
| `security` | Security and JWT configuration |
| `exception` | Centralized error handling |

## Getting Started

### Prerequisites
- Java 17+
- Docker (for MySQL) — or a local MySQL 8.0+
- Maven (or use the bundled `./mvnw`)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/azerc18/personal-finance-tracker.git
   cd personal-finance-tracker
   ```

2. **Configure environment variables**

   Copy the sample file and fill in your own values (the app loads `.env` automatically):
   ```bash
   cp .env-sample .env
   ```
   | Variable | Description |
   |---|---|
   | `DB_USERNAME` | MySQL username (use `root` with the bundled Docker setup) |
   | `DB_PASSWORD` | MySQL password |
   | `JWT_SECRET` | Base64-encoded key (at least 32 bytes) used to sign JWT tokens |
   | `MAIL_USERNAME` | Gmail address used to send verification and report emails |
   | `MAIL_PASSWORD` | Gmail app password |

3. **Start MySQL**
   ```bash
   docker compose up -d
   ```
   This creates the `demo` database. If you use a local MySQL instead, run `CREATE DATABASE demo;`.

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```
   API docs (Swagger UI): http://localhost:8080/swagger-ui.html

   Demo account created by the data seeder: `user@gmail.com` / `user123`

## Running Tests

```bash
./mvnw test
```
Tests run against an in-memory H2 database, so no MySQL or `.env` is needed.
