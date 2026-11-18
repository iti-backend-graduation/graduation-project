# Dev B Evaluation Report

The evaluation dataset contains 15 cases covering supported retrieval, citations,
out-of-scope questions, ambiguity, prompt injection, and refusal behavior.

The runner is intentionally adapter-based: it accepts the retrieval/answer
callback from the copilot module and calculates retrieval hit rate, groundedness,
and refusal correctness without calling a live model from tests. The adapter is
wired when the retrieval/copilot module is merged.

| Metric | Result |
| --- | --- |
| Cases | 15 |
| Retrieval hit rate | Pending copilot adapter |
| Groundedness | Pending copilot adapter |
| Refusal correctness | Pending copilot adapter |

Known limitation: Markdown sources expose section citations rather than physical
pages. PDF sources expose page numbers through `PdfExtractor`.
