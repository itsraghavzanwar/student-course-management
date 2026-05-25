package com.scm.service;

import com.scm.dto.*;
import com.scm.entity.*;
import com.scm.exception.*;
import com.scm.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final CourseRepository  courseRepository;
    private final UserRepository    userRepository;
    private final EntityManager     entityManager;

    public StudentService(StudentRepository studentRepository,
                          CourseRepository courseRepository,
                          UserRepository userRepository,
                          EntityManager entityManager) {
        this.studentRepository = studentRepository;
        this.courseRepository  = courseRepository;
        this.userRepository    = userRepository;
        this.entityManager     = entityManager;
    }

    public StudentDTOs.StudentResponse createStudent(StudentDTOs.StudentRequest request) {
        if (studentRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                "Student with email '" + request.getEmail() + "' already exists.");
        }

        // Validate course upfront for better error message
        if (request.getCourseId() != null) {
            courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Course not found with id: " + request.getCourseId()));
        }

        // Call stored procedure
        StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("sp_insert_student")
            .registerStoredProcedureParameter("p_first_name", String.class,  ParameterMode.IN)
            .registerStoredProcedureParameter("p_last_name",  String.class,  ParameterMode.IN)
            .registerStoredProcedureParameter("p_email",      String.class,  ParameterMode.IN)
            .registerStoredProcedureParameter("p_phone",      String.class,  ParameterMode.IN)
            .registerStoredProcedureParameter("p_dob",        java.sql.Date.class, ParameterMode.IN)
            .registerStoredProcedureParameter("p_course_id",  Long.class,    ParameterMode.IN)
            .registerStoredProcedureParameter("p_student_id", Long.class,    ParameterMode.OUT)
            .registerStoredProcedureParameter("p_message",    String.class,  ParameterMode.OUT)
            .setParameter("p_first_name", request.getFirstName())
            .setParameter("p_last_name",  request.getLastName())
            .setParameter("p_email",      request.getEmail())
            .setParameter("p_phone",      request.getPhone())
            .setParameter("p_dob",        request.getDateOfBirth() != null
                ? java.sql.Date.valueOf(request.getDateOfBirth()) : null)
            .setParameter("p_course_id",  request.getCourseId());

        query.execute();

        Long studentId = (Long) query.getOutputParameterValue("p_student_id");
        String message = (String) query.getOutputParameterValue("p_message");

        if (studentId == null || studentId == -1) {
            throw new OperationNotAllowedException("Could not create student: " + message);
        }

        return getStudentById(studentId);
    }

    @Transactional(readOnly = true)
    public List<StudentDTOs.StudentResponse> getAllStudents() {
        return studentRepository.findAllWithCourse().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentDTOs.StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findByIdWithCourse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public List<StudentDTOs.StudentResponse> getStudentsByCourse(Long courseId) {
        courseRepository.findById(courseId)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        return studentRepository.findAllByCourseId(courseId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentDTOs.StudentResponse getMyProfile() {
        String username = getCurrentUsername();
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Student student = studentRepository.findByUser_Id(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("No student profile linked to this account."));
        return toResponse(student);
    }

    public StudentDTOs.StudentResponse updateStudent(Long id, StudentDTOs.StudentRequest request) {
        // Check email conflict
        studentRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
            if (!existing.getStudentId().equals(id)) {
                throw new ResourceAlreadyExistsException(
                    "Email '" + request.getEmail() + "' is already registered.");
            }
        });

        if (request.getCourseId() != null) {
            courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Course not found with id: " + request.getCourseId()));
        }

        StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("sp_update_student")
            .registerStoredProcedureParameter("p_student_id",    Long.class,          ParameterMode.IN)
            .registerStoredProcedureParameter("p_first_name",    String.class,        ParameterMode.IN)
            .registerStoredProcedureParameter("p_last_name",     String.class,        ParameterMode.IN)
            .registerStoredProcedureParameter("p_email",         String.class,        ParameterMode.IN)
            .registerStoredProcedureParameter("p_phone",         String.class,        ParameterMode.IN)
            .registerStoredProcedureParameter("p_dob",           java.sql.Date.class, ParameterMode.IN)
            .registerStoredProcedureParameter("p_course_id",     Long.class,          ParameterMode.IN)
            .registerStoredProcedureParameter("p_rows_affected", Integer.class,       ParameterMode.OUT)
            .registerStoredProcedureParameter("p_message",       String.class,        ParameterMode.OUT)
            .setParameter("p_student_id",  id)
            .setParameter("p_first_name",  request.getFirstName())
            .setParameter("p_last_name",   request.getLastName())
            .setParameter("p_email",       request.getEmail())
            .setParameter("p_phone",       request.getPhone())
            .setParameter("p_dob",         request.getDateOfBirth() != null
                ? java.sql.Date.valueOf(request.getDateOfBirth()) : null)
            .setParameter("p_course_id",   request.getCourseId());

        query.execute();

        Integer rows    = (Integer) query.getOutputParameterValue("p_rows_affected");
        String  message = (String)  query.getOutputParameterValue("p_message");

        if (rows == null || rows == 0) {
            throw new OperationNotAllowedException("Update failed: " + message);
        }

        return getStudentById(id);
    }

    public void deleteStudent(Long id, boolean force) {
        studentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        StoredProcedureQuery query = entityManager
            .createStoredProcedureQuery("sp_delete_student")
            .registerStoredProcedureParameter("p_student_id",    Long.class,    ParameterMode.IN)
            .registerStoredProcedureParameter("p_force",         Integer.class, ParameterMode.IN)
            .registerStoredProcedureParameter("p_rows_affected", Integer.class, ParameterMode.OUT)
            .registerStoredProcedureParameter("p_message",       String.class,  ParameterMode.OUT)
            .setParameter("p_student_id", id)
            .setParameter("p_force",      force ? 1 : 0);

        query.execute();

        Integer rows    = (Integer) query.getOutputParameterValue("p_rows_affected");
        String  message = (String)  query.getOutputParameterValue("p_message");

        if (rows == null || rows == 0) {
            throw new OperationNotAllowedException("Delete failed: " + message);
        }
    }

    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails ud) return ud.getUsername();
        return principal.toString();
    }

    private StudentDTOs.StudentResponse toResponse(Student s) {
        CourseDTOs.CourseResponse courseResp = null;
        if (s.getCourse() != null) {
            Course c = s.getCourse();
            courseResp = CourseDTOs.CourseResponse.builder()
                .courseId(c.getCourseId())
                .courseName(c.getCourseName())
                .courseCode(c.getCourseCode())
                .courseDuration(c.getCourseDuration())
                .description(c.getDescription())
                .build();
        }

        return StudentDTOs.StudentResponse.builder()
            .studentId(s.getStudentId())
            .firstName(s.getFirstName())
            .lastName(s.getLastName())
            .fullName(s.getFirstName() + " " + s.getLastName())
            .email(s.getEmail())
            .phone(s.getPhone())
            .dateOfBirth(s.getDateOfBirth())
            .course(courseResp)
            .enrolledAt(s.getEnrolledAt())
            .updatedAt(s.getUpdatedAt())
            .build();
    }
}
