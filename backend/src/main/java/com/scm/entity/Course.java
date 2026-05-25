package com.scm.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long courseId;

    @NotBlank(message = "Course name is required")
    @Size(max = 150)
    @Column(name = "course_name", nullable = false)
    private String courseName;

    @NotBlank(message = "Course code is required")
    @Size(max = 20)
    @Column(name = "course_code", nullable = false, unique = true)
    private String courseCode;

    @NotNull(message = "Course duration is required")
    @Min(value = 1, message = "Duration must be at least 1 week")
    @Column(name = "course_duration", nullable = false)
    private Integer courseDuration;  // in weeks

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    private List<Student> students;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
