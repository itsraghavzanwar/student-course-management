package com.scm.service;

import com.scm.dto.*;
import com.scm.entity.Course;
import com.scm.exception.*;
import com.scm.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public CourseDTOs.CourseResponse createCourse(CourseDTOs.CourseRequest request) {
        if (courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new ResourceAlreadyExistsException(
                "Course with code '" + request.getCourseCode() + "' already exists.");
        }

        Course course = Course.builder()
            .courseName(request.getCourseName())
            .courseCode(request.getCourseCode().toUpperCase())
            .courseDuration(request.getCourseDuration())
            .description(request.getDescription())
            .build();

        return toResponse(courseRepository.save(course), 0);
    }

    @Transactional(readOnly = true)
    public List<CourseDTOs.CourseResponse> getAllCourses() {
        return courseRepository.findAllWithStudentCount().stream()
            .map(row -> {
                Course c = (Course) row[0];
                long count = (Long) row[1];
                return toResponse(c, (int) count);
            })
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseDTOs.CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findByIdWithStudents(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        int count = course.getStudents() != null ? course.getStudents().size() : 0;
        return toResponse(course, count);
    }

    public CourseDTOs.CourseResponse updateCourse(Long id, CourseDTOs.CourseRequest request) {
        Course course = courseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        // Check code conflict only if changed
        if (!course.getCourseCode().equalsIgnoreCase(request.getCourseCode())
                && courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new ResourceAlreadyExistsException(
                "Course code '" + request.getCourseCode() + "' is already taken.");
        }

        course.setCourseName(request.getCourseName());
        course.setCourseCode(request.getCourseCode().toUpperCase());
        course.setCourseDuration(request.getCourseDuration());
        course.setDescription(request.getDescription());

        return toResponse(courseRepository.save(course), 0);
    }

    public void deleteCourse(Long id) {
        Course course = courseRepository.findByIdWithStudents(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        if (course.getStudents() != null && !course.getStudents().isEmpty()) {
            throw new OperationNotAllowedException(
                "Cannot delete course with enrolled students. Please reassign or remove students first.");
        }

        courseRepository.delete(course);
    }

    private CourseDTOs.CourseResponse toResponse(Course c, int count) {
        return CourseDTOs.CourseResponse.builder()
            .courseId(c.getCourseId())
            .courseName(c.getCourseName())
            .courseCode(c.getCourseCode())
            .courseDuration(c.getCourseDuration())
            .description(c.getDescription())
            .enrolledCount(count)
            .createdAt(c.getCreatedAt())
            .build();
    }
}
