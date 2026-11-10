# The Horniness Index — Kafka edition

A satirical leaderboard scoring public-company CEOs on the risk that their
relationship headlines blow up shareholder value. The standalone edition
(`ci/horniness-index.py`) fetches and scores in one script; this package is
the streaming edition Navid asked for: many sources in, Kafka in the middle,
an aggregated leaderboard out.

```
 producers (one per source)                dedupe            scorer           aggregator
 ┌────────────────────┐
 │ google-news (RSS)  │──┐
 ├────────────────────┤  │   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
 │ gdelt (global API) │──┼──▶│ headlines.raw│──▶│headlines.clean│──▶│  ceo.scores  │──▶ index.html
 ├────────────────────┤  │   └──────────────┘   └──────────────┘   └──────────────┘    data.json
 │ outlet-rss (BBC…)  │──┘    keyed by CEO       syndicated copies   OpenAI via the        │
 └────────────────────┘                         collapsed to reach  repo's OPENAI_API_KEY   └─▶ GitHub Pages
                                                                          + heuristic fallback   /horniness-index/
```

**Why Kafka for a meme?** Three real properties, not just theatre:

1. **Many sources, one stream.** Each producer polls on its own and publishes
   to `headlines.raw`, keyed by CEO name, so a CEO's headlines stay together
   and in order no matter which source found them.
2. **Dedupe as a stream step.** One affair syndicated to 30 outlets is one
   story with reach 30, not 30 stories. The dedupe consumer collapses copies
   (normalised titles + token-overlap) and keeps the reach as a capped
   scoring signal.
3. **Replay.** Re-run the scorer over the retained topics with a new rubric
   and the leaderboard re-grades history without re-fetching a single page.

**How it runs**

- *CI (the live page):* the `horniness` job in `.github/workflows/ci.yml`
  starts a single-node Redpanda container (Kafka protocol, no ZooKeeper),
  runs `run_pipeline.py`, and hands the site to the Pages `report` job. If
  the pipeline fails for any reason the job falls back to the standalone
  script — the meme must go on. The broker exists only for the job's
  lifetime; the topics' output (the site + `data.json`) is what persists.
- *Locally:* `docker compose -f ci/horniness/docker-compose.yml up --build`
  runs the identical stages against a persistent Redpanda.
- *Stage logic test without a broker:* `--transport file` swaps Kafka for
  JSONL topic files (`run_pipeline.py --transport file`). Same producers,
  dedupe, scorer and aggregator code; only the transport differs.

**Topics**

| Topic | Key | Value |
|---|---|---|
| `headlines.raw` | CEO name | one record per headline found, with `via` = which producer |
| `headlines.clean` | CEO name | one record per deduplicated story, with `reach` + `also_in` |
| `ceo.scores` | CEO name | score, verdict, factors, story/placement counts |

Scoring uses the rubric in `ci/horniness-index.py`: allegations in headlines
are treated as allegations, nothing is invented, and the score measures
*headline risk to shareholder value* — affairs, workplace relationships,
board investigations, resignations, viral incidents, intelligence angles,
share-price moves. Satire, not investment advice.
