# 014: Gemini Live Function Calling - Findings

## Review Score: 10/10 (PASS)

## Key Discoveries
1. FunctionDeclaration takes name, description, and parameters map with Schema types
2. Tool.functionDeclarations() wraps multiple declarations into a single tool
3. FunctionCallPart.args returns Map<String, JsonElement>? - null-safe access needed
4. FunctionResponsePart requires (name, JsonObject) - must match declaration name
5. kotlinx-serialization-json dependency required for JsonObject/JsonPrimitive
6. Firebase AI experimental APIs require @PublicPreviewAPI opt-in via compiler args

## Patterns Extracted
- Gemini Function Calling handler pattern
- Shopping list agent state management
