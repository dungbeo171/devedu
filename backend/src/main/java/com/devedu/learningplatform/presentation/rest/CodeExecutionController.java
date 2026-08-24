package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.ExecuteCodeUseCase;
import com.devedu.learningplatform.application.port.in.command.ExecuteCodeCommand;
import com.devedu.learningplatform.presentation.rest.dto.CodeExecutionRequest;
import com.devedu.learningplatform.presentation.rest.dto.CodeExecutionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/code")
public class CodeExecutionController {

    private final ExecuteCodeUseCase executeCodeUseCase;

    public CodeExecutionController(ExecuteCodeUseCase executeCodeUseCase) {
        this.executeCodeUseCase = executeCodeUseCase;
    }

    @PostMapping("/execute")
    public ResponseEntity<CodeExecutionResponse> execute(@RequestBody CodeExecutionRequest request) {
        var result = executeCodeUseCase.execute(
                new ExecuteCodeCommand(request.language(), request.code(), request.input())
        );
        return ResponseEntity.ok(
                new CodeExecutionResponse(result.language(), result.status(), result.output())
        );
    }
}
