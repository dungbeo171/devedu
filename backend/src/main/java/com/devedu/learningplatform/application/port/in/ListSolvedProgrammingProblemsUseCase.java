package com.devedu.learningplatform.application.port.in;

import java.util.Set;
import java.util.UUID;

public interface ListSolvedProgrammingProblemsUseCase {

    Set<UUID> listSolvedProblemIds(UUID studentId);
}
