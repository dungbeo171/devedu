package com.devedu.learningplatform.application.codeassist;

import com.devedu.learningplatform.domain.codeassist.CodeSuggestion;
import com.devedu.learningplatform.domain.codeassist.CodeSuggestionLanguage;

import java.util.Objects;

public final class CodeSuggestionService implements GenerateCodeSuggestionUseCase {

    static final int MAX_SOURCE_LENGTH = 100_000;
    static final int MAX_PREFIX_CONTEXT = 12_000;
    static final int MAX_SUFFIX_CONTEXT = 2_000;
    static final int MAX_SUGGESTION_LENGTH = 500;
    static final int MAX_CONTEXT_LENGTH = 20_000;

    private final CodeSuggestionGenerator generator;

    public CodeSuggestionService(CodeSuggestionGenerator generator) {
        this.generator = Objects.requireNonNull(generator);
    }

    @Override
    public CodeSuggestion suggest(CodeSuggestionLanguage language, String source, int cursor, String context) {
        Objects.requireNonNull(language, "Ngôn ngữ là bắt buộc");
        if (source == null) throw new IllegalArgumentException("Mã nguồn là bắt buộc");
        if (source.length() > MAX_SOURCE_LENGTH) {
            throw new IllegalArgumentException("Mã nguồn không được vượt quá 100000 ký tự");
        }
        if (cursor < 0 || cursor > source.length()) {
            throw new IllegalArgumentException("Vị trí con trỏ không hợp lệ");
        }
        var safeContext = context == null ? "" : context.trim();
        if (safeContext.length() > MAX_CONTEXT_LENGTH) {
            throw new IllegalArgumentException("Ngữ cảnh gợi ý không được vượt quá 20000 ký tự");
        }
        if (source.isBlank()) return new CodeSuggestion("");

        var prefixStart = Math.max(0, cursor - MAX_PREFIX_CONTEXT);
        var suffixEnd = Math.min(source.length(), cursor + MAX_SUFFIX_CONTEXT);
        var prefix = source.substring(prefixStart, cursor);
        var suffix = source.substring(cursor, suffixEnd);
        var generated = generator.generate(language, prefix, suffix, safeContext);
        return new CodeSuggestion(normalize(generated, prefix));
    }

    private String normalize(String generated, String prefix) {
        if (generated == null) return "";
        var value = generated.replace("\r\n", "\n").replace('\r', '\n');
        if (value.startsWith("```")) {
            var firstBreak = value.indexOf('\n');
            value = firstBreak >= 0 ? value.substring(firstBreak + 1) : "";
        }
        value = value.replaceFirst("(?s)```.*$", "");
        while (value.startsWith("\n")) value = value.substring(1);
        var lineBreak = value.indexOf('\n');
        if (lineBreak >= 0) value = value.substring(0, lineBreak);

        var currentLineStart = prefix.lastIndexOf('\n') + 1;
        var currentLine = prefix.substring(currentLineStart);
        if (!currentLine.isBlank() && value.startsWith(currentLine)) {
            value = value.substring(currentLine.length());
        } else if (!currentLine.isBlank() && value.startsWith(currentLine.stripLeading())) {
            value = value.substring(currentLine.stripLeading().length());
        }
        value = value.stripTrailing();
        if (value.length() > MAX_SUGGESTION_LENGTH) value = value.substring(0, MAX_SUGGESTION_LENGTH);
        return value.isBlank() ? "" : value;
    }
}
