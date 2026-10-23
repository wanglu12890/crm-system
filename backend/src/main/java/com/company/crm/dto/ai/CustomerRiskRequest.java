package com.company.crm.dto.ai;

import com.company.crm.dto.risk.CustomerRiskQuery;
import com.company.crm.enums.CustomerQueryScopeIntent;
import com.company.crm.exception.CustomerRiskException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.*;

/** Strict, V2-only HTTP contract. Explicit fields are checked before applying defaults. */
@JsonDeserialize(using = CustomerRiskRequest.Deserializer.class)
public record CustomerRiskRequest(Action action, CustomerRiskQuery query, String expectedResultFingerprint) {
    public enum Action { LIST, REASON, SUGGESTIONS }
    @Override public String toString() { return "CustomerRiskRequest[redacted]"; }

    public static class Deserializer extends JsonDeserializer<CustomerRiskRequest> {
        private static final Set<String> ALLOWED = Set.of("action", "scopeIntent", "queryMode", "customerLevels",
                "page", "size", "customerId", "expectedResultFingerprint");
        @Override public CustomerRiskRequest deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            try {
                if (!parser.isExpectedStartObjectToken()) throw invalid();
                Map<String, JsonNode> fields = new HashMap<>();
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    if (parser.currentToken() != JsonToken.FIELD_NAME) throw invalid();
                    String name = parser.currentName();
                    if (!ALLOWED.contains(name) || fields.containsKey(name)) throw invalid();
                    parser.nextToken();
                    fields.put(name, parser.readValueAsTree());
                }
                if (parser.nextToken() != null) throw invalid();
                Action action = Action.valueOf(text(fields.get("action")));
                boolean reason = action == Action.REASON;
                if (reason && List.of("queryMode", "customerLevels", "page", "size", "expectedResultFingerprint")
                        .stream().anyMatch(fields::containsKey)) throw invalid();
                if (!reason && fields.containsKey("customerId")) throw invalid();
                if (action != Action.SUGGESTIONS && fields.containsKey("expectedResultFingerprint")) throw invalid();
                var scope = fields.containsKey("scopeIntent") ? CustomerQueryScopeIntent.valueOf(text(fields.get("scopeIntent")))
                        : CustomerQueryScopeIntent.CURRENT_AUTHORIZED;
                var mode = fields.containsKey("queryMode") ? CustomerRiskQuery.Mode.valueOf(text(fields.get("queryMode")))
                        : CustomerRiskQuery.Mode.PRIORITY;
                Set<String> levels = new HashSet<>();
                JsonNode levelNode = fields.get("customerLevels");
                if (levelNode != null && !levelNode.isNull()) {
                    if (!levelNode.isArray() || levelNode.isEmpty()) throw invalid();
                    for (var level : levelNode) levels.add(text(level));
                }
                Long id = null;
                if (reason) {
                    String value = text(fields.get("customerId"));
                    if (!value.matches("[1-9][0-9]{0,18}")) throw invalid();
                    id = Long.valueOf(value);
                }
                String fingerprint = null;
                if (fields.containsKey("expectedResultFingerprint")) {
                    fingerprint = text(fields.get("expectedResultFingerprint"));
                    if (fingerprint.isBlank() || fingerprint.length() > 128) throw invalid();
                }
                return new CustomerRiskRequest(action, new CustomerRiskQuery(reason ? CustomerRiskQuery.Operation.REASON
                        : CustomerRiskQuery.Operation.LIST, mode, scope, levels, id,
                        integer(fields, "page", 1), integer(fields, "size", 20)), fingerprint);
            } catch (IllegalArgumentException e) { throw invalid(); }
        }
        private static int integer(Map<String, JsonNode> fields, String name, int fallback) {
            if (!fields.containsKey(name)) return fallback;
            var value = fields.get(name);
            if (!value.isIntegralNumber() || !value.canConvertToInt()) throw invalid();
            return value.intValue();
        }
        private static String text(JsonNode value) {
            if (value == null || !value.isTextual()) throw invalid();
            return value.textValue();
        }
        private static CustomerRiskException invalid() {
            return new CustomerRiskException(CustomerRiskException.Code.INVALID_QUERY);
        }
    }
}
