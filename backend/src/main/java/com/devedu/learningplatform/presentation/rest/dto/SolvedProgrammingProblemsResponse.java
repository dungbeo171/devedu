package com.devedu.learningplatform.presentation.rest.dto;

import java.util.Set;
import java.util.UUID;

public record SolvedProgrammingProblemsResponse(Set<UUID> problemIds) {
}
