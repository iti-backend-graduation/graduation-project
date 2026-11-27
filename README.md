# Banque Misr Internal Banking Operations Copilot

**ITI Backend Development Track — Final Capstone Project (2026)**

An internal banking operations copilot built as a **Java/Spring Boot backend application**. It is designed to help simulated branch employees find reliable answers in approved banking documentation and prepare service requests for supervisor review.

> [!IMPORTANT]
> **Educational simulation only.** This project uses synthetic data and public or purpose-created training documents. It does **not** connect to Banque Misr production systems, access real customer accounts, or execute real financial transactions.

## Project Status

**In active development.** The features and architecture below describe the project requirements and implementation direction.

## Objectives

- Provide evidence-grounded answers to operational questions, including precise document and page/section citations.
- Refuse to answer when the knowledge base lacks sufficient evidence.
- Allow employees to draft and submit simulated service requests.
- Enforce supervisor approval before any protected simulated action.
- Demonstrate secure REST APIs, persistence, AI integration, testing, auditability, and Docker-based delivery.

## User Roles

| Role | Capabilities |
| --- | --- |
| `EMPLOYEE` | Ask the copilot questions, view cited answers, create/edit own drafts, submit requests, and track own requests. |
| `SUPERVISOR` | All employee capabilities, plus review submitted requests, approve, reject with a reason, or edit and approve requests. |

Permissions must be enforced **server-side**. Employees must not approve requests, access other employees' private requests, or trigger protected execution through direct API calls.

## Required Features

### 1. AI Copilot & Document Knowledge Base

- Ingest at least two document formats (e.g., PDF and Markdown).
- Process approximately **15–20 documents / 60–80 pages**.
- Extract, clean, chunk, embed, and index documents in identifiable stages.
- Track source, page/section, and version or ingestion time; prevent duplicate ingestion.
- Combine semantic and keyword retrieval using a documented ranking method.
- Generate grounded answers with exact citations, refuse unsupported answers, and resist prompt injection from source documents.

### 2. Service Request & Approval Workflow

- Create, edit, validate, and submit simulated service requests.
- Preserve request status, timestamps, history, decisions, and execution outcomes.
- Require supervisor review before simulated execution.
- Support approval, rejection with reason, and edit-and-approve.
- Prevent duplicate execution and retain an audit trail.

**Suggested lifecycle:**

```text
DRAFT -> PENDING_APPROVAL -> APPROVED -> EXECUTED
                          \-> REJECTED
```

### 3. Multi-Agent AI Workflow

The system must include an orchestrator and at least three specialized agents:

| Component | Responsibility |
| --- | --- |
| Knowledge Retrieval Agent | Find relevant document passages and citations. |
| Policy Validation Agent | Check evidence and identify missing information or unsupported assumptions. |
| Request Preparation Agent | Produce a structured service-request draft. |
| Orchestrator | Coordinate agents and control execution flow. |

Agent boundaries, restricted tools, typed messages, termination conditions, iteration limits, per-step timeouts, and a fallback to ordinary retrieval/answering are required. Agents cannot bypass supervisor approval.

### 4. Security, Observability & Quality

- Authenticated API access, secure password hashing, role-based authorization, input validation, and safe database access.
- Correlation/run IDs, structured logs, auditable approval and execution events.
- Queryable AI token usage and estimated cost per request.
- Health and readiness endpoints.
- Unit, integration, security, and workflow tests.
- Repeatable evaluation suite with **at least 15 cases**, covering retrieval/citations, refusal, ambiguity, prompt injection, invalid requests, unauthorized actions, duplicate execution, and agent failures.
- Report measured retrieval hit rate, groundedness, refusal correctness, and known limitations.

## Planned Technology Stack

| Area | Technology |
| --- | --- |
| Language | Java 17+ |
| Backend | Spring Boot, Spring MVC |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL with compatible vector-search support |
| Security | Spring Security |
| AI | Spring AI with a supported LLM provider |
| Build | Maven |
| Testing | JUnit 5, Mockito, Spring Boot Test |
| Operations | Docker, Spring Boot Actuator |

*Specific versions, the vector-search solution, LLM provider, and authentication mechanism will be finalized during implementation.*

## Proposed Architecture

A **modular Spring Boot monolith** is the recommended baseline. Logical modules:

```text
identity         Authentication, users, roles, permissions
knowledge        Document ingestion and metadata
retrieval        Semantic + keyword search and ranking
copilot          Grounded answers and agent orchestration
service-request  Request creation, validation, lifecycle
approval         Supervisor review and simulated execution
audit            Request tracing, activity history, AI usage
```

This is a logical module plan, **not** a claim about the current repository structure.

## Development Roadmap

| Week | Focus | Target outcome |
| --- | --- | --- |
| 1 | Backend foundation, authentication, database model, document ingestion | Secured application and initial searchable knowledge base |
| 2 | Hybrid retrieval, grounded answers, citations, refusal behavior | Working AI copilot |
| 3 | Service requests, permissions, approvals, audit trail | Employee-to-supervisor workflow |
| 4 | Multi-agent orchestration, evaluation, testing, Docker, documentation | Demonstrable capstone project |

## Team Collaboration

- Track work as [GitHub Issues](https://github.com/iti-backend-graduation/graduation-project/issues).
- Follow progress on the [Project Board](https://github.com/orgs/iti-backend-graduation/projects/1).
- Develop on a dedicated branch and open a Pull Request to `main`.
- Link related Issues in PR descriptions and follow the PR checklist.
- Follow the repository's [Contributing Guidelines](CONTRIBUTING.md).

## Getting Started

The application is under active development. Setup instructions, environment variables, local database configuration, Maven commands, Docker usage, API documentation, and the repeatable demo scenario will be maintained here as the implementation is completed.

**Do not use real banking data or commit credentials, API keys, or other secrets.**

### Local setup

1. Start PostgreSQL with pgvector:

```powershell
cd copilot
docker compose up -d db
```

2. Set local environment values in `copilot/.env`:

```properties
DB_PASSWORD=1234
DB_USER=copilot
DB_NAME=copilot
JWT_SECRET=replace-with-at-least-32-characters
GEMINI_API_KEY=your-local-gemini-key
```

3. Run the application:

```powershell
cd copilot
.\mvnw spring-boot:run
```

4. Run all tests:

```powershell
cd copilot
.\mvnw test
```

The Gemini embedding model is `gemini-embedding-001`; document chunks use 768
dimensions. Flyway migration V21 switches the existing vector schema and removes
old provider embeddings, which cannot be reused after an embedding-model change.

## Copilot: Retrieval, Answers and Agents

Code: `copilot/src/main/java/com/bm/copilot/retrieval` and `.../copilot`. Demo requests: [`copilot/http/copilot.http`](copilot/http/copilot.http).

### Hybrid retrieval

1. **Semantic search:** the question is embedded and compared with every chunk by pgvector cosine distance (`<=>`). Hits below `copilot.retrieval.min-similarity` (0.5) are dropped.
2. **Keyword search:** PostgreSQL full-text search (`tsvector`, English dictionary). The question's words are OR-ed, so a long natural question still matches, and `ts_rank` puts chunks with more and rarer matches first.
3. **Ranking with Reciprocal Rank Fusion (RRF):** each search returns its top 20; every chunk scores `Σ 1 / (60 + rank)` over the lists it appears in, and the top 5 are kept. RRF uses ranks only, so the two very different score scales never need to be compared.

Equal scores are ordered by file name and chunk index, so the same question always gets the same sources. All values are bound SQL parameters.

### Grounded answers

`POST /api/copilot/ask` (role `EMPLOYEE`; supervisors inherit it):

```json
{ "question": "How much is the replacement fee for a lost debit card?" }
```

```json
{
  "answer": "The replacement fee for a lost debit card is EGP 100.",
  "citations": [{ "documentName": "02-lost-stolen-card-procedure.md", "page": null, "section": "5.2 Replacement Fee" }],
  "refused": false,
  "suggestServiceRequest": false,
  "requestDraft": null,
  "clarificationQuestion": null
}
```

When the answer suggests a service request (for example "A customer lost their debit card. What should I do?"), `requestDraft` holds a prepared draft for the employee to complete. Nothing is submitted.

- The model (Gemini, temperature 0, JSON output) sees only the retrieved sources, each wrapped in a numbered `<source>` tag, and must cite them by number.
- **Refusal:** the copilot refuses without calling the model when nothing is retrieved. It also refuses when the model refuses, cites a source it was not given, cites nothing, or returns invalid JSON.
- **Prompt injection:** source text is escaped so it cannot close its tag, and the system prompt treats source text as data, never instructions.
- **Clarifying questions:** when the sources give different answers depending on a missing detail ("What is the card limit?"), the copilot returns `clarificationQuestion` instead of guessing. `answer` then holds the same question and there are no citations.
- **Limits:** questions are 1–1000 characters (400 `VALIDATION_FAILED` otherwise). Each user can ask 10 questions per minute (429 `TOO_MANY_REQUESTS`).
- **Token usage:** every model call is recorded in `ai_usage` and can be queried at `/api/admin/ai-usage`.

### Agents

`CopilotOrchestrator` runs typed agents, each with one job:

| Agent | Can do | Cannot do |
| --- | --- | --- |
| Knowledge Retrieval Agent | search the knowledge base | call the model or change data |
| Policy Check Agent | check the answer against its cited evidence | rewrite the answer |
| Request Preparation Agent | return a request draft | submit, approve or execute a request |

- **Flow:** retrieve → answer → policy check → request draft (only when the answer suggests one).
- **Termination:** the run ends when an answer passes the policy check, the model refuses, or the model asks a clarifying question.
- **Iteration limit:** at most 3 attempts; retries search wider (5 sources, then 10). After 3 failed checks the copilot refuses.
- **Timeouts:** every step has a timeout (`copilot.orchestrator.step-timeout`, 30 s).
- **Fallback:** any error or timeout falls back to plain retrieve-and-answer, which is still policy-checked.
- **Logging:** the correlation ID is carried into every step's log lines.

### Evaluation

```powershell
cd copilot
.\mvnw test "-Dtest=CopilotEvaluationTest" "-Deval=true"
.\mvnw test "-Dtest=ClarificationProbeTest" "-Deval=true"
```

Both runs need Docker and `GEMINI_API_KEY`, and call the real model. Embeddings are cached in `target/eval-embeddings.cache` to stay within the free-tier quota. Reports: [`copilot/eval/copilot-results.md`](copilot/eval/copilot-results.md) and [`copilot/eval/clarification-probe.md`](copilot/eval/clarification-probe.md).

To reproduce the corpus page count, run `python eval/render_markdown_corpus.py` from
`copilot/`. The script renders all Markdown sources with headless Chrome/Chromium
using US Letter paper, 13 pt Arial, 1.75 line spacing, 0.55-inch vertical and
1.25-inch horizontal margins, and one source per starting page; it counts the
resulting PDF pages and adds the physical pages in source PDFs. It prints the
browser version and exits unsuccessfully if the total is outside 60–80 pages.
The generated PDF is written to the ignored `copilot/target/` directory.

Most recent real Gemini-backed orchestrator run (21 cases; 27 Nov 2026), using
the current evaluation cases and separate outcome metrics:

| Metric | Result |
| --- | --- |
| Hit@1 / Hit@5 (answerable) | 12/13 / 13/13 |
| Answered when expected | 13/13 |
| Correct refusals / clarifications | 7/7 / 1/1 |
| Citation validity (answered) | 13/13 |
| Grounded answers (Gemini judge, answered) | 13/13 |
| Clarifying-question probe (separate two-run evaluation) | 12/12 |

The run met the documented evaluation thresholds, including retrieving the
expected support FAQ for `support-01` and directly answering the revised
account-opening question. Full per-case results are in
[`copilot/eval/copilot-results.md`](copilot/eval/copilot-results.md). The real
Gemini evaluation requires Docker and a locally configured `GEMINI_API_KEY`.

### Known limitations

- The model can vary between runs even at temperature 0. The reports show each run, not a best case.
- Refusals and clarifications are measured as distinct outcomes and are not combined into a non-answer correctness metric.
- A question that is only partly ambiguous (for example a fee that differs by card type) may be answered or clarified.
- Branch business-hours questions are refused, because the planted injection test line sits in the same chunk as the hours. The policy check then drops that evidence; this is safe but strict.
- The Request Preparation Agent is a placeholder until the service-request module provides the real one.
- The free Gemini tier limits embeddings to 1000 a day and 100 a minute. A full ingestion needs about 250, so uploading all 16 files back to back can hit the per-minute limit. That upload fails (HTTP 500, nothing is stored) and succeeds when retried a minute later.
- The corpus has 15 Markdown sources and one PDF. Under the documented Chrome print layout, the Markdown renders to 75 physical pages and the source PDF is 1 page, for 76 physical pages total. See [`copilot/eval/report.md`](copilot/eval/report.md) and rerun the documented script after corpus changes.
- Request/approval endpoints are not implemented in the merged code yet, so the workflow evaluation cases still depend on Dev D's implementation.

## Acceptance Demo

The completed project should demonstrate:

1. An employee logs in and asks a question about an ingested banking document.
2. The copilot returns a cited answer or correctly refuses unsupported questions.
3. The employee submits a simulated service request.
4. The employee is blocked from approving it, including via direct REST API access.
5. A supervisor reviews, approves, rejects, or edits and approves the request.
6. The application records the decision and performs only approved **simulated** execution.
7. Automated tests verify security, workflow integrity, retrieval, and failure handling.
8. The application runs using the documented Docker configuration.

## Scope & Priorities

All core features above are mandatory for the capstone. Microservices, cloud deployment, advanced retrieval, bilingual AI, and advanced monitoring are **optional bonuses** and should not displace mandatory work.

## Reference

Project scope: **ITI — Final Capstone Project: Banque Misr Internal Banking Operations Copilot** (provided project brief).
