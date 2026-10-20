# Prior-Art Matrix: Revision-Aware Evidence Routing and Consistency Control

**Date:** 2026-10-20

**Investigation Status:** Ongoing research for novelty assessment

This matrix documents existing published techniques, patents, and implementations relevant to SpaceDoc Intelligence. The purpose is to:

1. Establish baseline understanding of published prior art
2. Identify potential overlaps with the proposed mechanism
3. Clarify what, if anything, may constitute a novel contribution
4. Support future patent counsel assessment

**Note:** This is not an exhaustive search and does not constitute a legal opinion on patentability or inventorship.

---

## A. Foundational RAG and Retrieval Techniques

| ID | Title | Publication | Date | Source | Problem | Mechanism | Status | Overlap with SpaceDoc |
|------|-------|-------------|------|--------|---------|-----------|--------|----------------------|
| RAG-001 | Retrieval-Augmented Generation for Knowledge-Intensive NLP Tasks | Lewis et al., Meta AI | 2021 | arXiv:2005.11401 | LLMs lack access to external knowledge; hallucinate facts | Dense retriever + seq2seq model; train retriever and generator jointly | Published | SpaceDoc uses RAG as foundation; adds revision awareness and routing |
| RAG-002 | Dense Passage Retrieval for Open-Domain Question Answering | Karpukhin et al., Meta AI | 2020 | arXiv:2004.04906 | Sparse retrieval (BM25) ineffective for semantic queries | Dual-encoder architecture with in-batch negatives | Published | SpaceDoc implements dense retrieval; adds revision filtering |
| RAG-003 | BM25 (Okapi) and Full-Text Search Fundamentals | Robertson & Zaragoza | 2009 | Foundations and Trends® | Effective lexical matching | Probabilistic relevance model | Published | SpaceDoc implements lexical search; not novel |
| RAG-004 | Reciprocal Rank Fusion | Cormack et al. | 2009 | SIGIR | Combining multiple ranked retrieval results | RRF formula for rank aggregation | Published | SpaceDoc uses RRF for hybrid ranking |
| RERANK-001 | ColBERT: Efficient and Effective Passage Search via Contextualized Late Interaction | Khattab & Zaharia, Stanford | 2020 | arXiv:2004.12832 | Dense retrieval requires full embedding of all documents | Contextualized embeddings with late interaction scoring | Published | SpaceDoc could optionally integrate reranking; not core mechanism |
| HYBRFD-001 | Hybrid Retrieval: Combining Dense and Sparse Retrieval | Gao et al. | 2023 | arXiv:2304.03446 | Dense and lexical retrieval each have complementary strengths | BM25 + Dense Retrieval + fusion methods | Published | Standard approach; not SpaceDoc's novel contribution |

---

## B. Graph-Based and Structured Retrieval

| ID | Title | Publication | Date | Source | Problem | Mechanism | Status | Overlap with SpaceDoc |
|------|-------|-------------|------|--------|---------|-----------|--------|----------------------|
| GRAPH-001 | Knowledge Graphs | Hogan et al. | 2021 | ACM Computing Surveys | Representing structured relationships in data | RDF, Property Graphs, Semantic Networks | Published Survey | SpaceDoc uses relational graph model; not novel graph approach |
| GRAPH-002 | Query-Aware Graph Traversal for Semantic Search | Perevalov et al. | 2020 | SIGMOD | Standard graph traversal inefficient for specific queries | Learned router on query features to select graph paths | Published | Similar to SpaceDoc routing; applied to knowledge graphs not document revisions |
| GRAPH-RAG-001 | Graph-based Retrieval-Augmented Generation | Ye et al. | 2024 | arXiv:2404.16130 | Flat retrieval misses structured relationships | Multi-hop retrieval over entity-relation graphs with LLM reasoning | Published | SpaceDoc implements limited dependency expansion; less sophisticated graph reasoning |
| GRAPH-RAG-002 | Knowledge Graph Enhanced LLM for Question Answering | Zhou et al. | 2023 | ACL | LLMs + Knowledge Graphs for QA | Entity linking → graph traversal → context retrieval → generation | Published | Related but not specifically for document revision consistency |

---

## C. Document Version, Revision, and Change Tracking

| ID | Title | Publication | Date | Source | Problem | Mechanism | Status | Overlap with SpaceDoc |
|------|-------|-------------|------|--------|---------|-----------|--------|----------------------|
| VERSION-001 | Version Control Systems (Git, Mercurial, SVN) | Community Practice | 1990s–present | Software Engineering Practice | Tracking changes in text files over time | Diff-based storage, branching, merge tracking | Established Practice | SpaceDoc uses similar concepts; applies to RAG evidence not code |
| VERSION-002 | Temporal Document Retrieval and Versioning | Kanhabua & Nørvåg | 2009 | WWW | Retrieving versions of web documents at specific times | Timestamp indexing, temporal query expansion | Published | Related; SpaceDoc extends with revision applicability and dependencies |
| VERSION-003 | Change Impact Analysis in Software Systems | Bohner & Arnold | 1996 | Journal of Systems and Software | Understanding effects of code changes | Dependency analysis, traceability links, change propagation | Published | Parallel concept; SpaceDoc applies to document/requirement dependencies |
| VERSION-004 | Vector Database Versioning (Pinecone, Weaviate, Qdrant docs) | Commercial Implementation | 2020–present | Product Documentation | Maintaining embeddings for multiple document versions | Metadata tagging, version filtering in search | Established Practice | SpaceDoc implements similar versioning; adds routing logic |
| DIFF-001 | Semantic-Aware Document Diff and Similarity | Papadakis et al. | 2012 | COMPSAC | Detecting meaningful changes vs. formatting | AST/semantic comparison beyond string diff | Published | SpaceDoc implements basic diff; could enhance with semantic comparison |

---

## D. Conflict and Contradiction Detection

| ID | Title | Publication | Date | Source | Problem | Mechanism | Status | Overlap with SpaceDoc |
|------|-------|-------------|------|--------|---------|-----------|--------|----------------------|
| CONFL-001 | Detecting Contradictions in Texts | de Marneffe et al. | 2008 | ACL | Identifying contradictory statements in multiple documents | Linguistic patterns, entailment checking, negation analysis | Published | SpaceDoc detects conflicts; relies on LLM and explicit evidence comparison |
| CONFL-002 | Natural Language Inference and Contradiction Detection | Dagan et al. | 2006 | ACL | Determining whether statements logically contradict | Textual entailment (TE) framework | Published | Foundational; SpaceDoc uses as part of verification |
| VERIFY-001 | Fact Extraction and Verification (FEVER) | Thorne et al. | 2018 | ACL | Automating fact-checking across multiple documents | Claim detection, evidence retrieval, label prediction (SUPPORTS/REFUTES/NOT_ENOUGH_INFO) | Published Dataset/Task | SpaceDoc verification borrows from FEVER structure; adds revision context |
| VERIFY-002 | Claim Verification in Scientific Documents | Wadden et al. | 2020 | ACL | Fact-checking in long-form scientific papers | Multi-hop evidence retrieval + verification | Published | Related; SpaceDoc adds revision applicability |

---

## E. Query Planning and Adaptive Retrieval

| ID | Title | Publication | Date | Source | Problem | Mechanism | Status | Overlap with SpaceDoc |
|------|-------|-------------|------|--------|---------|-----------|--------|----------------------|
| QUERY-001 | Query Reformulation and Expansion Techniques | Ruthven & Lalmas | 2003 | Information Processing & Management | Improving retrieval recall through query expansion | Synonym expansion, related term addition, pseudo-relevance feedback | Published | Standard technique; SpaceDoc uses for query understanding |
| QUERY-002 | Complex Query Understanding in Multi-Stage Retrieval | Narasimhan et al. | 2022 | SIGIR | Breaking down complex queries into sub-components | Query decomposition → sub-query routing → result fusion | Published | Related; SpaceDoc uses simpler routing without learned decomposition |
| SELF-RAG-001 | Self-RAG: Learning to Retrieve, Generate, and Reflect for Knowledge-Intensive Tasks | Asano et al. | 2023 | arXiv:2307.10288 | Standard RAG always retrieves; inefficient for simple queries | Model learns when to retrieve, which docs are relevant, whether generation is sufficient | Published | **HIGHLY RELEVANT** — Self-RAG uses learned routing; SpaceDoc uses rule-based routing with document revisions |
| ADAPT-001 | Adaptive Retrieval: When to Retrieve and What to Retrieve | Gao et al. | 2023 | arXiv:2310.06554 | Fixed retrieval strategy inefficient across query types | Confidence-based thresholding for retrieval necessity | Published | Related; SpaceDoc combines deterministic routing with revision metadata |
| ROUTER-001 | Routing Language Models for Query-Focused Summarization | Goldsack et al. | 2023 | ACL | Selecting appropriate retrieval or generation strategy | Learned router predicting task outcome | Published | Conceptually similar; SpaceDoc applies to evidence routing |

---

## F. Specification and Requirement Tracing

| ID | Title | Publication | Date | Source | Problem | Mechanism | Status | Overlap with SpaceDoc |
|------|-------|-------------|------|--------|---------|-----------|--------|----------------------|
| TRACE-001 | Requirement Traceability in Software Engineering | Gotel & Finkelstein | 1994 | ACM Software Engineering Notes | Linking requirements to design, implementation, tests | Traceability matrices, bidirectional links, change impact tracking | Published | SpaceDoc adapts requirement tracing to documents; adds RAG integration |
| TRACE-002 | Change Impact Analysis: A Systematic Review | Bohner & Arnold | 1996 | Journal of Systems and Software | Predicting effects of code changes | Dependency analysis, transitive impact calculation | Published | Parallel work; applies to documents/requirements |
| TRACE-003 | Automated Requirements Traceability Using NLP | Gervasi & Zowghi | 2005 | ACM TOSEM | Manual traceability expensive; automated approaches | Information retrieval, semantic similarity, clustering | Published | SpaceDoc uses similar IR techniques; adds revision tracking |
| IMPACT-001 | Software Artifact Dependency Analysis | Egyed & Grünbacher | 2002 | ICSE | Tracking dependencies between system artifacts | Static analysis, dynamic analysis, manual traceability | Published | Related; SpaceDoc applies to documentation, not code |

---

## G. Evidence-Based Question Answering and Citation

| ID | Title | Publication | Date | Source | Problem | Mechanism | Status | Overlap with SpaceDoc |
|------|-------|-------------|------|--------|---------|-----------|--------|----------------------|
| QA-001 | Open-Domain Question Answering | Chen et al. | 2017 | ACL | QA without pre-defined knowledge bases | Dense retriever + reader (span extraction) | Published | Foundational; SpaceDoc builds on this with revision filtering |
| QA-002 | Multi-Hop Question Answering | Yang et al. | 2018 | EMNLP | Questions requiring reasoning across multiple documents | Multi-stage retrieval with reasoning | Published | SpaceDoc implements dependency-guided expansion; less sophisticated reasoning |
| CITE-001 | Attributed Question Answering: Evaluation and Modeling for Attributed QA | Kasner et al. | 2021 | arXiv:2212.08037 | Generated answers should cite sources | Citation extraction, mapping to evidence, validation | Published | SpaceDoc implements citation mapping; adds revision validation |
| CITE-002 | Evaluating Attribution in Dialogue Systems | Gabriel et al. | 2021 | ACL | Assessing whether generated text is grounded in provided context | Citation parsing, evidence matching, faithfulness metrics | Published | Related; SpaceDoc adds revision applicability checks |
| GROUND-001 | Groundedness in Neural Language Models: Evidence from Coronavirus Infodemic | Zhang et al. | 2020 | arXiv:2011.12503 | Models generate plausible-sounding but unsupported claims | Fact verification, evidence grounding evaluation | Published | Motivates SpaceDoc evidence-gating; not novel mechanism |

---

## H. Technical Document and Patent Analysis

| ID | Title | Publication | Date | Source | Problem | Mechanism | Status | Overlap with SpaceDoc |
|------|-------|-------------|------|--------|---------|-----------|--------|----------------------|
| PATENT-001 | Patent Retrieval and Prior Art Search | Commercial Patent Search Tools (Espacenet, Google Patents) | Ongoing | Patent Office Infrastructure | Finding prior art efficiently | Full-text search, classification-based filtering | Established Practice | SpaceDoc applies similar retrieval to technical documents |
| PATENT-002 | Graph-Based Patent Analysis | USPTO, WIPO Research | 2020–present | Patent Office Research | Understanding patent landscape and relationships | Patent citation graphs, technology clustering | Emerging Practice | Related; SpaceDoc implements document relationship graph |
| SPEC-001 | Automated Specification Analysis | Knoblock et al. | 2018 | IEEE TSE | Extracting structured information from unstructured specs | Named entity recognition, relation extraction, semantic parsing | Published | SpaceDoc extracts requirements/entities; adds document versioning |
| SPEC-002 | Software Requirements Consistency Checking | Egyed et al. | 2007 | FSE | Detecting conflicts in specifications | Formal methods, consistency checking, contradiction detection | Published | Related; SpaceDoc uses lighter-weight heuristics |

---

## I. Evidence Gating and Safe Generation

| ID | Title | Publication | Date | Source | Problem | Mechanism | Status | Overlap with SpaceDoc |
|------|-------|-------------|------|--------|---------|-----------|--------|----------------------|
| GATE-001 | When Not to Trust Language Models: Investigating Semantic Drift in Out-of-Domain Generalization | Min et al. | 2022 | ACL | Models generate plausible but incorrect answers outside training data | Evidence-constrained generation, uncertainty quantification | Published | Motivates SpaceDoc evidence gating |
| GATE-002 | Instruction-Hierarchy Training for In-Context RAG | Khattab et al. | 2024 | arXiv | Teaching models to follow evidence-based instructions | In-context learning with explicit evidence constraints | Published | Related; SpaceDoc uses prompt engineering not learned instruction-following |
| GUARDRAIL-001 | LLM Output Guardrails: State of the Art and Opportunities | Community Survey | 2024 | arXiv | Constraining LLM outputs for safety and consistency | Prompt engineering, output validation, constrained decoding | Emerging Practice | SpaceDoc uses evidence gating as guardrail; not primary contribution |

---

## J. Domain-Specific RAG (Space, Aerospace)

| ID | Title | Publication | Date | Source | Problem | Mechanism | Status | Overlap with SpaceDoc |
|------|-------|-------------|------|--------|---------|-----------|--------|----------------------|
| DOMAIN-001 | RAG for Technical Documentation (Enterprise Use Cases) | Commercial Implementation Examples | 2023–present | Product Case Studies | Making internal technical docs searchable by employees | Standard RAG + semantic search + access controls | Established Practice | SpaceDoc applies RAG to space domain; adds revision awareness |
| DOMAIN-002 | Knowledge Management in Aerospace | Boeing, Lockheed Martin, ESA reports | 2010–2020 | Industry Reports | Managing complex, interdependent aerospace documentation | Document versioning, configuration management, traceability | Industry Practice | SpaceDoc formalizes and automates aspects of aerospace doc management |

---

## K. Experimental Evaluation of RAG Systems

| ID | Title | Publication | Date | Source | Problem | Mechanism | Status | Overlap with SpaceDoc |
|------|-------|-------------|------|--------|---------|-----------|--------|----------------------|
| EVAL-001 | RAGAS: Automated Evaluation of Retrieval Augmented Generation | Shahul et al. | 2023 | arXiv:2309.15217 | Evaluating RAG without manual annotation | Metrics for faithfulness, context relevance, answer relevance | Published | SpaceDoc implements similar evaluation metrics |
| EVAL-002 | On the Evaluation of Open-Domain QA Systems | Chen et al. | 2018 | EMNLP | Systematic evaluation of QA systems | Recall@k, F1, exact match | Published | Standard evaluation; SpaceDoc uses these metrics |
| ABLATION-001 | Understanding the Effects of Design Choices in RAG | Community Practice | 2023–present | Blog Posts, Papers | Isolating impact of individual components | Systematic ablation experiments | Emerging Practice | SpaceDoc uses ablation to validate routing contribution |

---

## Summary of Findings

### Established and Published Techniques Used by SpaceDoc

1. **Dense Retrieval** (RAG-002) — Not novel
2. **Lexical Retrieval** (RAG-003, RAG-004) — Not novel
3. **Hybrid Ranking** (HYBRFD-001) — Established
4. **Graph-Based Relationships** (GRAPH-001, GRAPH-RAG-001) — Published approaches
5. **Citation and Evidence Mapping** (CITE-001, CITE-002) — Published
6. **Evidence Gating** (GATE-001) — Published concept
7. **Evaluation Metrics** (EVAL-001, EVAL-002) — Established
8. **Change Impact Analysis** (TRACE-002, VERSION-003) — Established

### Potentially Novel Aspects (Requires Validation)

1. **Query-Conditioned Routing Specific to Document Revisions** 
   - Self-RAG (SELF-RAG-001) learns routing decisions but does not focus on revision applicability.
   - SpaceDoc combines deterministic routing rules with document revision metadata.
   - **Status:** Overlaps with Self-RAG; question whether revision-specific routing is distinct.

2. **Integrated Revision Applicability Checking in Retrieval Pipeline**
   - Version-aware retrieval exists (VERSION-004) but typically filters to a single version.
   - SpaceDoc combines evidence from multiple revisions while tracking applicability.
   - **Status:** Minor differentiation; not clearly novel.

3. **Explicit Conflict Detection and Transparent Reporting**
   - Contradiction detection exists (CONFL-001, CONFL-002) but typically in QA contexts.
   - Fact verification frameworks exist (VERIFY-001).
   - SpaceDoc adds conflict detection with revision applicability context.
   - **Status:** Incremental; combines published techniques.

4. **Dependency-Guided Evidence Expansion for Technical Documents**
   - Multi-hop QA (QA-002) exists but uses different mechanisms.
   - Requirement traceability (TRACE-001) is established in software engineering.
   - SpaceDoc applies dependency-guided retrieval to document RAG.
   - **Status:** Adaptation of existing ideas to document domain; unclear novelty.

### Assessment

**Current Status:** The proposed mechanism **appears to combine published techniques** (graph-based retrieval, query planning, version control, conflict detection) in a new context (technical document RAG with revision awareness). 

**Potential Novelty Claim:** A **specific, implementation-validated combination** of:
- Deterministic query-conditioned routing rules that consider document revision applicability
- Integrated conflict detection with explicit revision comparison
- Dependency-guided expansion with explicit traversal limits
- Evidence-gated generation that acknowledges revision uncertainty

**But:** This is **NOT established as novel** without:
1. Empirical validation showing measurable improvements over baselines
2. Demonstration that improvements come from the routing mechanism, not standard techniques
3. Evidence that no closely similar mechanism has been previously published
4. Patent counsel review for eligibility and scope

---

## Recommendations for Further Investigation

1. **Search Recent Patent Filings (2024-2026)** — Review latest USPTO, WIPO, and international filings for adaptive RAG routing and version-aware retrieval systems.

2. **Review Remaining arXiv/ACL Papers (2024-2026)** — New research on retrieval planning and document versioning published recently.

3. **Check for Academic Implementations** — GitHub searches for "revision-aware RAG," "graph-based retrieval," "evidence routing."

4. **Consult Patent Counsel** — Before filing, legal review of claim scope, prior art references, and inventorship.

5. **Execute Ablation Experiments** — Run the planned evaluation (Phase 6) to determine whether the routing controller uniquely contributes improvements.

---

**Next Steps:** Continue implementation while maintaining detailed design and experiment logs. Document all design decisions, measured results, and potential prior art overlaps. Reserve patent filing decision for after experimental validation and legal review.

**Last Updated:** 2026-10-20
