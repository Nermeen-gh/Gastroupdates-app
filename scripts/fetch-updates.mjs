import fs from "node:fs/promises";
import path from "node:path";

const OUTPUT = path.resolve("data/updates.json");
const RETMAX = 30;
const QUERY = [
  "(",
  "digestive system diseases[MeSH Terms]",
  "OR gastrointestinal endoscopy[MeSH Terms]",
  "OR liver diseases[MeSH Terms]",
  "OR inflammatory bowel diseases[MeSH Terms]",
  "OR gastroenterology[Title]",
  "OR hepatology[Title]",
  "OR endoscopy[Title]",
  "OR cirrhosis[Title]",
  "OR liver[Title]",
  "OR inflammatory bowel[Title]",
  "OR Crohn[Title]",
  "OR ulcerative colitis[Title]",
  "OR colonoscopy[Title]",
  ")",
  "AND (guideline[Publication Type] OR practice guideline[Publication Type]",
  "OR meta-analysis[Publication Type] OR systematic review[Publication Type]",
  "OR randomized controlled trial[Publication Type] OR clinical trial, phase iii[Publication Type])",
  "AND hasabstract[text]",
].join(" ");

const entityMap = { amp: "&", lt: "<", gt: ">", quot: '"', apos: "'", "#39": "'" };

function decode(value = "") {
  return value
    .replace(/<[^>]+>/g, " ")
    .replace(/&#(x?[0-9a-f]+);/gi, (_, code) =>
      String.fromCodePoint(parseInt(code.replace(/^x/i, ""), /^x/i.test(code) ? 16 : 10))
    )
    .replace(/&([a-z]+|#39);/gi, (_, key) => entityMap[key.toLowerCase()] || `&${key};`)
    .replace(/\s+/g, " ")
    .trim();
}

function textBetween(xml, tag) {
  return decode(xml.match(new RegExp(`<${tag}[^>]*>([\\s\\S]*?)</${tag}>`, "i"))?.[1] || "");
}

function classify(text) {
  const lower = text.toLowerCase();
  if (/(hepat|liver|cirrho|portal hypertension|masld|mash|steato|biliary|cholangi|hcv|hbv)/.test(lower)) {
    return "Hepatology";
  }
  if (/(endoscop|colonoscopy|ercp|eus|poem|esd|polyp|adenoma|barrett)/.test(lower)) {
    return "Endoscopy";
  }
  return "Gastroenterology";
}

function evidenceStatus(text) {
  const lower = text.toLowerCase();
  if (/(guideline|guidance|consensus|recommendation)/.test(lower)) return "Guideline candidate";
  if (/(randomized|randomised|clinical trial|phase ii|phase iii)/.test(lower)) return "Clinical trial";
  if (/(systematic review|meta-analysis)/.test(lower)) return "Evidence review";
  return "New publication";
}

function xmlArticles(xml) {
  return [...xml.matchAll(/<PubmedArticle>([\s\S]*?)<\/PubmedArticle>/gi)].map((match) => {
    const article = match[1];
    const pmid = textBetween(article, "PMID");
    const title = textBetween(article, "ArticleTitle");
    const abstractParts = [...article.matchAll(/<AbstractText[^>]*>([\s\S]*?)<\/AbstractText>/gi)]
      .map((part) => decode(part[1]))
      .filter(Boolean);
    const abstract = abstractParts.join(" ");
    const journal = textBetween(article, "Title") || textBetween(article, "ISOAbbreviation");
    const year = textBetween(article, "Year");
    const month = textBetween(article, "Month");
    const day = textBetween(article, "Day");
    const doi = decode(article.match(/<ArticleId IdType="doi">([\s\S]*?)<\/ArticleId>/i)?.[1] || "");
    const published = [year, month, day].filter(Boolean).join(" ");
    return {
      id: `pmid-${pmid}`,
      pmid,
      doi,
      title,
      category: classify(`${title} ${abstract}`),
      source: journal || "PubMed",
      summary: abstract.length > 420 ? `${abstract.slice(0, 417).trim()}…` : abstract,
      fullDetails: abstract,
      practicalTakeaways: [],
      date: published || "Recently indexed",
      status: evidenceStatus(`${title} ${abstract}`),
      references: [journal, published, pmid ? `PMID: ${pmid}` : "", doi ? `DOI: ${doi}` : ""]
        .filter(Boolean)
        .join(" · "),
      sourceUrl: `https://pubmed.ncbi.nlm.nih.gov/${pmid}/`,
      reviewStatus: "pending",
      discoveredAt: new Date().toISOString(),
    };
  }).filter((item) => item.pmid && item.title && item.summary);
}

async function ncbi(endpoint, params) {
  const url = new URL(`https://eutils.ncbi.nlm.nih.gov/entrez/eutils/${endpoint}`);
  Object.entries(params).forEach(([key, value]) => url.searchParams.set(key, value));
  url.searchParams.set("tool", "gastro_updates_daily");
  if (process.env.NCBI_EMAIL) url.searchParams.set("email", process.env.NCBI_EMAIL);
  if (process.env.NCBI_API_KEY) url.searchParams.set("api_key", process.env.NCBI_API_KEY);
  const response = await fetch(url, { headers: { "User-Agent": "GastroUpdates/1.0" } });
  if (!response.ok) throw new Error(`NCBI request failed: ${response.status}`);
  return response;
}

async function main() {
  const existing = JSON.parse(await fs.readFile(OUTPUT, "utf8"));
  const search = await ncbi("esearch.fcgi", {
    db: "pubmed",
    term: QUERY,
    datetype: "edat",
    reldate: "2",
    retmax: String(RETMAX),
    retmode: "json",
    sort: "pub date",
  });
  const ids = (await search.json()).esearchresult?.idlist || [];
  let candidates = [];
  if (ids.length) {
    const records = await ncbi("efetch.fcgi", {
      db: "pubmed",
      id: ids.join(","),
      retmode: "xml",
    });
    candidates = xmlArticles(await records.text());
  }

  const known = new Set((existing.items || []).map((item) => item.id));
  const additions = candidates.filter((item) => !known.has(item.id));
  const next = {
    lastChecked: new Date().toISOString().slice(0, 10),
    items: [...(existing.items || []), ...additions],
  };
  await fs.writeFile(OUTPUT, `${JSON.stringify(next, null, 2)}\n`);
  console.log(`Checked ${ids.length} PubMed records; added ${additions.length} review candidate(s).`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
