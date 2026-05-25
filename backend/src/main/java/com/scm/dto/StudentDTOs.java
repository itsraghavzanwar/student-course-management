package com.scm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class StudentDTOs {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class StudentRequest {
        @NotBlank(message = "First name is required")
        @Size(max = 80)
        private String firstName;

        @NotBlank(message = "Last name is required")
        @Size(max = 80)
        private String lastName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email")
        private String email;

        @Size(max = 20)
        private String phone;

        private LocalDate dateOfBirth;

        private Long courseId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StudentResponse {
        private Long studentId;
        private String firstName;
        private String lastName;
        private String fullName;
        private String email;
        private String phone;
        private LocalDate dateOfBirth;
        private CourseDTOs.CourseResponse course;
        private LocalDateTime enrolledAt;
        private LocalDateTime updatedAt;
    }
}
