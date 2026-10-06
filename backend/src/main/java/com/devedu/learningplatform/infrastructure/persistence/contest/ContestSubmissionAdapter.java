package com.devedu.learningplatform.infrastructure.persistence.contest;

import com.devedu.learningplatform.application.contest.*;
import com.devedu.learningplatform.application.port.in.ProgrammingProblemsUseCase;
import com.devedu.learningplatform.application.port.in.command.SubmitProblemCommand;
import com.devedu.learningplatform.domain.model.ProblemSubmission;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class ContestSubmissionAdapter implements ContestSubmissionPort {
    private final ProgrammingProblemsUseCase problems;
    private final ContestRepository contests;
    private final EntityManager em;
    private final ProblemSubmissionRepository saved;
    private final Clock clock;
    public ContestSubmissionAdapter(ProgrammingProblemsUseCase problems, ContestRepository contests, EntityManager em, ProblemSubmissionRepository saved, Clock clock) {
        this.problems = problems; this.contests = contests; this.em=em; this.saved=saved; this.clock=clock;
    }
    @Override @Transactional
    public ProblemSubmission submit(UUID contestId, SubmitProblemCommand command, Instant receivedAt) {
        return submit(contestId,command,receivedAt,null,null);
    }
    @Override @Transactional
    public ProblemSubmission submit(UUID contestId, SubmitProblemCommand command, Instant receivedAt, UUID virtualId, String requestKey) {
        if(requestKey==null) requestKey=UUID.randomUUID().toString();
        if(!requestKey.matches("[A-Za-z0-9_-]{1,100}")) throw new IllegalArgumentException("Invalid Idempotency-Key");
        UUID scope=virtualId==null?contestId:virtualId;
        // Shared admission lock lets different submissions run in parallel, but excludes finalization.
        em.createNativeQuery("select 1 from pg_advisory_xact_lock_shared(hashtextextended(:key,0))").setParameter("key","contest:"+contestId).getSingleResult();
        em.createNativeQuery("select 1 from pg_advisory_xact_lock(hashtextextended(:key,0))").setParameter("key","request:"+scope+":"+command.studentId()+":"+requestKey).getSingleResult();
        String hash=digest(command.problemSlug()+"\n"+command.language()+"\n"+command.sourceCode());
        var existing=em.createNativeQuery("select request_hash,submission_id from contest_submission_requests where scope_id=:scope and user_id=:user and request_key=:key")
                .setParameter("scope",scope).setParameter("user",command.studentId()).setParameter("key",requestKey).getResultList();
        if(!existing.isEmpty()) {
            var row=(Object[])existing.get(0);
            if(!hash.equals(row[0])) throw new ContestException(ContestException.Kind.CONFLICT,"Idempotency-Key was already used for different code");
            return saved.findById((UUID)row[1]).orElseThrow();
        }
        String admission=virtualId==null
                ? "select count(*) from contests c where c.id=:id and c.starts_at <= clock_timestamp() and c.ends_at > clock_timestamp() and not exists(select 1 from contest_finalizations f where f.contest_id=c.id)"
                : "select count(*) from virtual_contests where id=:id and user_id=:user and contest_id=:contest and starts_at <= clock_timestamp() and ends_at > clock_timestamp()";
        var query=em.createNativeQuery(admission).setParameter("id",scope);
        if(virtualId!=null) query.setParameter("user",command.studentId()).setParameter("contest",contestId);
        if(((Number)query.getSingleResult()).longValue()==0) throw new ContestException(ContestException.Kind.CONFLICT,"Contest is not accepting submissions");
        receivedAt=clock.instant();
        var submission = problems.submit(command);
        if(virtualId==null) contests.linkSubmission(contestId, submission.id(), receivedAt);
        else em.createNativeQuery("insert into contest_submissions(submission_id,contest_id,received_at,virtual_id,mode) values(:submission,:contest,:at,:virtual,'VIRTUAL')")
                .setParameter("submission",submission.id()).setParameter("contest",contestId).setParameter("at",receivedAt).setParameter("virtual",virtualId).executeUpdate();
        em.createNativeQuery("insert into contest_submission_requests(scope_id,user_id,request_key,request_hash,submission_id) values(:scope,:user,:key,:hash,:submission)")
                .setParameter("scope",scope).setParameter("user",command.studentId()).setParameter("key",requestKey).setParameter("hash",hash).setParameter("submission",submission.id()).executeUpdate();
        return submission;
    }
    private String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
