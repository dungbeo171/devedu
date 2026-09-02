package com.devedu.learningplatform.presentation.rest.dto;

import java.time.LocalDate;

public record CreateCourseRequest(String slug, String title, String description,
                                  LocalDate startDate, LocalDate endDate) {}
