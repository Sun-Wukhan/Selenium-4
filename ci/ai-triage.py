#!/usr/bin/env python3
"""AI failure triage for CI runs.

Reads the Surefire reports left by a failed test run, extracts the failing
tests, and asks an LLM for a concise root-cause summary per failure. The
result is written as Markdown (the workflow appends it to the run's step
summary and uploads it as an artifact).

Provider selection, in order:
  1. ANTHROPIC_API_KEY set  -> Anthropic Messages API
  2. OPENAI_API_KEY set     -> OpenAI Chat Completions API
  3. neither                -> no AI call; the report carries the raw
                               failure excerpts plus how to enable triage

The model can be overridden with AI_TRIAGE_MODEL. This script never fails
the build: any error (network, bad response, missing reports) degrades to
the raw-excerpt report and exit code 0.

Usage: python3 ci/ai-triage.py [reports_dir] [output_file]
"""

import glob
import json
import os
import sys
import urllib.request
import urllib.error

MAX_FAILURE_CHARS = 24_000
MAX_LINES_PER_FAILURE = 30

PROMPT_TEMPLATE = """You are triaging failures from a Selenium/TestNG UI test suite \
for the Weather Shopper demo shop (weathershopper.pythonanywhere.com). The shop \
re-randomises its product catalogue on every page load and fails about 5% of \
payments at random by design.

For each failure below, write a short Markdown section with:
- a one-line likely root cause
- a category: TEST BUG, SITE BEHAVIOUR, ENVIRONMENT, or UNCLEAR
- the evidence from the log that supports it
- one concrete next step

Be concise and specific. Do not invent details that are not in the excerpts.

{failures}
"""


def extract_failures(reports_dir):
    """Return a list of failure excerpts from Surefire .txt reports."""
    excerpts = []
    pattern = os.path.join(reports_dir, "**", "*.txt")
    for path in sorted(glob.glob(pattern, recursive=True)):
        try:
            lines = open(path, encoding="utf-8", errors="replace").read().splitlines()
        except OSError:
            continue
        current = None
        for line in lines:
            if "<<< FAILURE!" in line or "<<< ERROR!" in line:
                if "-- Time elapsed" in line:  # start of one failed test's block
                    header = line.strip()
                    # The same failure appears once per browser job; keep the
                    # first occurrence of each failed test.
                    if any(block[0].split(" -- ")[0] == header.split(" -- ")[0]
                           for block in excerpts):
                        current = None
                        continue
                    current = [header]
                    excerpts.append(current)
                continue
            if current is not None:
                if line.strip() == "" and len(current) > 3:
                    current = None
                    continue
                if len(current) <= MAX_LINES_PER_FAILURE:
                    current.append(line.rstrip())
    return ["\n".join(block) for block in excerpts]


def call_anthropic(prompt):
    body = json.dumps({
        "model": os.environ.get("AI_TRIAGE_MODEL") or "claude-sonnet-4-5",
        "max_tokens": 1500,
        "messages": [{"role": "user", "content": prompt}],
    }).encode()
    request = urllib.request.Request(
        "https://api.anthropic.com/v1/messages",
        data=body,
        headers={
            "content-type": "application/json",
            "x-api-key": os.environ["ANTHROPIC_API_KEY"],
            "anthropic-version": "2023-06-01",
        },
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        payload = json.load(response)
    return payload["content"][0]["text"]


def call_openai(prompt):
    body = json.dumps({
        "model": os.environ.get("AI_TRIAGE_MODEL") or "gpt-4o-mini",
        "messages": [{"role": "user", "content": prompt}],
    }).encode()
    request = urllib.request.Request(
        "https://api.openai.com/v1/chat/completions",
        data=body,
        headers={
            "content-type": "application/json",
            "authorization": "Bearer " + os.environ["OPENAI_API_KEY"],
        },
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        payload = json.load(response)
    return payload["choices"][0]["message"]["content"]


def raw_report(failures, note):
    parts = ["## Failure triage", "", note, ""]
    for failure in failures:
        parts.append("```")
        parts.append(failure)
        parts.append("```")
        parts.append("")
    return "\n".join(parts)


def main():
    reports_dir = sys.argv[1] if len(sys.argv) > 1 else "client/target/surefire-reports"
    output_file = sys.argv[2] if len(sys.argv) > 2 else "ai-triage.md"

    failures = extract_failures(reports_dir)
    if not failures:
        report = "## Failure triage\n\nNo test failures found in the Surefire reports.\n"
    else:
        joined = "\n\n---\n\n".join(failures)[:MAX_FAILURE_CHARS]
        prompt = PROMPT_TEMPLATE.format(failures=joined)
        summary = None
        provider = None
        error = None
        try:
            if os.environ.get("ANTHROPIC_API_KEY"):
                summary, provider = call_anthropic(prompt), "Anthropic"
            elif os.environ.get("OPENAI_API_KEY"):
                summary, provider = call_openai(prompt), "OpenAI"
        except Exception as exc:  # never fail the build over triage
            error = type(exc).__name__

        if summary:
            report = f"## AI failure triage ({provider})\n\n{summary}\n"
        elif error:
            report = raw_report(
                failures,
                f"_AI triage was requested but the provider call failed ({error}); "
                "raw failure excerpts follow._",
            )
        else:
            report = raw_report(
                failures,
                "_AI triage is not enabled: add an `ANTHROPIC_API_KEY` or "
                "`OPENAI_API_KEY` repository secret and future failures will get "
                "an AI root-cause summary here. Raw failure excerpts follow._",
            )

    with open(output_file, "w", encoding="utf-8") as out:
        out.write(report)
    print(f"Wrote {output_file} from {len(failures)} failure(s) in {reports_dir}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
