package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.ExamForbiddenException;
import com.devedu.learningplatform.application.exception.ExamStateException;
import com.devedu.learningplatform.application.port.in.command.AddExamQuestionCommand;
import com.devedu.learningplatform.application.port.in.command.AnswerExamQuestionCommand;
import com.devedu.learningplatform.application.port.in.command.CreateExamCommand;
import com.devedu.learningplatform.application.port.out.*;
import com.devedu.learningplatform.domain.model.*;
import org.junit.jupiter.api.Test;
import java.time.Clock; import java.time.Instant; import java.time.ZoneOffset;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExamServiceTest {
    private static final Instant NOW=Instant.parse("2026-08-22T10:00:00Z");
    private static final UUID TEACHER=UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID OTHER=UUID.fromString("30000000-0000-0000-0000-000000000002");
    private static final UUID STUDENT=UUID.fromString("30000000-0000-0000-0000-000000000003");
    private final TestExamRepository exams=new TestExamRepository(); private final TestQuestionRepository questions=new TestQuestionRepository();
    private final TestAttemptRepository attempts=new TestAttemptRepository(); private final TestAnswerRepository answers=new TestAnswerRepository();
    private final ExamService service=new ExamService(exams,questions,attempts,answers,Clock.fixed(NOW,ZoneOffset.UTC));

    @Test void teacherCreatesExamAndBothQuestionTypes(){
        var exam=createExam(NOW.minusSeconds(60)); var choice=addChoice(exam.id());
        var coding=service.addQuestion(new AddExamQuestionCommand(TEACHER,UserRole.TEACHER,exam.id(),ExamQuestionType.CODING,"Viết chương trình",List.of(),null,CodeLanguage.JAVA,5,2));
        assertThat(choice.options()).containsExactly("2","4"); assertThat(coding.codingLanguage()).isEqualTo(CodeLanguage.JAVA);
    }

    @Test void scoresMultipleChoiceAndLeavesCodingPending(){
        var exam=createExam(NOW.minusSeconds(60)); var choice=addChoice(exam.id());
        var coding=service.addQuestion(new AddExamQuestionCommand(TEACHER,UserRole.TEACHER,exam.id(),ExamQuestionType.CODING,"Code",List.of(),null,CodeLanguage.PYTHON,7,2));
        var session=service.startExam(STUDENT,exam.slug());
        service.answerQuestion(new AnswerExamQuestionCommand(STUDENT,session.attempt().id(),choice.id(),1,null));
        service.answerQuestion(new AnswerExamQuestionCommand(STUDENT,session.attempt().id(),coding.id(),null,"print(4)"));
        var result=service.submitExam(STUDENT,session.attempt().id());
        assertThat(result.attempt().automaticScore()).isEqualTo(3);
        assertThat(result.attempt().automaticMaxScore()).isEqualTo(3);
        assertThat(result.attempt().pendingCodingQuestions()).isEqualTo(1);
    }

    @Test void examCannotStartBeforeScheduledTime(){
        var exam=createExam(NOW.plusSeconds(60)); addChoice(exam.id());
        assertThatThrownBy(()->service.startExam(STUDENT,exam.slug())).isInstanceOf(ExamStateException.class).hasMessageContaining("not started");
    }

    @Test void teacherCannotManageAnotherTeachersExam(){
        var exam=createExam(NOW);
        assertThatThrownBy(()->service.addQuestion(new AddExamQuestionCommand(OTHER,UserRole.TEACHER,exam.id(),ExamQuestionType.MULTIPLE_CHOICE,"Q",List.of("A","B"),0,null,1,1))).isInstanceOf(ExamForbiddenException.class);
    }

    @Test void questionSetIsLockedAfterAttemptStarts(){
        var exam=createExam(NOW.minusSeconds(1)); addChoice(exam.id()); service.startExam(STUDENT,exam.slug());
        assertThatThrownBy(()->addChoice(exam.id())).isInstanceOf(ExamStateException.class).hasMessageContaining("cannot be changed");
    }

    @Test void submittingTwiceIsIdempotent(){
        var exam=createExam(NOW.minusSeconds(1)); addChoice(exam.id()); var attempt=service.startExam(STUDENT,exam.slug()).attempt();
        assertThat(service.submitExam(STUDENT,attempt.id()).attempt()).isEqualTo(service.submitExam(STUDENT,attempt.id()).attempt());
    }

    @Test void teacherListsOnlyOwnedExams(){
        var owned=createExam(NOW);
        exams.save(new Exam(UUID.randomUUID(),"other-exam","Other","Other",OTHER,NOW,30,NOW));
        assertThat(service.listManagedExams(TEACHER,UserRole.TEACHER)).extracting(Exam::id).containsExactly(owned.id());
        assertThat(service.listManagedExams(TEACHER,UserRole.ADMIN)).hasSize(2);
    }

    private Exam createExam(Instant scheduled){return service.createExam(new CreateExamCommand(TEACHER,UserRole.TEACHER,"java-midterm","Java Midterm","Kiểm tra",scheduled,60));}
    private ExamQuestion addChoice(UUID examId){return service.addQuestion(new AddExamQuestionCommand(TEACHER,UserRole.TEACHER,examId,ExamQuestionType.MULTIPLE_CHOICE,"2 + 2?",List.of("2","4"),1,null,3,1));}

    private static final class TestExamRepository implements ExamRepository{
        private final Map<UUID,Exam> values=new HashMap<>();
        public boolean existsBySlug(String slug){return values.values().stream().anyMatch(e->e.slug().equals(slug));} public Exam save(Exam e){values.put(e.id(),e);return e;}
        public List<Exam> findAll(){return new ArrayList<>(values.values());} public Optional<Exam> findById(UUID id){return Optional.ofNullable(values.get(id));}
        public List<Exam> findAllByTeacherId(UUID teacherId){return values.values().stream().filter(e->e.teacherId().equals(teacherId)).toList();}
        public Optional<Exam> findBySlug(String slug){return values.values().stream().filter(e->e.slug().equals(slug)).findFirst();}
    }
    private static final class TestQuestionRepository implements ExamQuestionRepository{
        private final Map<UUID,ExamQuestion> values=new HashMap<>(); public ExamQuestion save(ExamQuestion q){values.put(q.id(),q);return q;}
        public Optional<ExamQuestion> findById(UUID id){return Optional.ofNullable(values.get(id));}
        public List<ExamQuestion> findAllByExamId(UUID examId){return values.values().stream().filter(q->q.examId().equals(examId)).sorted(Comparator.comparingInt(ExamQuestion::position)).toList();}
    }
    private static final class TestAttemptRepository implements ExamAttemptRepository{
        private final Map<UUID,ExamAttempt> values=new HashMap<>(); public ExamAttempt save(ExamAttempt a){values.put(a.id(),a);return a;}
        public Optional<ExamAttempt> findById(UUID id){return Optional.ofNullable(values.get(id));}
        public Optional<ExamAttempt> findByExamIdAndStudentId(UUID examId,UUID studentId){return values.values().stream().filter(a->a.examId().equals(examId)&&a.studentId().equals(studentId)).findFirst();}
        public List<ExamAttempt> findAllByExamId(UUID examId){return values.values().stream().filter(a->a.examId().equals(examId)).toList();}
        public boolean existsByExamId(UUID examId){return values.values().stream().anyMatch(a->a.examId().equals(examId));}
    }
    private static final class TestAnswerRepository implements ExamAnswerRepository{
        private final Map<UUID,ExamAnswer> values=new HashMap<>(); public ExamAnswer save(ExamAnswer a){values.put(a.id(),a);return a;}
        public Optional<ExamAnswer> findByAttemptIdAndQuestionId(UUID attemptId,UUID questionId){return values.values().stream().filter(a->a.attemptId().equals(attemptId)&&a.questionId().equals(questionId)).findFirst();}
        public List<ExamAnswer> findAllByAttemptId(UUID attemptId){return values.values().stream().filter(a->a.attemptId().equals(attemptId)).toList();}
    }
}
