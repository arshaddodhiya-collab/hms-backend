# Admin Module Documentation

Comprehensive documentation for the Hospital Management System (HMS) Admin Module, covering Department and User management.

## 📚 Documentation Index

1. **[Admin Module Overview](./01_ADMIN_MODULE_OVERVIEW.md)**
   - Core components and architecture
   - Entities, DTOs, Services, and Controllers
   - Directory structure
   - Key features overview

2. **[Department Service & Endpoints](./02_DEPARTMENT_SERVICE_AND_ENDPOINTS.md)**
   - DepartmentService business logic
   - CRUD operations and validation
   - REST API endpoints
   - Request/response examples
   - Business rules

3. **[User Service & Endpoints](./03_USER_SERVICE_AND_ENDPOINTS.md)**
   - UserService implementation
   - Password handling and security
   - User creation and updates
   - Department and role assignment
   - Integration with authentication module

4. **[Database Schema](./04_DATABASE_SCHEMA.md)**
   - Table structures and relationships
   - Entity relationships diagram
   - Common SQL queries
   - JPA/Hibernate queries
   - Migration scripts
   - Performance considerations

5. **[API Testing Guide](./05_API_TESTING_GUIDE.md)**
   - Authentication prerequisites
   - Department API test cases
   - User API test cases
   - Integration test scenarios
   - Error response examples
   - Postman collection structure

6. **[Security & Permissions](./06_SECURITY_AND_PERMISSIONS.md)**
   - Security architecture
   - Permission definitions
   - Role-based access control
   - Endpoint security annotations
   - Best practices
   - Production recommendations

## 🚀 Quick Start

### Prerequisites
- Java 17+
- MySQL 8.0+
- Maven 3.6+
- Valid JWT token (obtain via `/api/v1/auth/login`)

### Base URL
```
http://localhost:8080/api/v1
```

### Authentication
All Admin endpoints require authentication:
```bash
Authorization: Bearer <your-jwt-token>
```

### Required Permissions
- **Department Management**: `CMP_ADMIN_DEPT_READ`, `CMP_ADMIN_DEPT_WRITE`, `CMP_ADMIN_DEPT_DELETE`
- **User Management**: `CMP_ADMIN_USER_READ`, `CMP_ADMIN_USER_WRITE`

## 📋 API Endpoints Summary

### Departments
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | `/departments` | `CMP_ADMIN_DEPT_READ` | List all departments |
| GET | `/departments/{id}` | `CMP_ADMIN_DEPT_READ` | Get department by ID |
| POST | `/departments` | `CMP_ADMIN_DEPT_WRITE` | Create department |
| PUT | `/departments/{id}` | `CMP_ADMIN_DEPT_WRITE` | Update department |
| DELETE | `/departments/{id}` | `CMP_ADMIN_DEPT_DELETE` | Delete department |

### Users
| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| GET | `/users` | `CMP_ADMIN_USER_READ` | List all users |
| GET | `/users/{id}` | `CMP_ADMIN_USER_READ` | Get user by ID |
| POST | `/users` | `CMP_ADMIN_USER_WRITE` | Create user |
| PUT | `/users/{id}` | `CMP_ADMIN_USER_WRITE` | Update user |

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Client (Angular)                      │
│                  JWT Token in Header                     │
└────────────────────┬────────────────────────────────────┘
                     │ HTTPS
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Spring Security Filter Chain                │
│  ┌──────────────────────────────────────────────────┐  │
│  │        JwtAuthenticationFilter                    │  │
│  │  - Validates JWT                                  │  │
│  │  - Sets SecurityContext                           │  │
│  └──────────────────────────────────────────────────┘  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                    Controllers                           │
│  ┌──────────────────┐      ┌──────────────────┐        │
│  │ DepartmentCtrl   │      │   UserController  │        │
│  │ @PreAuthorize    │      │   @PreAuthorize   │        │
│  └────────┬─────────┘      └────────┬──────────┘        │
└───────────┼──────────────────────────┼──────────────────┘
            │                          │
            ▼                          ▼
┌─────────────────────────────────────────────────────────┐
│                     Services                             │
│  ┌──────────────────┐      ┌──────────────────┐        │
│  │ DepartmentSvc    │      │   UserService     │        │
│  │ - Business Logic │      │   - Password Enc  │        │
│  │ - Validation     │      │   - Role Mgmt     │        │
│  └────────┬─────────┘      └────────┬──────────┘        │
└───────────┼──────────────────────────┼──────────────────┘
            │                          │
            ▼                          ▼
┌─────────────────────────────────────────────────────────┐
│                   Repositories                           │
│  ┌──────────────────┐      ┌──────────────────┐        │
│  │ DepartmentRepo   │      │   UserRepository  │        │
│  │ (JPA)            │      │   (JPA)           │        │
│  └────────┬─────────┘      └────────┬──────────┘        │
└───────────┼──────────────────────────┼──────────────────┘
            │                          │
            ▼                          ▼
┌─────────────────────────────────────────────────────────┐
│                  MySQL Database                          │
│  ┌──────────────────┐      ┌──────────────────┐        │
│  │   departments    │      │      users        │        │
│  │   - id           │◄─────┤   - id            │        │
│  │   - name         │      │   - username      │        │
│  │   - hod_id       ├─────►│   - department_id │        │
│  └──────────────────┘      └──────────────────┘        │
└─────────────────────────────────────────────────────────┘
```

## 🔑 Key Features

### Department Management
- ✅ Create, read, update, delete departments
- ✅ Assign head of department (any user)
- ✅ Track staff count automatically
- ✅ Prevent deletion of departments with staff
- ✅ Standard departments seeded on initialization

### User Management
- ✅ Create and update users
- ✅ BCrypt password encryption
- ✅ Department assignment
- ✅ Multiple role support
- ✅ Active/inactive status management
- ✅ Integration with authentication module

### Security
- ✅ JWT-based authentication
- ✅ Fine-grained permissions
- ✅ Role-based access control
- ✅ Method-level security with @PreAuthorize
- ✅ Global exception handling
- ✅ CORS configuration

## 📦 Database Schema

### Core Tables
- `departments`: Department information
- `users`: User accounts and profiles
- `user_roles`: Many-to-many user-role mapping
- `roles`: Role definitions
- `permissions`: Permission definitions
- `role_permissions`: Many-to-many role-permission mapping

### Relationships
- Department → Users (One-to-Many): Staff members
- User → Department (Many-to-One): Head of department
- User ↔ Role (Many-to-Many): User role assignments
- Role ↔ Permission (Many-to-Many): Role permissions

## 🧪 Testing

### Quick Test with cURL

**Login**:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**Get Departments**:
```bash
curl -X GET http://localhost:8080/api/v1/departments \
  -H "Authorization: Bearer <your-token>"
```

**Create Department**:
```bash
curl -X POST http://localhost:8080/api/v1/departments \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Oncology",
    "description": "Cancer treatment",
    "active": true
  }'
```

For comprehensive testing examples, see [API Testing Guide](./05_API_TESTING_GUIDE.md).

## 🛠️ Development

### Project Structure
```
src/main/java/com/hms/HospitalManagementSystem/
├── controller/
│   ├── DepartmentController.java
│   └── UserController.java
├── dto/
│   ├── DepartmentDto.java
│   └── UserDto.java
├── entity/
│   ├── Department.java
│   └── User.java
├── repository/
│   ├── DepartmentRepository.java
│   └── UserRepository.java
├── service/
│   ├── DepartmentService.java
│   └── UserService.java
└── exception/
    └── GlobalExceptionHandler.java
```

### Running the Application
```bash
# Start MySQL
mysql -u root -p

# Run Spring Boot
./mvnw spring-boot:run

# Application runs on http://localhost:8080
```

## 📖 Related Documentation

- [Authentication Flow](../auth_docs/02_AUTHENTICATION_FLOW.md)
- [JWT Implementation](../auth_docs/04_JWT_IMPLEMENTATION.md)
- [Database Migration Guide](../DATABASE_MIGRATION_GUIDE.md)
- [Backend Best Practices](../BEST_PRACTICES_BACKEND.md)

## 🤝 Contributing

When extending the Admin module:
1. Follow existing patterns for DTOs, Services, and Controllers
2. Add appropriate `@PreAuthorize` annotations
3. Update documentation
4. Add test cases
5. Update migration scripts if schema changes

## 📝 License

Part of the Hospital Management System (HMS) project.

---

**Last Updated**: February 2026  
**Version**: 1.0  
**Maintained By**: HMS Development Team
