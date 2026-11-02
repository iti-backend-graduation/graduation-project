# Contributing Guidelines

Welcome to the ITI Backend Graduation Project!

This document defines our development workflow and collaboration standards.

## 1. Branching Strategy

The `main` branch is protected and should always contain stable code.

All development must happen in separate branches created from the latest `main`.

### Branch Naming Convention

Use the following format:

`type/short-description`

| Type | Purpose | Example |
|---|---|---|
| feature | New functionality | `feature/user-authentication` |
| fix | Bug fixes | `fix/login-validation` |
| refactor | Code improvements | `refactor/user-service` |
| test | Automated tests | `test/authentication` |
| docs | Documentation | `docs/api-documentation` |
| chore | Configuration and maintenance | `chore/maven-setup` |
| ci | CI/CD changes | `ci/github-actions` |

Use lowercase letters and hyphens.

## 2. Commit Message Convention

Follow Conventional Commits:

`type: short description`

Examples:

- `feat: implement user registration`
- `fix: handle invalid login credentials`
- `refactor: simplify user service`
- `test: add authentication unit tests`
- `docs: update API documentation`
- `chore: configure Maven dependencies`
- `ci: add GitHub Actions workflow`

Keep commits focused and meaningful.

## 3. Development Workflow

1. Pull the latest changes from `main`.
2. Create a feature branch.
3. Implement your assigned task.
4. Commit your changes with meaningful messages.
5. Push your branch to GitHub.
6. Open a Pull Request targeting `main`.
7. Complete the Pull Request template.
8. Review changes and resolve any feedback.
9. Merge only after satisfying repository rules.

## 4. Pull Request Guidelines

Each Pull Request should:

- Have a clear and descriptive title.
- Explain the changes made.
- Reference related issues when applicable.
- Include relevant testing information.
- Avoid unrelated changes.
- Be small enough to review effectively.

Peer code reviews are encouraged, even when not required by branch protection rules.

## 5. Code Quality

- Follow agreed Java and Spring Boot conventions.
- Keep code readable and maintainable.
- Use meaningful names for classes, methods, and variables.
- Avoid unnecessary duplication.
- Handle errors appropriately.
- Add relevant unit and integration tests.
- Ensure the application builds successfully before requesting a merge.

## 6. Security

Never commit:

- Passwords or API keys.
- Access tokens or private credentials.
- Real database secrets.
- Local environment files containing sensitive information.

Use environment variables or appropriate secret management.

## 7. Team Collaboration

- Use your own GitHub account for all contributions.
- Do not share GitHub credentials.
- Communicate before making major architectural changes.
- Respect other developers' assigned work.
- Resolve merge conflicts carefully.
- Keep the team informed of blockers.
- Document important technical decisions.

## 8. Contribution Tracking

Each contributor is responsible for maintaining a clear contribution history through:

- Meaningful commits.
- Feature branches.
- Pull Requests.
- Issue references where applicable.
- Code reviews and discussions.

All team members share responsibility for delivering a high-quality graduation project.

---

**One Team. One Goal. Build Better Together.**
