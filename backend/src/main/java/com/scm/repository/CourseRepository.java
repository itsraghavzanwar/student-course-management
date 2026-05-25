package com.scm.repository;

import com.scm.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByCourseCode(String courseCode);

    Optional<Course> findByCourseCode(String courseCode);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.students WHERE c.courseId = :id")
    Optional<Course> findByIdWithStudents(@Param("id") Long id);

    @Query("SELECT c, COUNT(s) FROM Course c LEFT JOIN c.students s GROUP BY c")
    List<Object[]> findAllWithStudentCount();
}
