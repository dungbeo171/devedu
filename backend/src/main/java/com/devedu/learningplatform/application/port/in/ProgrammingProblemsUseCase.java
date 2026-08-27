package com.devedu.learningplatform.application.port.in;

public interface ProgrammingProblemsUseCase extends
        ListProgrammingProblemsUseCase,
        GetProgrammingProblemUseCase,
        CreateProgrammingProblemUseCase,
        SubmitProgrammingProblemUseCase,
        ListSolvedProgrammingProblemsUseCase,
        GetProblemDraftUseCase,
        SaveProblemDraftUseCase {
}
