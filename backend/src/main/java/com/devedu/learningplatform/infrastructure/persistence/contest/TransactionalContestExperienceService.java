package com.devedu.learningplatform.infrastructure.persistence.contest;

import com.devedu.learningplatform.application.contest.*;
import com.devedu.learningplatform.application.contest.rating.EloRatingCalculator;
import com.devedu.learningplatform.application.port.out.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.*;
import java.time.Clock;

@Service @Transactional @EnableScheduling
public class TransactionalContestExperienceService extends ContestExperienceService {
    public TransactionalContestExperienceService(ContestRepository contests, ContestExperienceRepository storage, ContestUseCase catalog,
            UserRepository users, ProgrammingProblemRepository problems, ContestSubmissionPort submissions, Clock clock) {
        super(contests,storage,catalog,users,problems,submissions,clock,new EloRatingCalculator());
    }
    @Override @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void finalizeEnded() { super.finalizeEnded(); }
}
