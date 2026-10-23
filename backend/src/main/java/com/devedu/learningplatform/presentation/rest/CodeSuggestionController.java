package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.codeassist.GenerateCodeSuggestionUseCase;
import com.devedu.learningplatform.presentation.rest.dto.CodeSuggestionRequest;
import com.devedu.learningplatform.presentation.rest.dto.CodeSuggestionResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/code/suggestions")
public class CodeSuggestionController {

    private final GenerateCodeSuggestionUseCase useCase;

    public CodeSuggestionController(GenerateCodeSuggestionUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    public ResponseEntity<CodeSuggestionResponse> suggest(@RequestBody CodeSuggestionRequest request) {
        var result = useCase.suggest(request.language(), request.source(), request.cursor(), request.context());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CodeSuggestionResponse(result.text()));
    }
}
