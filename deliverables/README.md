# Assignment 2 — Submission Folder Contents

Place these files in your OneDrive / shared folder alongside the demo video:

| File | Purpose |
|------|---------|
| `ASSIGNMENT-2-WRITEUP.docx` | Problem solution write-up (includes architecture diagram) |
| `architecture-diagram.png` | Standalone diagram (also embedded in write-up) |
| `SAMPLE-USE-CASE-OUTPUTS.docx` | Recorded outputs for UC1–UC6 (Word) |
| `SAMPLE-USE-CASE-OUTPUTS.md` | Same content (Markdown, in repo) |
| `raw-responses/uc1.json` … `uc6.json` | Live API JSON evidence |
| Demo video (MP4) | 8–10 min walkthrough — see `DEMO-SCRIPT.md` |

## Refresh outputs (before submission)

From repository root:

```bash
docker compose up -d
python3 scripts/refresh_analytics_outputs.py
./scripts/verify_analytics_gateway.sh   # 6/6 gateway checks
python3 scripts/generate_architecture_diagram.py
python3 scripts/generate_assignment2_writeup.py
```

Regenerates `SAMPLE-USE-CASE-OUTPUTS.md`, `.docx`, `raw-responses/*.json`, and the write-up with diagram from live API.

## Email

Use `SUBMISSION-EMAIL-TEMPLATE.md` and include GitHub repo link + this folder link.
