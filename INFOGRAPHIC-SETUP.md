# Infographic automation setup

This extends the existing `reviewStatus: approved` workflow. It does not replace daily PubMed discovery or editorial review.

1. Upload these files to the live GitHub repository, preserving their folders.
2. Under **Settings → Secrets and variables → Actions**, add the secret `GEMINI_API_KEY`.
3. Add repository variables:
   - `GEMINI_IMAGE_MODEL`: `gemini-3.1-flash-image`
   - `GEMINI_QA_MODEL`: `gemini-3.6-flash`
   - `INFOGRAPHIC_VARIANTS`: `3`
4. Open **Actions → Generate approved infographics → Run workflow**.
5. Keep **dry run** enabled first. Optionally enter one approved PMID.
6. Review the log. Then rerun with dry run disabled.

Successful packages are stored under `infographics/YYYY/MM/article-pmid/`. Failed QA candidates remain in the same folder with `qa_report.json`, but no `infographic_master.png` is created. `data/infographic-manifest.json` prevents successful articles from being regenerated unless their locked source content changes.

The generator treats the current records as abstract-only. It must not infer treatment recommendations. For publication-grade clinical summaries, add editor-verified takeaways to `practicalTakeaways` before approval.

Current Gemini request formats and model examples were checked against Google's official image-generation documentation in August 2026. Model identifiers can change; verify them before future migrations.
