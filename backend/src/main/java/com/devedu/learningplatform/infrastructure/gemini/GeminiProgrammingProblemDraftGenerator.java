package com.devedu.learningplatform.infrastructure.gemini;

import com.devedu.learningplatform.application.exception.ProblemGenerationException;
import com.devedu.learningplatform.application.port.in.result.GeneratedProgrammingProblemDraft;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemDraftGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class GeminiProgrammingProblemDraftGenerator implements ProgrammingProblemDraftGenerator {

    private static final String API_ROOT = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String SYSTEM_INSTRUCTION = """
            Bạn là trợ lý tạo bài tập lập trình cho nền tảng DevEdu. Hãy tạo đúng một bài tập bằng tiếng Việt từ mô tả của giáo viên.
            Yêu cầu bắt buộc:
            - Nội dung rõ ràng, tự chứa, không nhắc đến AI.
            - description chỉ chứa đề bài; định dạng Input và Output phải nằm riêng trong inputDescription và outputDescription.
            - sampleOutput phải chính xác với sampleInput.
            - Tạo từ 3 đến 6 test case ẩn khác nhau và tự giải để expectedOutput chính xác; không giải thích test ẩn trong đề.
            - allowedLanguages chỉ dùng CPP, JAVA, PYTHON, HTML, MYSQL. Bài thuật toán không dùng HTML hoặc MYSQL.
            - Bài SQL chỉ có topic SQL, chỉ cho MYSQL, không đọc stdin; input test case là SQL setup và starter code là câu truy vấn mẫu có thể sửa.
            - Mỗi ngôn ngữ được phép phải có starterCodes tương ứng, tối giản và biên dịch/chạy được sau khi người học hoàn thiện.
            - Dùng topic INTRODUCTION, CPP, JAVA, PYTHON, OOP, DATA_STRUCTURES, ALGORITHMS hoặc SQL.
            - Dùng difficulty EASY, MEDIUM hoặc HARD.
            - timeLimitMillis nằm trong 100 đến 30000.
            Chỉ trả dữ liệu theo JSON schema, không thêm markdown hoặc lời giải.
            """;
    private static final String RESPONSE_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "slug": { "type": "string" },
                "title": { "type": "string" },
                "summary": { "type": "string" },
                "description": { "type": "string" },
                "inputDescription": { "type": "string" },
                "outputDescription": { "type": "string" },
                "sampleInput": { "type": "string" },
                "sampleOutput": { "type": "string" },
                "topic": { "type": "string", "enum": ["INTRODUCTION", "CPP", "JAVA", "PYTHON", "OOP", "DATA_STRUCTURES", "ALGORITHMS", "SQL"] },
                "difficulty": { "type": "string", "enum": ["EASY", "MEDIUM", "HARD"] },
                "allowedLanguages": {
                  "type": "array",
                  "items": { "type": "string", "enum": ["CPP", "JAVA", "PYTHON", "HTML", "MYSQL"] }
                },
                "starterCodes": {
                  "type": "object",
                  "properties": {
                    "CPP": { "type": "string" },
                    "JAVA": { "type": "string" },
                    "PYTHON": { "type": "string" },
                    "HTML": { "type": "string" },
                    "MYSQL": { "type": "string" }
                  }
                },
                "testCases": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "input": { "type": "string" },
                      "expectedOutput": { "type": "string" },
                      "timeLimitMillis": { "type": "integer" }
                    },
                    "required": ["input", "expectedOutput", "timeLimitMillis"]
                  }
                }
              },
              "required": ["slug", "title", "summary", "description", "inputDescription", "outputDescription", "sampleInput", "sampleOutput", "topic", "difficulty", "allowedLanguages", "starterCodes", "testCases"]
            }
            """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final URI endpoint;
    private final Duration timeout;

    public GeminiProgrammingProblemDraftGenerator(
            ObjectMapper objectMapper,
            @Value("${devedu.gemini.api-key:}") String apiKey,
            @Value("${devedu.gemini.model:gemini-2.5-flash}") String model,
            @Value("${devedu.gemini.timeout:PT45S}") Duration timeout
    ) {
        if (model == null || !model.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Gemini model name is invalid");
        }
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.endpoint = URI.create(API_ROOT + model + ":generateContent");
        this.timeout = timeout;
    }

    @Override
    public GeneratedProgrammingProblemDraft generate(String description) {
        if (apiKey.isBlank()) {
            throw new ProblemGenerationException(
                    ProblemGenerationException.Kind.NOT_CONFIGURED,
                    "Gemini chưa được cấu hình. Quản trị viên cần thêm GEMINI_API_KEY"
            );
        }
        try {
            var request = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody(description)))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                var message = response.statusCode() == 429
                        ? "Gemini đã hết hạn mức tạm thời. Vui lòng thử lại sau"
                        : "Gemini không thể tạo bài tập lúc này";
                throw new ProblemGenerationException(ProblemGenerationException.Kind.UNAVAILABLE, message);
            }
            return parseResponse(response.body());
        } catch (ProblemGenerationException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ProblemGenerationException(
                    ProblemGenerationException.Kind.UNAVAILABLE,
                    "Yêu cầu tạo bài bằng Gemini đã bị gián đoạn",
                    exception
            );
        } catch (IOException | RuntimeException exception) {
            throw new ProblemGenerationException(
                    ProblemGenerationException.Kind.UNAVAILABLE,
                    "Không thể kết nối đến Gemini. Vui lòng thử lại sau",
                    exception
            );
        }
    }

    private String requestBody(String description) throws JsonProcessingException {
        var root = objectMapper.createObjectNode();
        root.putObject("systemInstruction").putArray("parts").addObject().put("text", SYSTEM_INSTRUCTION);
        var content = root.putArray("contents").addObject();
        content.put("role", "user");
        content.putArray("parts").addObject().put("text", description);
        var generation = root.putObject("generationConfig");
        generation.put("responseMimeType", "application/json");
        generation.set("responseSchema", objectMapper.readTree(RESPONSE_SCHEMA));
        generation.put("temperature", 0.35);
        generation.put("maxOutputTokens", 12_000);
        return objectMapper.writeValueAsString(root);
    }

    private GeneratedProgrammingProblemDraft parseResponse(String body) {
        try {
            var root = objectMapper.readTree(body);
            var candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) throw invalidResponse();
            var parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) throw invalidResponse();
            var text = new StringBuilder();
            for (JsonNode part : parts) {
                if (part.hasNonNull("text")) text.append(part.get("text").asText());
            }
            if (text.isEmpty()) throw invalidResponse();
            return objectMapper.readValue(text.toString(), GeneratedProgrammingProblemDraft.class);
        } catch (ProblemGenerationException exception) {
            throw exception;
        } catch (JsonProcessingException exception) {
            throw new ProblemGenerationException(
                    ProblemGenerationException.Kind.INVALID_RESPONSE,
                    "Gemini trả về dữ liệu bài tập không hợp lệ. Vui lòng mô tả rõ hơn và thử lại",
                    exception
            );
        }
    }

    private ProblemGenerationException invalidResponse() {
        return new ProblemGenerationException(
                ProblemGenerationException.Kind.INVALID_RESPONSE,
                "Gemini không trả về nội dung bài tập. Vui lòng thử lại"
        );
    }
}
