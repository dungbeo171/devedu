package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.ProgrammingProblem;

import java.time.Instant;

public record CourseProblemProgress(ProgrammingProblem problem, Instant assignedAt, boolean solved) {}
