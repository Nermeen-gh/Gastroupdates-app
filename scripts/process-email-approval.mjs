import fs from "node:fs/promises";

const FILE = "data/updates.json";
const comment = (process.env.APPROVAL_COMMENT || "").trim();
const commandLine = comment.split(/\r?\n/).map((line) => line.trim()).find(Boolean) || "";
const payload = JSON.parse(await fs.readFile(FILE, "utf8"));
const items = payload.items || [];

function numbersFrom(text) {
  return [...new Set((text.match(/\d+/g) || []).map(Number))]
    .filter((number) => Number.isInteger(number) && number >= 1 && number <= items.length);
}

let approve = [];
let reject = [];
const approveMatch = commandLine.match(/^APPROVE\s*:\s*([\d,\s]+)$/i);
const rejectMatch = commandLine.match(/^REJECT\s*:\s*([\d,\s]+)$/i);
const plainNumbers = commandLine.match(/^\s*[\d,\s]+\s*$/);

if (/^ALL$/i.test(commandLine) || /^APPROVE\s*:\s*ALL$/i.test(commandLine)) {
  approve = items.map((_, index) => index + 1).filter((number) => items[number - 1].reviewStatus === "pending");
} else if (approveMatch) {
  approve = numbersFrom(approveMatch[1]);
} else if (plainNumbers) {
  approve = numbersFrom(commandLine);
}
if (rejectMatch) reject = numbersFrom(rejectMatch[1]);

const approvedTitles = [];
const approvedPmids = [];
const rejectedTitles = [];
for (const number of approve) {
  const item = items[number - 1];
  if (item && item.reviewStatus === "pending") {
    item.reviewStatus = "approved";
    item.reviewedAt = new Date().toISOString();
    approvedTitles.push(`${number}. ${item.title}`);
    if (item.pmid) approvedPmids.push(String(item.pmid));
  }
}
for (const number of reject) {
  const item = items[number - 1];
  if (item && item.reviewStatus === "pending") {
    item.reviewStatus = "rejected";
    item.reviewedAt = new Date().toISOString();
    rejectedTitles.push(`${number}. ${item.title}`);
  }
}

const changed = approvedTitles.length + rejectedTitles.length > 0;
if (changed) await fs.writeFile(FILE, `${JSON.stringify(payload, null, 2)}\n`);

const summary = [
  changed ? "## Your email selection was processed" : "## No selection was processed",
  "",
  ...(approvedTitles.length ? ["### Approved and scheduled for publication", ...approvedTitles.map((title) => `- ${title}`), ""] : []),
  ...(rejectedTitles.length ? ["### Rejected", ...rejectedTitles.map((title) => `- ${title}`), ""] : []),
  ...(!changed ? ["Reply with only article numbers, such as `1, 4, 7`, or reply `ALL` to approve every pending article."] : []),
].join("\n");
await fs.writeFile("approval-summary.md", `${summary}\n`);

if (process.env.GITHUB_OUTPUT) {
  await fs.appendFile(process.env.GITHUB_OUTPUT, `changed=${changed}\n`);
  await fs.appendFile(process.env.GITHUB_OUTPUT, `approved_pmids=${approvedPmids.join(",")}\n`);
  await fs.appendFile(process.env.GITHUB_OUTPUT, `approved_count=${approvedPmids.length}\n`);
  await fs.appendFile(process.env.GITHUB_OUTPUT, `pending=${items.filter((item) => item.reviewStatus === "pending").length}\n`);
}
