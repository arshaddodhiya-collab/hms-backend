# Backend Debugging Guide

This guide provides a comprehensive approach to debugging the **Hospital Management System (HMS) Backend**. It covers logging, database inspection, security troubleshooting, and common error resolutions.

## 1. Application Logs

Logs are your first line of defense. The application uses **SLF4J with Logback**.

### 1.1 Log Configuration
*   **File**: `src/main/resources/application.properties`
*   **Key Settings**:
    ```properties
    # General Logging Level
    logging.level.root=INFO
    
    # Application Specific Logging (Set to DEBUG to see more)
    logging.level.com.hms=DEBUG
    
    # Spring Security Logging (Crucial for Auth issues)
    logging.level.org.springframework.security=DEBUG
    logging.level.org.springframework.web=DEBUG
    
    # SQL Query Logging (See actual SQL executed)
    logging.level.org.hibernate.SQL=DEBUG
    logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
    ```

### 1.2 Viewing Logs
*   **Console**: If running via `./mvnw spring-boot:run`, logs appear in the terminal.
*   **File**: If configured (e.g., `logging.file.name=app.log`), verify the file content using `tail -f app.log`.

### 1.3 Interpreting Common Logs
*   `o.s.s.web.DefaultSecurityFilterChain`: Shows which security filter chain is matching the request.
*   `o.s.s.w.a.AnonymousAuthenticationFilter`: Indicates the user is not logged in (Anonymous).
*   `o.h.engine.jdbc.spi.SqlExceptionHelper`: Database errors (e.g., constraint violations).

## 2. Database Inspection (PostgreSQL)

### 2.1 Connection Details
*   **Host**: `localhost` (or from `application.properties`)
*   **Port**: `5432`
*   **Database**: `hms_db` (or as configured)
*   **Username/Password**: Defined in `application.properties`.

### 2.2 Common SQL Queries for Debugging
Use these in a SQL client (e.g., DBeaver, pgAdmin) or `psql`:

**Check User Existence & Roles**:
```sql
SELECT u.id, u.username, u.active, r.name as role 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
JOIN roles r ON ur.role_id = r.id;
```

**Check Permissions for a Role**:
```sql
SELECT r.name as role, p.code as permission 
FROM roles r 
JOIN role_permissions rp ON r.id = rp.role_id 
JOIN permissions p ON rp.permission_id = p.id 
WHERE r.name = 'DOCTOR';
```

**Check Refresh Tokens**:
```sql
SELECT * FROM refresh_tokens;
```

## 3. Security & Authentication Debugging

### 3.1 The "401 Unauthorized" Error
*   **Cause**: Invalid credentials, missing cookie, or expired token.
*   **Tools**: Browser DevTools (Network Tab), Postman.
*   **Checklist**:
    1.  **Cookies**: Are `accessToken` and `refreshToken` cookies being sent? (Check "Cookies" tab in request).
    2.  **CORS**: Is the browser blocking the request? (Look for CORS error in Console).
    3.  **Token Validity**: Decode the JWT (from cookie value) using [jwt.io](https://jwt.io) to check `exp` (expiration) and `role`.

### 3.2 The "403 Forbidden" Error
*   **Cause**: User is logged in but lacks Permission/Role.
*   **Checklist**:
    1.  **Log Check**: Look for `AccessDeniedException` in logs.
    2.  **Verify Permissions**: Use the SQL query above to confirm the user's role has the required permission.
    3.  **Controller annotations**: Check `@PreAuthorize("hasAuthority('PERM_CODE')")` on the endpoint.

## 4. Common Issues & Fixes

### 4.1 "Unknown Error" / Network Error in Frontend
*   **Cause**: Typically CORS (Cross-Origin Resource Sharing).
*   **Fix**: Ensure `SecurityConfig.java` allows the frontend origin:
    ```java
    configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
    ```

### 4.2 Database "Relation does not exist"
*   **Cause**: Tables not created.
*   **Fix**: Check `spring.jpa.hibernate.ddl-auto=update` in `application.properties`.

### 4.3 Users/Roles Missing
*   **Cause**: `DataInitializer` didn't run or failed.
*   **Fix**: Check logs for startup errors. Verify `DataInitializer` defines the missing user.

## 5. Testing with CURL
Isolate backend issues by bypassing the frontend.

**Login (Sets Cookies)**:
```bash
curl -v -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
-H "Content-Type: application/json" \
-d '{"username": "admin", "password": "admin123"}'
```

**Access Protected Endpoint (Uses Cookies)**:
```bash
curl -v -b cookies.txt http://localhost:8080/api/v1/auth/me
```

## 6. Remote Debugging (IDE)
To attach a debugger (IntelliJ/Eclipse) to the running app:
1.  Stop the current instance.
2.  Run with debug agent:
    ```bash
    ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
    ```
3.  In IDE, create a **Remote JVM Debug** configuration on port `5005`.
4.  Set breakpoints and debug.
