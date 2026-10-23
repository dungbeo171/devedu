package com.devedu.learningplatform.application.codeassist;

import com.devedu.learningplatform.domain.codeassist.CodeSuggestionLanguage;

public interface CodeSuggestionGenerator {
    String generate(CodeSuggestionLanguage language, String prefix, String suffix, String context);
}
