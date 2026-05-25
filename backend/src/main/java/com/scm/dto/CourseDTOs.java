package com.scm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

public class CourseDTOs {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CourseRequest {
        @NotBlank(message = "Course name is required")
        @Size(max = 150)
        private String courseName;

        @NotBlank(message = "Course code is required")
        @Size(max = 20)
        private String courseCode;

        @NotNull(message = "Course duration is required")
        @Min(1)
        private Integer courseDuration;

        private String description;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CourseResponse {
        private Long courseId;
        private String courseName;
        private String courseCode;
        private Integer courseDuration;
        private String description;
        private Integer enrolledCount;
        private LocalDateTime createdAt;
    }
}
