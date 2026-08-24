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
    UUID getId(){return id;} UUID getProblemId(){return problemId;} String getInput(){return input;}
    String getExpectedOutput(){return expectedOutput;} int getTimeLimitMillis(){return timeLimitMillis;} int getPosition(){return position;}
}
