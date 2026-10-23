package com.devedu.learningplatform.infrastructure.persistence.contest;

import com.devedu.learningplatform.application.contest.*;
import com.devedu.learningplatform.application.port.out.*;
import com.devedu.learningplatform.application.port.in.CodeJudgeUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.*;
import java.time.Clock;
import java.util.UUID;
import com.devedu.learningplatform.application.security.AuthenticatedUser;

@Service @Transactional @EnableScheduling
public class TransactionalContestExperienceService extends ContestExperienceService {
    public TransactionalContestExperienceService(ContestRepository contests, ContestExperienceRepository storage, ContestUseCase catalog,
            UserRepository users, ProgrammingProblemRepository problems, ContestSubmissionPort submissions,
            ContestProblemRepository contestProblems, CodeJudgeUseCase judge, Clock clock) {
        super(contests,storage,catalog,users,problems,submissions,contestProblems,judge,clock);
    }
    @Override @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void finalizeEnded() { super.finalizeEnded(); }
    // Redeclare inherited write use case so Spring applies one transaction to lock + insert.
    @Override @Transactional
    public ContestExperienceViews.VirtualSession startVirtual(UUID id, AuthenticatedUser user) {
        return super.startVirtual(id, user);
    }
}
