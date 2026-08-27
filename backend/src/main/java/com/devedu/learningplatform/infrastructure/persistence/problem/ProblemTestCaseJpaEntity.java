package com.devedu.learningplatform.infrastructure.persistence.problem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "problem_test_cases")
class ProblemTestCaseJpaEntity {
    @Id private UUID id;
    @Column(name="problem_id",nullable=false) private UUID problemId;
    @Column(nullable=false,columnDefinition="TEXT") private String input;
    @Column(name="expected_output",nullable=false,columnDefinition="TEXT") private String expectedOutput;
    @Column(name="time_limit_ms",nullable=false) private int timeLimitMillis;
    @Column(nullable=false) private int position;
    protected ProblemTestCaseJpaEntity() {}
    ProblemTestCaseJpaEntity(UUID id, UUID problemId, String input, String expectedOutput,
                             int timeLimitMillis, int position) {
        this.id = id;
        this.problemId = problemId;
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.timeLimitMillis = timeLimitMillis;
        this.position = position;
    }
    UUID getId(){return id;} UUID getProblemId(){return problemId;} String getInput(){return input;}
    String getExpectedOutput(){return expectedOutput;} int getTimeLimitMillis(){return timeLimitMillis;} int getPosition(){return position;}
}
