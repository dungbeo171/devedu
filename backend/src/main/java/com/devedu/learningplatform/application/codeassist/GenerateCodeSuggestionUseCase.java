package com.devedu.learningplatform.application.codeassist;

import com.devedu.learningplatform.domain.codeassist.CodeSuggestion;
import com.devedu.learningplatform.domain.codeassist.CodeSuggestionLanguage;

public interface GenerateCodeSuggestionUseCase {
    CodeSuggestion suggest(CodeSuggestionLanguage language, String source, int cursor, String context);
}
