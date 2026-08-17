# Good First Issue Guidelines for the Hiero Java SDK

## Purpose

This document explains what should and should not be considered a Good First Issue (GFI) for the Hiero Java SDK.

The goal is to make it easier for:
- Maintainers to consistently label beginner-friendly issues
- New contributors to identify safe and realistic first contributions
- Community members to propose useful first issues that fit the Java SDK specifically

A Good First Issue should help someone become familiar with the project without requiring deep knowledge of protocol internals, architecture, or sensitive SDK behavior.

---

## What Makes a Good First Issue?

A Good First Issue should generally:

- Be small in scope
- Be possible to complete in a focused pull request
- Be limited to a clear area of the codebase
- Require minimal domain-specific knowledge
- Be low-risk and easy to review
- Improve readability, maintainability, documentation, or small isolated functionality
- Help contributors learn project patterns without requiring architectural decisions

---

## Good First Issue Examples

### Small, Focused Source Changes

These are isolated improvements to existing source code that do not introduce major behavioral or architectural changes.

Examples:
- Improving utility or helper methods
- Improving `toString()` output
- Cleaning up `equals()` or `hashCode()` implementations
- Improving exception messages
- Clarifying null checks
- Refactoring small methods for readability
- Removing redundant or confusing code in isolated areas

---

### Documentation Improvements

These are changes that improve clarity for users or contributors.

Examples:
- Improving JavaDoc
- Clarifying README instructions
- Updating outdated comments
- Adding explanations for confusing logic
- Improving contributor documentation
- Fixing wording, grammar, or formatting issues

---

### Refactors of Existing Examples

These changes should improve readability or maintainability only.

Examples:
- Renaming unclear variables
- Improving formatting
- Extracting repeated logic into helper methods
- Improving example comments
- Making example structure easier to follow

**Note:** This applies to existing examples only, not new examples.

---

### Print and Output Clarity

Examples should be understandable and user-friendly.

Examples:
- Replacing vague output like `"Done"` with more descriptive text
- Improving formatting of printed output
- Adding context around displayed values
- Standardizing example output for clarity

---

### Functional Improvements to Existing Examples

These are small improvements that better demonstrate existing SDK behavior without introducing new workflows.

Examples:
- Adding a missing setup step
- Improving ordering of steps
- Clarifying example flow
- Improving basic error-handling clarity

---

### Test Improvements (Additive Only)

These should improve existing tests without creating entirely new testing structures.

Examples:
- Adding assertions to existing tests
- Covering small edge cases
- Improving test readability
- Renaming tests for clarity
- Improving failure messages

**Note:** Good First Issues should improve existing tests, not create new test suites.

---

## What We Do NOT Consider Good First Issues

The following are generally out of scope for first-time contributors.

---

### New Examples

Examples:
- Creating entirely new example projects
- Adding new workflows
- Designing new educational demos

Why:
These often require deeper understanding of intended SDK usage patterns.

---

### New Unit or Integration Tests

Examples:
- Creating new test files
- Designing new testing frameworks
- Building large new test structures

Why:
These usually require broader architectural context.

---

### Core Protocol or Serialization Logic

Examples:
- `toProto` / `fromProto`
- Serialization or deserialization changes
- Wire-level protocol changes
- Network-sensitive behavior
- Transaction mapping logic

Why:
These are sensitive areas that may affect SDK correctness.

---

### Cross-Cutting or Architectural Changes

Examples:
- Multi-module refactors
- Public API redesign
- Concurrency changes
- Performance overhauls
- Build system redesign

Why:
These changes typically require broader project familiarity.

---

## General Principles for Maintainers

When labeling a Good First Issue:

- Keep scope narrow
- Keep expectations clear
- Prefer isolated improvements
- Avoid protocol-sensitive areas
- Avoid architectural redesign
- Ensure the task is realistic for a first-time contributor

---

## General Principles for Contributors

When selecting a Good First Issue:

- Start with readability, documentation, or isolated fixes
- Follow existing code patterns
- Keep pull requests focused
- Avoid broad refactors unless explicitly requested
- Ask for clarification when boundaries are unclear

---

## Summary

A Good First Issue for the Hiero Java SDK should be:

**Small, clear, safe, reviewable, and beginner-friendly.**

The best first contributions improve clarity, documentation, maintainability, or small isolated areas of the Java SDK without requiring deep protocol or architectural expertise.
