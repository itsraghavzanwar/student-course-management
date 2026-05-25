# EduTrack – Student & Course Management System

A full-stack application built with **Java Spring Boot**, **MySQL**, and **React** featuring JWT-based Role-Based Access Control (RBAC), stored procedures, and transaction management.

---

## Tech Stack

| Layer      | Technology                          |
|------------|-------------------------------------|
| Backend    | Java 17, Spring Boot 3.2            |
| Security   | Spring Security + JWT (JJWT)        |
| Database   | MySQL 8.x                           |
| ORM        | Spring Data JPA / Hibernate         |
| Docs       | SpringDoc OpenAPI (Swagger UI)      |
| Frontend   | React 18 + Vite                     |
| API Client | Axios                               |

---

## Database Design

### Tables

#### `courses`
| Column          | Type         | Constraint       |
|-----------------|--------------|------------------|
| course_id       | BIGINT       | PK, AUTO_INCREMENT |
| course_name     | VARCHAR(150) | NOT NULL         |
| course_code     | VARCHAR(20)  | UNIQUE, NOT NULL |
| course_duration | INT          | NOT NULL (weeks) |
| description     | TEXT         |                  |
| created_at      | TIMESTAMP    | DEFAULT NOW()    |
| updated_at      | TIMESTAMP    | ON UPDATE NOW()  |

#### `students`
| Column        | Type         | Constraint                   |
|---------------|--------------|------------------------------|
| student_id    | BIGINT       | PK, AUTO_INCREMENT           |
| first_name    | VARCHAR(80)  | NOT NULL                     |
| last_name     | VARCHAR(80)  | NOT NULL                     |
| email         | VARCHAR(100) | UNIQUE, NOT NULL             |
| phone         | VARCHAR(20)  |                              |
| date_of_birth | DATE         |                              |
| course_id     | BIGINT       | FK → courses(course_id) SET NULL |
| user_id       | BIGINT       | FK → users(id) SET NULL      |

#### `users` + `roles` + `user_roles`
Standard JWT auth tables with many-to-many role assignment.

---

### Stored Procedures

| Procedure          | Purpose                              |
|--------------------|--------------------------------------|
| `sp_insert_student`| Inserts student with transaction + course validation |
| `sp_update_student`| Updates student/course with transaction              |
| `sp_delete_student`| Deletes with optional force flag for enrolled check  |

Each procedure uses `START TRANSACTION` / `COMMIT` / `ROLLBACK` for data integrity.

---

## API Endpoints

### Auth
| Method | Endpoint           | Access  | Description        |
|--------|--------------------|---------|--------------------|
| POST   | /api/auth/register | Public  | Register user      |
| POST   | /api/auth/login    | Public  | Login, returns JWT |

### Courses
| Method | Endpoint         | Access  | Description         |
|--------|------------------|---------|---------------------|
| GET    | /api/courses     | Auth    | List all courses    |
| GET    | /api/courses/:id | Auth    | Get course by ID    |
| POST   | /api/courses     | Admin   | Create course       |
| PUT    | /api/courses/:id | Admin   | Update course       |
| DELETE | /api/courses/:id | Admin   | Delete course       |

### Students
| Method | Endpoint                      | Access  | Description                    |
|--------|-------------------------------|---------|--------------------------------|
| GET    | /api/students                 | Admin   | List all students              |
| GET    | /api/students/:id             | Admin   | Get student by ID              |
| GET    | /api/students/me              | Auth    | Get own profile                |
| GET    | /api/students/course/:id      | Admin   | Students in a course           |
| POST   | /api/students                 | Admin   | Create student (via stored proc)|
| PUT    | /api/students/:id             | Admin   | Update student (via stored proc)|
| DELETE | /api/students/:id?force=bool  | Admin   | Delete student                 |

---

## Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.x
- Node.js 18+ & npm

### 1. Database Setup

```bash
mysql -u root -p < database/schema.sql
```

This creates the `scm_db` database, all tables, stored procedures, and seed courses.

### 2. Backend Configuration

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/scm_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
app.jwt.secret=YOUR_SECRET_KEY_AT_LEAST_32_CHARS
```

### 3. Run Backend

```bash
cd backend
mvn spring-boot:run
```

Backend starts at: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 4. Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts at: `http://localhost:3000`

---

## Creating Your First Admin

Register via the API:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@edutrack.com",
    "password": "Admin@123",
    "roles": ["admin"]
  }'
```

Then login to get your JWT token:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "Admin@123"}'
```

---

## Testing with Postman

1. Import the base URL: `http://localhost:8080`
2. Add header `Authorization: Bearer <your_jwt_token>` to protected routes
3. Or use Swagger UI at `/swagger-ui.html` which has built-in auth

### Sample: Create a Student

```json
POST /api/students
Authorization: Bearer <admin_token>

{
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane@example.com",
  "phone": "+91 9876543210",
  "dateOfBirth": "2000-05-15",
  "courseId": 2
}
```

### Sample: Delete Student (restrict if enrolled)

```
DELETE /api/students/1?force=false    ← fails if enrolled
DELETE /api/students/1?force=true     ← force deletes
```
