# Banque Misr Internal Banking Operations Copilot

**ITI Backend Development Track — Final Capstone Project (2026)**

An internal banking operations copilot built as a **Java/Spring Boot backend application**. It is designed to help simulated branch employees find reliable answers in approved banking documentation and prepare service requests for supervisor review.

> [!IMPORTANT]
> **Educational simulation only.** This project uses synthetic data and public or purpose-created training documents. It does **not** connect to Banque Misr production systems, access real customer accounts, or execute real financial transactions.

## Project Status

**Planning / initial setup.** The features and architecture below describe the project requirements and intended implementation, not completed functionality.

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

The application has not yet been scaffolded. Once the implementation is added, this section will document:

1. Prerequisites and required environment variables (without secrets).
2. Local database and application setup.
3. Maven build and test commands.
4. Docker startup and shutdown commands.
5. API documentation and a repeatable demo scenario.

**Do not use real banking data or commit credentials, API keys, or other secrets.**

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
