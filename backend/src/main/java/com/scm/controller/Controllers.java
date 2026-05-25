package com.scm.controller;

import com.scm.dto.*;
import com.scm.service.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register and login endpoints")
class AuthController {

    private final AuthService authService;

    AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody AuthDTOs.RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("User registered successfully.", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDTOs.JwtResponse>> login(
            @Valid @RequestBody AuthDTOs.LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful.", authService.login(request)));
    }
}

@RestController
@RequestMapping("/api/courses")
@Tag(name = "Courses", description = "Course management")
@SecurityRequirement(name = "bearerAuth")
class CourseController {

    private final CourseService courseService;

    CourseController(CourseService courseService) { this.courseService = courseService; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseDTOs.CourseResponse>>> getAllCourses() {
        return ResponseEntity.ok(ApiResponse.ok("Courses fetched.", courseService.getAllCourses()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseDTOs.CourseResponse>> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Course fetched.", courseService.getCourseById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseDTOs.CourseResponse>> createCourse(
            @Valid @RequestBody CourseDTOs.CourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Course created.", courseService.createCourse(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CourseDTOs.CourseResponse>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseDTOs.CourseRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Course updated.", courseService.updateCourse(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.ok("Course deleted.", null));
    }
}

@RestController
@RequestMapping("/api/students")
@Tag(name = "Students", description = "Student management")
@SecurityRequirement(name = "bearerAuth")
class StudentController {

    private final StudentService studentService;

    StudentController(StudentService studentService) { this.studentService = studentService; }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentDTOs.StudentResponse>>> getAllStudents() {
        return ResponseEntity.ok(ApiResponse.ok("Students fetched.", studentService.getAllStudents()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StudentDTOs.StudentResponse>> getStudent(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Student fetched.", studentService.getStudentById(id)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    public ResponseEntity<ApiResponse<StudentDTOs.StudentResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.ok("Profile fetched.", studentService.getMyProfile()));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentDTOs.StudentResponse>>> getStudentsByCourse(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Students fetched.", studentService.getStudentsByCourse(courseId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StudentDTOs.StudentResponse>> createStudent(
            @Valid @RequestBody StudentDTOs.StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Student created.", studentService.createStudent(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StudentDTOs.StudentResponse>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentDTOs.StudentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Student updated.", studentService.updateStudent(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {
        studentService.deleteStudent(id, force);
        return ResponseEntity.ok(ApiResponse.ok("Student deleted.", null));
    }
}
