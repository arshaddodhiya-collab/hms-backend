# MySQL Setup for Hospital Management System

This guide explains how to set up the MySQL database for this project.

## Prerequisites

1.  **Install MySQL Server**:
    *   **Linux (Ubuntu/Debian)**:
        ```bash
        sudo apt update
        sudo apt install mysql-server
        ```
    *   **Windows/Mac**: Download and install from [MySQL Community Downloads](https://dev.mysql.com/downloads/mysql/).

2.  **Start MySQL Service**:
    *   **Linux**: `sudo systemctl start mysql`
    *   **Windows**: Ensure the MySQL service is running via Services.msc.

## Configuration

The project is configured to connect to a MySQL database named `hospital_db` on `localhost:3306`.

### Default Credentials
The `src/main/resources/application.properties` file uses the following default credentials:

*   **Username**: `root`
*   **Password**: `root`

**IMPORTANT**: If your local MySQL installation uses a different password (or no password), you **MUST** update `src/main/resources/application.properties` to match your credentials.

```properties
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

## Database Creation

The application is configured with `createDatabaseIfNotExist=true` in the connection URL. This means:
1.  You do **not** need to manually create the `hospital_db` database.
2.  The application will attempt to create it automatically when it starts.

However, if you prefer to create it manually, log in to MySQL and run:
```sql
CREATE DATABASE hospital_db;
```

## Running the Application

1.  Open a terminal in the project root.
2.  Run the application using Maven:
    ```bash
    ./mvnw spring-boot:run
    ```
3.  The application will start and connect to the database. Hibernate will automatically create the necessary tables (`ddl-auto=update`).

## Troubleshooting

*   **Access Denied**: If you see `Access denied for user 'root'@'localhost'`, verify your username and password in `application.properties`.
*   **Connection Refused**: Ensure MySQL is running and listening on port 3306.
*   **Driver Missing**: If you see errors about missing drivers, run `./mvnw clean install` to download dependencies.
