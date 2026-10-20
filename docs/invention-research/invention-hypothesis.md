# Invention Hypothesis: Revision-Aware Evidence Routing and Consistency Control

**Date:** 2026-10-20

## Executive Summary

SpaceDoc Intelligence investigates whether a **query-conditioned, revision-aware evidence-routing controller** can improve evidence selection reliability and answer consistency in technical document RAG systems by systematically accounting for document revision metadata, explicit dependencies, and evidence conflicts before answer generation.

This is a **candidate invention hypothesis under investigation**, not a claim of established novelty or patentability.

## Problem Statement

Technical document collections—particularly in aerospace and space mission domains—exhibit structural complexity that conventional RAG systems do not fully address:

1. **Revision Proliferation**: Documents undergo multiple revisions. A requirement may be superseded in a newer revision, yet older documents may still reference the original requirement. Retrieving from mixed revisions without applicability checking produces inconsistent answers.

2. **Explicit Dependencies**: Requirements reference other requirements. Procedures depend on external specifications. Missions are described across multiple documents. Conventional retrieval does not systematically exploit these explicit dependencies to select complementary evidence.

3. **Unmanaged Evidence Conflicts**: When retrieved passages contradict each other, conventional RAG systems may generate answers that:
   - Ignore the conflict entirely.
   - Cherry-pick one passage without acknowledgment.
   - Allow the model's internal preference to resolve the conflict without transparency.
   
   This is especially problematic in technical domains where conflicts may indicate:
   - Document revision inconsistency.
   - Requirement clarifications or corrections.
   - Genuinely incompatible design choices.
   - Extraction or retrieval errors.

4. **Limited Evidence Transparency**: Users cannot easily determine:
   - Why specific evidence was selected over alternatives.
   - Whether retrieved evidence is from the current or obsolete revision.
   - Which documents depend on the evidence presented.
   - Whether the evidence is contradicted elsewhere.

5. **Brittle Impact Analysis**: When a technical document changes, identifying which other documents and requirements are affected requires manual review or external tools. A document-centric RAG system has the necessary dependency information but does not expose it in a structured, queryable form.

## Proposed Solution: Revision-Aware Evidence Routing

### Core Mechanism

A **revision-aware evidence router** that:

1. **Analyzes Query Intent** — Extracts signals from the query such as:
   - Target document or revision (explicit: "latest version"; implicit: reference to a mission or requirement).
   - Required evidence type (procedure, requirement, rationale, comparison).
   - Scope (single document vs. cross-document consistency).

2. **Plans Retrieval Strategy** — Selects an appropriate combination of retrieval operations:
   - **Dense Retrieval** — Semantic similarity for exploratory questions.
   - **Lexical Retrieval** — Exact term matching for requirement numbers, acronyms, version identifiers.
   - **Revision Filtering** — Restrict to a specific document revision or temporal range.
   - **Dependency-Guided Expansion** — Traverse explicit relationships to retrieve supporting evidence.
   - **Consistency Retrieval** — Retrieve evidence that confirms, contradicts, or clarifies the primary result.

3. **Routes Evidence Collection** — Executes the selected plan with:
   - Configurable traversal depth for dependency expansion.
   - Limits on retrieved chunk count to avoid information overload.
   - Timeouts to maintain acceptable query latency.
   - Duplicate and near-duplicate detection.

4. **Converges Evidence** — Combines retrieval results with:
   - Relevance scoring (from retrieval).
   - Revision applicability checks (document current for this query?).
   - Explicit relationship validation (does this evidence directly support the claim?).
   - Conflict detection (do retrieved passages contradict each other?).

5. **Gates Answer Generation** — Produces answers that:
   - Explicitly reference supporting evidence with page/section mappings.
   - Acknowledge contradictory evidence and explain the contradiction.
   - State when evidence is insufficient.
   - Distinguish documented facts from inferences.
   - Treat retrieved text as untrusted data, not instructions.

### Data Model

- **Document** — Stable identifier, metadata, source.
- **DocumentRevision** — Revision identifier, timestamp, change summary.
- **DocumentChunk** — Text, page, section, embedding, revision association.
- **Requirement** — Identifier, text, document, revision applicability.
- **TechnicalEntity** — Domain entity (mission, subsystem, standard, acronym) extracted from or referenced by documents.
- **DocumentRelationship** — (document_a, document_b, relationship_type, evidence, explicit_or_inferred, applicable_revision).
- **RetrievalPlan** — Query, chosen strategy, parameters, candidates, result, duration.
- **EvidenceSet** — Query, retrieved chunks, routing plan, applicability status, conflict status.
- **ReviewerFeedback** — Feedback on evidence, citation quality, conflict resolution, completeness.

### Testable Hypotheses

1. **Query-Conditioned Routing Improves Relevance** — Evidence selected by the routing controller contains a higher proportion of relevant, applicable chunks than conventional hybrid retrieval.

2. **Revision Filtering Reduces Inconsistency** — Answers respect document revision boundaries more strictly when routing uses revision metadata compared to revision-unaware retrieval.

3. **Dependency Expansion Improves Coverage** — Cross-document questions receive more complete evidence when the routing controller expands through explicit dependencies compared to single-stage retrieval.

4. **Conflict Detection Improves Transparency** — Reviewers can more easily identify evidence inconsistency and understand its source when conflicts are explicitly detected and reported.

5. **Evidence-Gated Generation Reduces Hallucination** — Answers contain fewer unsupported claims when generation is constrained to explicitly retrieved evidence and proper handling of conflicts.

## What This Is NOT

- **Not a purely graph-based RAG.** While the system maintains a relationship graph, it uses primarily relational storage and selective graph traversal, not a dedicated graph database.

- **Not a standard RAG + knowledge graph combination.** Those are established, published techniques. The specific contribution, if any, lies in the **query-conditioned routing controller** and its integration with revision metadata and explicit conflict handling.

- **Not a learning system.** The routing controller uses deterministic rules and configurable parameters, not learned policies or neural routing.

- **Not a replacement for human review.** It is a tool to improve evidence selection and transparency, requiring human validation of conclusions.

- **Not a general-purpose AI safety advancement.** The focus is specifically on technical document consistency and applicability in the ISRO domain, not broad AI alignment.

## Inventorship and Contribution

This hypothesis was formulated as a software architecture task by Claude Code (an AI assistant) at the direction of the project owner. 

**Actual inventorship attribution requires:**

1. Evidence of a human contributor's independent conception of the mechanism.
2. Disclosure of reduction to practice, design decisions, and validation results.
3. Contemporaneous documentation (design documents, dated development logs, commits).
4. Legal and patent counsel review to assess patentability and inventorship.

Do not assume that generating code, documentation, or architecture constitutes inventorship. Patent eligibility and inventorship are determined by law, not by participation in development.

## Success Criteria

The hypothesis is **supported by evidence** if:

1. The routing controller is implemented and functional.
2. Baseline comparison experiments are conducted:
   - Dense retrieval alone.
   - Lexical retrieval alone.
   - Conventional hybrid (RRF).
   - Hybrid + revision filtering.
   - Full routing controller.
3. Measured differences in recall, precision, citation validity, conflict detection, and unsupported-claim rate are observed.
4. Ablation experiments show that each component (routing, revision filtering, dependency expansion, conflict detection) contributes measurably to the results.
5. The improvements are not attributable to existing published techniques applied in isolation.

The hypothesis is **not supported** (or requires refinement) if:

1. The routing controller does not improve over conventional hybrid retrieval by measurable margins.
2. Component ablations show that most improvements come from standard techniques (e.g., reranking), not the routing mechanism.
3. Prior-art analysis reveals that a closely similar mechanism has been disclosed or patented.
4. Retrieval latency or implementation complexity introduces unacceptable overhead.

## Next Steps

1. **Prior-Art Investigation** (Phase 1)
   - Search patent databases (USPTO, WIPO, Google Patents).
   - Search scholarly literature (arXiv, ACM, IEEE).
   - Review open-source RAG implementations.
   - Document findings in a prior-art matrix.

2. **Architecture and Data Model** (Phase 2)
   - Define the precise routing controller algorithm.
   - Specify the relationship graph schema.
   - Design the evidence-gating generation prompt.

3. **Implementation** (Phases 3–5)
   - Build functional RAG.
   - Implement routing controller.
   - Implement impact analysis.

4. **Evaluation** (Phase 6)
   - Create evaluation dataset with human-annotated evidence.
   - Run baseline and ablation experiments.
   - Record quantitative results.

5. **Analysis and Documentation** (Phase 7–8)
   - Assess whether measured results support the hypothesis.
   - Document limitations and future work.
   - Prepare research documentation and dated development log.
   - **Do not submit patent application without appropriate legal counsel.**

---

## Disclaimers

- **Novelty is not established.** Published techniques in graph-based RAG, retrieval planning, and version-aware information retrieval may fully or partially overlap with this work.
- **Patentability is not assessed.** Legal review is required to determine eligibility and scope.
- **Implementation does not guarantee impact.** The routing controller may not measurably improve outcomes, or the improvements may be attributable to other factors.
- **ISRO endorsement is not claimed.** This is an independent research project using space domain framing for demonstration and evaluation purposes.

---

**Status:** Hypothesis formulated, Phase 1 in progress.

**Last Updated:** 2026-10-20
