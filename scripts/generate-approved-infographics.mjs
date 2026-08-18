import fs from "node:fs/promises";
import path from "node:path";
import crypto from "node:crypto";

const UPDATES = path.resolve("data/updates.json");
const MANIFEST = path.resolve("data/infographic-manifest.json");
const OUTPUT_ROOT = path.resolve("infographics");
const API_KEY = process.env.GEMINI_API_KEY;
const IMAGE_MODEL = process.env.GEMINI_IMAGE_MODEL || "gemini-3.1-flash-image";
const QA_MODEL = process.env.GEMINI_QA_MODEL || "gemini-3.6-flash";
const VARIANTS = Math.max(1, Math.min(3, Number(process.env.INFOGRAPHIC_VARIANTS || 3)));
const DRY_RUN = /^(1|true|yes)$/i.test(process.env.DRY_RUN || "");
const TEST_PMID = (process.env.TEST_PMID || "").replace(/^pmid-/, "");

function stable(value) {
  if (Array.isArray(value)) return value.map(stable);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.keys(value).sort().map((key) => [key, stable(value[key])]));
  }
  return value;
}

function hash(value) {
  return crypto.createHash("sha256").update(JSON.stringify(stable(value))).digest("hex");
}

function slug(value) {
  return value.toLowerCase().normalize("NFKD").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "").slice(0, 72) || "article";
}

function lockedEvidence(item) {
  return {
    id: item.id,
    pmid: item.pmid || null,
    doi: item.doi || null,
    title: item.title,
    category: item.category || null,
    article_type: item.status || null,
    journal: item.source || null,
    date: item.date || null,
    source_url: item.sourceUrl || null,
    source_scope: "abstract_only",
    approved_summary: item.summary || null,
    approved_details: item.fullDetails || null,
    approved_practical_takeaways: Array.isArray(item.practicalTakeaways) ? item.practicalTakeaways : [],
    reference: item.references || null,
    safety_note: "Use only these fields. Do not infer recommendations, doses, thresholds, effect sizes, or certainty grades. Preserve numbers exactly.",
  };
}

function infographicPrompt(evidence, variant) {
  const styles = ["medical sketchnote notebook", "clinical whiteboard doodle", "hand-drawn visual abstract"];
  return `Create a polished landscape 16:9 GastroUpdates medical infographic in a ${styles[variant % styles.length]} style.

The image will later be manually separated for animation. Use 8–12 clearly separated zones arranged across a widescreen canvas, generous whitespace, discrete doodles, minimal overlap, dark navy ink, teal and one warm accent on an off-white background. Use Comic Sans MS for all visible text, with large highly legible lettering for medical facts and references. Do not imitate another handwriting font. No element may cross into an adjacent zone.

Use ONLY this locked JSON evidence:
${JSON.stringify(evidence, null, 2)}

Required hierarchy: title; why it matters; study/article type; population or scope when explicitly available; 3–5 supported findings; cautious clinical implication only if supplied; limitations/source-scope warning; reference. If a field is unavailable, omit it. Do not invent or alter any number, dose, endpoint, sample size, confidence interval, recommendation strength, PMID, or DOI. Do not add a logo. Output the finished infographic only.`;
}

async function gemini(model, body) {
  const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "x-goog-api-key": API_KEY },
    body: JSON.stringify(body),
  });
  if (!response.ok) throw new Error(`Gemini ${model} failed (${response.status}): ${await response.text()}`);
  return response.json();
}

function parts(response) {
  return response.candidates?.[0]?.content?.parts || [];
}

async function render(prompt) {
  const response = await gemini(IMAGE_MODEL, {
    contents: [{ role: "user", parts: [{ text: prompt }] }],
    generationConfig: {
      responseModalities: ["TEXT", "IMAGE"],
    responseFormat: {
  image: {
    aspectRatio: "ASPECT_RATIO_SIXTEEN_BY_NINE",
    imageSize: "IMAGE_SIZE_TWO_K",
  },
},
    },
  });
  const image = parts(response).find((part) => part.inlineData?.data && /^image\//.test(part.inlineData.mimeType || ""));
  if (!image) throw new Error("Image model returned no image.");
  return { bytes: Buffer.from(image.inlineData.data, "base64"), mimeType: image.inlineData.mimeType || "image/png" };
}

async function qaImage(evidence, image) {
  const prompt = `Act as a strict medical infographic proofreader. Compare every visible claim and number with the locked evidence. Fail any invented, contradicted, or altered medical fact; wrong PMID/DOI; clipped key text; or unreadable key result. An omission is not a contradiction. Return JSON only with: publishable (boolean), critical_errors (string array), omissions (string array), legibility (integer 1-5), extractability (integer 1-5). publishable can be true only with zero critical errors.\n\nLOCKED EVIDENCE:\n${JSON.stringify(evidence, null, 2)}`;
  const response = await gemini(QA_MODEL, {
    contents: [{ role: "user", parts: [{ text: prompt }, { inlineData: { mimeType: image.mimeType, data: image.bytes.toString("base64") } }] }],
    generationConfig: { responseMimeType: "application/json" },
  });
  const text = parts(response).map((part) => part.text || "").join("").trim();
  const report = JSON.parse(text);
  report.publishable = report.publishable === true && Array.isArray(report.critical_errors) && report.critical_errors.length === 0;
  return report;
}

async function readJson(file, fallback) {
  try { return JSON.parse(await fs.readFile(file, "utf8")); } catch (error) { if (error.code === "ENOENT") return fallback; throw error; }
}

async function main() {
  const updates = await readJson(UPDATES, { items: [] });
  const manifest = await readJson(MANIFEST, { version: 1, items: {} });
  const approved = (updates.items || []).filter((item) => item.reviewStatus === "approved" && (!TEST_PMID || item.pmid === TEST_PMID));
  let processed = 0;

  for (const item of approved) {
    const evidence = lockedEvidence(item);
    const sourceHash = hash(evidence);
    if (!TEST_PMID && manifest.items[item.id]?.sourceHash === sourceHash && manifest.items[item.id]?.status === "published") continue;
    const dated = new Date().toISOString().slice(0, 7).replace("-", "/");
    const relative = path.join(dated, `${slug(item.title)}-${item.pmid || slug(item.id)}`);
    const directory = path.join(OUTPUT_ROOT, relative);

    if (DRY_RUN) {
      console.log(`[dry-run] ${item.id}: would generate ${VARIANTS} variant(s) in ${relative}`);
      continue;
    }
    if (!API_KEY) throw new Error("GEMINI_API_KEY is required outside dry-run mode.");

    await fs.mkdir(path.join(directory, "candidates"), { recursive: true });
    await fs.writeFile(path.join(directory, "locked_evidence.json"), `${JSON.stringify(evidence, null, 2)}\n`);
    const reports = [];
    let winner = null;
    for (let index = 0; index < VARIANTS; index += 1) {
      try {
        const image = await render(infographicPrompt(evidence, index));
        const candidate = path.join(directory, "candidates", `variant-${index + 1}.png`);
        await fs.writeFile(candidate, image.bytes);
        const report = await qaImage(evidence, image);
        reports.push({ variant: index + 1, ...report });
        if (!winner && report.publishable && report.legibility >= 4 && report.extractability >= 3) winner = { image, variant: index + 1 };
      } catch (error) {
        reports.push({ variant: index + 1, publishable: false, critical_errors: [error.message], omissions: [], legibility: 1, extractability: 1 });
      }
    }
    await fs.writeFile(path.join(directory, "qa_report.json"), `${JSON.stringify({ reports }, null, 2)}\n`);
    if (winner) {
      await fs.writeFile(path.join(directory, "infographic_master.png"), winner.image.bytes);
      await fs.writeFile(path.join(directory, "video_brief.md"), `# ${item.title}\n\n1. Title and clinical context\n2. Population/scope\n3. Key evidence\n4. Limitations\n5. Cautious take-home message\n\nSource: ${item.references || item.sourceUrl || "See locked evidence"}\n`);
      manifest.items[item.id] = { sourceHash, status: "published", generatedAt: new Date().toISOString(), directory: relative.replaceAll("\\", "/"), selectedVariant: winner.variant };
    } else {
      manifest.items[item.id] = { sourceHash, status: "qa_failed", attemptedAt: new Date().toISOString(), directory: relative.replaceAll("\\", "/") };
    }
    await fs.writeFile(MANIFEST, `${JSON.stringify(manifest, null, 2)}\n`);
    processed += 1;
  }
  console.log(`${DRY_RUN ? "Planned" : "Processed"} ${processed || approved.length} approved article(s).`);
}

main().catch((error) => { console.error(error); process.exitCode = 1; });
