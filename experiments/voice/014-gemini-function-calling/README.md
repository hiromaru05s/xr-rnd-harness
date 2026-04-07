# 014: Gemini Live Function Calling Integration

## Hypothesis
Gemini Live Function Calling can enable voice-controlled app state manipulation (shopping list add/remove/list) for an agent-style UI on AI glasses.

## Technologies
- Skills: glasses-hardware, glimmer-api
- Libraries: Firebase AI Logic (Gemini Live), Glimmer

## Implementation
- FunctionDeclaration for addItem, removeItem, listItems
- Tool.functionDeclarations() for tool list
- functionCallHandler dispatching by function name
- FunctionResponsePart with JsonObject result
- LiveSession.startAudioConversation(functionCallHandler)
- Shopping list state reflected in Glimmer UI

## How to Run
1. Set up Firebase project and add google-services.json
2. Open in Android Studio Canary
3. Start Smartphone AVD and AI Glasses AVD
4. Target Smartphone AVD and run

## Findings
(After test/review)

## Extracted Patterns
(After PASS, reference patterns/ links)
