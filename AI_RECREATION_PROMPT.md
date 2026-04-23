# Java Project AI Feature Recreation Spec

This document describes exactly what an AI should generate for the Java project so the requested features can be recreated cleanly and consistently.

## Goal

Rebuild three features in the Java project:

1. Analytics for the shop backend, displayed in the admin area next to the existing `commande` and `produit` tabs.
2. A chatbot integrated with Gemini API.
3. Automation features for the shop workflow.

The AI should produce production-ready Java code, consistent with the existing project structure, naming style, database layer, and UI conventions.

## General Requirements

- Keep the implementation aligned with the current Java project architecture.
- Reuse existing entities, repositories, services, and controllers where possible.
- Add new files only when necessary.
- Keep UI styling consistent with the project theme.
- Add null checks, error handling, and useful logging.
- Do not break the current `commande`, `produit`, or other shop features.
- If a feature requires configuration, document the needed environment variables and setup steps.
- If the AI adds database changes, it must also provide the corresponding entity, repository, migration, and service updates.

## 1. Analytics Module

### Purpose

Create an analytics section for the shop backend so the admin can monitor sales and product performance.

### Placement in the UI

- The analytics page must live in the backend/admin area.
- It must appear next to the existing `commande` and `produit` tabs.
- The navigation should make it feel like a native part of the admin shop module, not a separate feature.

### What the AI should generate

- A backend analytics service that computes metrics from shop data.
- A controller and view/page for the admin analytics dashboard.
- Any DTOs, helpers, or query methods needed for the metrics.
- A clean UI section with cards, charts, and tables.

### Analytics content to include

- KPI cards:
  - Sales for the last 30 days.
  - Orders for the last 30 days.
  - Growth rate.
  - Average basket value.
- Sales trend chart:
  - Show the last 7 days.
  - Include a forecast line if possible.
- Top products chart:
  - Show products sorted by quantity sold.
- Product performance table:
  - Demand.
  - Margin.
  - Stock risk.
- Customer segmentation:
  - VIP customers.
  - Regular customers.
  - Occasional customers.

### Data logic expected

- Use order history and product data from the database.
- Aggregate values by day, product, and customer when needed.
- If the project already has stock prediction or sales prediction logic, reuse it instead of duplicating it.
- Keep the computations server-side so the UI only renders prepared analytics data.

### Acceptance criteria

- Admin can open the analytics section from the backend navigation.
- Analytics loads without crashing even when data is sparse.
- Charts and tables display meaningful values from real shop data.
- The section is visually consistent with the existing admin area.

## 2. Chatbot Module

### Purpose

Create a chatbot that answers shop-related questions and uses Gemini as the AI provider.

### Configuration

- The chatbot must use a Gemini API key supplied through configuration.
- The AI should not hardcode the key.
- The implementation must support environment-based configuration, for example through `.env`, application properties, or server secrets.

### What the AI should generate

- A chatbot service responsible for building prompts and sending requests to Gemini.
- A chatbot controller or API endpoint for sending and receiving messages.
- A minimal health check endpoint to confirm whether the chatbot is configured correctly.
- A frontend widget or UI integration point that can open the chat window and show messages.

### Chatbot behavior expected

- Greet the user politely.
- Answer questions about products, orders, shop support, and basic help topics.
- Use the shop context when possible, especially product information.
- Keep replies short, useful, and conversational.
- If the chatbot does not know the answer, it should suggest escalation to a human or fallback help.

### Prompt construction rules

- Build a system prompt describing the assistant persona.
- Add current product context from the database if available.
- Optionally include customer context such as name, order number, or issue type.
- Limit the amount of context sent to Gemini so the request stays clean and efficient.
- Escape unsafe text before embedding it in prompts.

### Error handling

- If the Gemini API key is missing or invalid, return a controlled error message.
- If the API call fails, return a friendly fallback response.
- Log request status and API errors for debugging.

### Acceptance criteria

- A user can open the chatbot and send messages.
- The chatbot returns AI-generated answers from Gemini.
- The chatbot can use shop product context.
- The system fails gracefully if the API key is not configured.

## 3. Automation Module

### Purpose

Create automation features for recurring shop tasks.

### Scope

The automation should cover operational behavior such as:

- Stock alerts.
- Reordering or restocking suggestions.
- Customer notifications.
- Publication or workflow triggers if the project supports them.

### What the AI should generate

- A scheduled task or job runner.
- A service layer that detects conditions and decides which actions to trigger.
- Optional event/log persistence if the project stores automation events.
- A UI section in the admin area that shows automation cards or status panels.

### Automation behavior expected

- Run automatically on a configurable schedule.
- Analyze shop data and detect critical situations.
- Generate alerts when stock is low or a product becomes critical.
- Optionally prepare messages or tasks for future manual review.
- Keep the automation visible in the UI even if some actions are backend-only.

### UI expectation

- In the admin analytics area, show automation cards such as:
  - Auto restocking.
  - Customer email automation.
  - Stock alerts.
  - Product publication workflows.
- If backend execution is not yet implemented for a card, the card should still be shown as a UI-only placeholder and clearly labeled.

### Acceptance criteria

- Automation tasks can run without user interaction.
- Alerts are generated from real data rules.
- The admin UI shows the automation status or available workflows.
- The design matches the rest of the backend shop interface.

## Output Format Required From the AI

When generating the implementation, the AI should provide:

1. The Java classes and methods needed for the feature.
2. Any controllers, services, repositories, DTOs, or entities.
3. Any view or UI components required for the backend pages.
4. Configuration changes, including environment variables.
5. Database migration or schema changes if needed.
6. A short verification checklist explaining how to test the feature.

## Suggested Priority

1. Analytics module first, because it belongs in the backend navigation next to `commande` and `produit`.
2. Chatbot second, because it depends on Gemini configuration.
3. Automation last, because it may reuse analytics and stock logic.

## Final Instruction For the AI

Generate the features as if they belong to one coherent Java shop application. The result should feel integrated, maintainable, and ready to extend later.