package com.devedu.learningplatform.presentation.rest.dto;

import java.util.List;

public record ManageCourseStudentsRequest(List<Long> studentIds) {}
