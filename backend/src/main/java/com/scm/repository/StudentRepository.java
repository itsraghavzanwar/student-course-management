package com.scm.repository;

import com.scm.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByEmail(String email);

    Optional<Student> findByEmail(String email);

    List<Student> findByCourse_CourseId(Long courseId);

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.course WHERE s.studentId = :id")
    Optional<Student> findByIdWithCourse(@Param("id") Long id);

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.course")
    List<Student> findAllWithCourse();

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.course WHERE s.course.courseId = :courseId")
    List<Student> findAllByCourseId(@Param("courseId") Long courseId);

    Optional<Student> findByUser_Id(Long userId);
}
