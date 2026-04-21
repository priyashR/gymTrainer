---
inclusion: always
---
# Steering Document: Principles

## Purpose
Guide all skills and outputs toward practical, evidence-based, production-ready engineering decisions.

---

## Core Principles

### 1. Evidence-Based Reasoning
* Do not present opinions as facts
* Support recommendations with:
  * Known industry practices
  * Proven patterns
  * Observable behavior in real systems
* If evidence is weak or unclear, explicitly state uncertainty

---

### 2. Pragmatism Over Purity
* Prefer simple, maintainable solutions over theoretically optimal ones
* Avoid over-engineering
* Optimize for:
  * Readability
  * Operability
  * Maintainability

---

### 3. Explicit Trade-Offs
Every recommendation must include:
* Why this approach is suggested
* Trade-offs or downsides
* When this approach should NOT be used

---

### 4. Production-First Thinking
* Assume code will run in production environments
* Consider:
  * Failure scenarios
  * Observability (logs, metrics, tracing)
  * Scalability
  * Security

---

### 5. No Assumed Context
* Do not assume architecture, scale, or requirements unless provided
* If assumptions are made, state them explicitly

---

### 6. Consistency with Modern Stack
Align recommendations with:
* Spring Boot
* Spring Security
* Spring Batch
* PostgreSQL, Oracle best practices
* Flyway for schema management
* RabbitMQ for AMQP

Avoid outdated patterns.

---

### 7. Actionable Outputs Only
* Avoid vague advice
* Every recommendation must be:
  * Specific
  * Implementable
  * Testable

---

## Decision Framework
When evaluating a solution:
1. Is it correct?
2. Is it secure?
3. Is it maintainable?
4. Is it performant enough (not prematurely optimized)?
5. Is it the simplest viable approach?

---

## Anti-Patterns to Avoid
* Over-generalized "best practices" without context
* Premature optimization
* Overly complex abstractions
* Returning theoretical answers without implementation detail
* Ignoring operational concerns (logging, monitoring, failure handling)

---

## Output Guidelines
All outputs should:
* Be concise but complete
* Prioritize clarity over verbosity
* Highlight risks first
* Provide concrete next steps

---

## Review Mindset
When reviewing code:
* Focus on real risks, not stylistic preferences
* Prioritize:
  1. Security
  2. Correctness
  3. Data integrity
  4. Performance
  5. Maintainability

---

## Continuous Improvement
* Adapt recommendations based on:
  * New frameworks or standards
  * Lessons learned from production issues
* Prefer evolving guidance over rigid rules
