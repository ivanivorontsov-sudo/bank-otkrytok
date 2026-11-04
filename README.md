# Банк открыток

Android-приложение (Kotlin, Compose): открытки с ИИ-генерацией, стихами Ивана Воронцова и 150 готовыми картинками (PD/CC0, см. IMAGES-LICENSES.md).

- ИИ: веб-чат в WebView (Алиса, ChatGPT, Gemini, Grok, Qwen, Copilot, GLM — вход запоминается, как в «ИванОператор»), бесплатно через Pollinations или свой OpenAI-совместимый API-ключ. К каждому запросу добавляется системный промпт открытки.
- Стихи: `app/src/main/assets/poems.json` (145 стихов из epub, `tools-epub-poems.py`) + `quotes.json`. ИИ может подобрать стих к поводу.
- Готовая открытка сжимается в JPEG (до 1440 px, ≤450 КБ) и отправляется через «Поделиться» / сохраняется в галерею.
- Сборка release: положите `keystore.properties` (не коммитится) в корень. Тесты: `./gradlew testDebugUnitTest`.
- Ветка `otkrytki-original` — полная история бывшего репозитория otkrytki.
