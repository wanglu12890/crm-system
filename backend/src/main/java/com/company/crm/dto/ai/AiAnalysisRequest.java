package com.company.crm.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.*;
import java.io.IOException;

@JsonDeserialize(using = AiAnalysisRequest.Deserializer.class)
public record AiAnalysisRequest(
        @NotBlank(message = "分析问题不能为空")
        @Size(max = 1000, message = "分析问题长度不能超过1000个字符")
        String question,
        Map<String, JsonNode> interaction
) {
    public AiAnalysisRequest(String question) { this(question, Map.of()); }
    public AiAnalysisRequest { interaction = interaction == null ? Map.of() : Map.copyOf(interaction); }
    /** Unknown V1 fields remain compatible; risk validates its own allowlist. */
    public static class Deserializer extends JsonDeserializer<AiAnalysisRequest> {
        @Override public AiAnalysisRequest deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.isExpectedStartObjectToken()) return (AiAnalysisRequest) context.handleUnexpectedToken(AiAnalysisRequest.class, parser);
            Map<String, JsonNode> fields = new HashMap<>();
            String question = null;
            boolean questionSeen = false;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.FIELD_NAME) throw JsonMappingException.from(parser, "Invalid request");
                String name = parser.currentName();
                parser.nextToken();
                JsonNode value = parser.readValueAsTree();
                if ("question".equals(name)) {
                    if (questionSeen) fields.put("__duplicate_control_field", com.fasterxml.jackson.databind.node.BooleanNode.TRUE);
                    questionSeen = true;
                    if (!value.isTextual()) throw JsonMappingException.from(parser, "Invalid question");
                    question = value.textValue();
                } else if (fields.putIfAbsent(name, value) != null) {
                    fields.put("__duplicate_control_field", com.fasterxml.jackson.databind.node.BooleanNode.TRUE);
                }
            }
            if (parser.nextToken() != null)
                fields.put("__trailing_content", com.fasterxml.jackson.databind.node.BooleanNode.TRUE);
            return new AiAnalysisRequest(question, fields);
        }
    }
    @Override
    public String toString() {
        return "AiAnalysisRequest[question=<redacted>]";
    }
}
