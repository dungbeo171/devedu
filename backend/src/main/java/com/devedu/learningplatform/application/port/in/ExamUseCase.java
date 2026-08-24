package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.AddExamQuestionCommand;
import com.devedu.learningplatform.application.port.in.command.AnswerExamQuestionCommand;
import com.devedu.learningplatform.application.port.in.command.CreateExamCommand;
import com.devedu.learningplatform.application.port.in.result.ExamAttemptSession;
import com.devedu.learningplatform.application.port.in.result.ExamDetails;
import com.devedu.learningplatform.application.port.in.result.ExamResult;
import com.devedu.learningplatform.domain.model.Exam;
import com.devedu.learningplatform.domain.model.ExamAnswer;
import com.devedu.learningplatform.domain.model.ExamQuestion;
import com.devedu.learningplatform.domain.model.UserRole;

import java.util.List;
import java.util.UUID;

public interface ExamUseCase {
    Exam createExam(CreateExamCommand command);
    ExamQuestion addQuestion(AddExamQuestionCommand command);
    List<Exam> listManagedExams(UUID actorId, UserRole actorRole);
    List<Exam> listExams();
    ExamDetails getExamBySlug(String slug);
    ExamAttemptSession startExam(UUID studentId, String slug);
    ExamAttemptSession getAttempt(UUID studentId, UUID attemptId);
    ExamAnswer answerQuestion(AnswerExamQuestionCommand command);
    ExamResult submitExam(UUID studentId, UUID attemptId);
    ExamResult getResult(UUID studentId, UUID attemptId);
    List<ExamResult> listResults(UUID actorId, UserRole actorRole, UUID examId);
}
