package com.devedu.learningplatform.application.port.in;

public interface ProgrammingProblemsUseCase extends
        ListProgrammingProblemsUseCase,
        GetProgrammingProblemUseCase,
        CreateProgrammingProblemUseCase,
        ManageProgrammingProblemsUseCase,
        RunProblemTestsUseCase,
        SubmitProgrammingProblemUseCase,
        ListSolvedProgrammingProblemsUseCase,
        GetProblemDraftUseCase,
        SaveProblemDraftUseCase {
}
