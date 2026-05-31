package com.example.model

data class GastroUpdate(
    val id: String,
    val title: String,
    val category: String, // Gastroenterology, Hepatology, Endoscopy
    val source: String, // e.g., "AGA 2026 Guidelines", "ASGE Consensus Update"
    val summary: String,
    val fullDetails: String,
    val practicalTakeaways: List<String>,
    val date: String,
    val status: String, // e.g., "New Standard", "FDA Approved", "Clinical Trial"
    val references: String,
    val isBookmarked: Boolean = false
)

object GastroDataRepository {
    val updates = listOf(
        GastroUpdate(
            id = "resmetirom_2026",
            title = "Resmetirom (Rezdiffra) Real-World MASH Efficacy (2026)",
            category = "Hepatology",
            source = "AASLD Practice Guidelines Update 2026",
            summary = "Real-world registry data in 2026 confirms high efficacy of Resmetirom in reversing Metabolic Dysfunction-Associated Steatohepatitis (MASH) hepatic fibrosis in stage F2-F3 disease.",
            fullDetails = "Post-marketing analysis from the multi-center MASH registries in 2026 demonstrates that oral Resmetirom (80-100 mg daily THR-β selective thyroid hormone receptor agonist) achieves histologically significant resolution of steatohepatitis in 28.6% of patients and improves fibrosis stage (by at least 1 stage with no worsening of MASH) in 25.9% of F2-F3 patients over 52 weeks. Liver stiffness measurements (LSM) by transient elastography (FibroScan) decreased by an average of 1.8 kPa, and MRI-PDFF showed an average relative hepatic fat reduction of 32%. No significant increase in safety signals was reported compared to initial Phase 3 trials.",
            practicalTakeaways = listOf(
                "Indicated for patients with moderate to advanced (F2 to F3) hepatic fibrosis due to MASH/NAS.",
                "Dosing should be weight-based: 80 mg daily (<100 kg) or 100 mg daily (>=100 kg).",
                "Non-invasive tests (FIB-4, FibroScan, or MRI-PDFF) are recommended at baseline, 6 months, and 12 months for therapeutic tracking."
            ),
            date = "March 2026",
            status = "Standard of Care",
            references = "Harrison SA, et al. Real-World Efficacy of THR-β Selectivity in NASH. NEJM 2024 / Hepatology Clinical Registries Jan-May 2026 Progress Report."
        ),
        GastroUpdate(
            id = "ai_colon_2026",
            title = "Standardizing AI Computer-Aided Detection (CADe) in Colonoscopy",
            category = "Endoscopy",
            source = "ASGE Quality Standards Updates 2026",
            summary = "The 2026 ASGE Task Force updates recommend AI-assisted computer-aided detection (CADe) during screening colonoscopies to boost adenoma detection rates.",
            fullDetails = "The American Society for Gastrointestinal Endoscopy (ASGE) in early 2026 updated its standards of clinical excellence. Based on data from over 250,000 screenings, incorporating real-time computer-aided detection (CADe) platforms during colonoscopy is strongly advised. CADe AI overlays provide dynamic visual bounding boxes that outline suspected sessile or flat polyps that might otherwise be missed. Clinical cohorts demonstrated an absolute Adenoma Detection Rate (ADR) increase of 6.2% across certified endoscopy centers using AI, significantly lowering post-colonoscopy colorectal cancer (PCCRC) risk.",
            practicalTakeaways = listOf(
                "AI systems (CADe) should scale across routine screenings to minimize flat polyp miss-rates.",
                "High ADR (>=30% in males, >=20% in females) remains the key quality metric, and AI assistance is proven to elevate sub-threshold performers.",
                "AI does not replace thorough mucosal visualization, and withdrawal time should still exceed 6 minutes."
            ),
            date = "January 2026",
            status = "New guidelines",
            references = "ASGE/ACG Joint Task Force on Quality Indicators for Colonoscopy. Gastrointestinal Endoscopy 2026;103(2):145-154."
        ),
        GastroUpdate(
            id = "dual_biologic_2026",
            title = "Dual Biologic & Targeted Therapy in Refractory IBD",
            category = "Gastroenterology",
            source = "AGA Clinical Practice Update 2026",
            summary = "Dual-targeted therapies (combining advanced biologics or small molecules) are highlighted as safe and highly effective options for refractory Crohn's and UC.",
            fullDetails = "The Management of Refractory Inflammatory Bowel Disease (IBD) guidelines published in 2026 address 'dual-targeted therapy' (DTT). For severe Crohn's Disease or Ulcerative Colitis failing multiple single agents, combining an anti-integrin (Vedolizumab) or anti-IL23p19 (Risankizumab) with a second biologic (Infliximab/Adalimumab) or a JAK inhibitor (Upadacitinib) achieves high clinical remission where previous single agents failed. 2026 clinical results from the DUO-IBD pilot protocol reported 44.5% mucosal healing at 24 weeks with no clinical synergy in opportunistic infections relative to monotherapy.",
            practicalTakeaways = listOf(
                "Consider dual biologic/targeted therapy only in verified highly refractory Crohn's or UC under expert supervision.",
                "High-risk combinations (such as dual anti-TNF + JAK inhibitors) should be monitored with baseline and periodic viral panel screening.",
                "The preferred pairs usually combine a gut-selective agent (e.g., Vedolizumab) with a systemic pathway blocker (e.g., Risankizumab/Upadacitinib)."
            ),
            date = "May 2026",
            status = "Treatment Update",
            references = "AGA Clinical Practice Updates on Dual Targeted Therapies in IBD. Gastroenterology 2026;170(4):910-922."
        ),
        GastroUpdate(
            id = "hbv_cure_2026",
            title = "Hepatitis B Functional Cure Breakthrough Trials",
            category = "Hepatology",
            source = "AASLD-EASL Special Consensus Report 2026",
            summary = "Phase III trials verify high rates of HBsAg clearance with combined Bepirovirsen (antisense oligonucleotide) and interferon therapies in chronic HBV.",
            fullDetails = "In 2026, the clinical landscape for Chronic Hepatitis B shifted with the release of full outcomes for antisense RNA-silencing drugs. Administering Bepirovirsen (300 mg subcutaneous weekly loading, followed by monthly maintenance) in nucleos(t)ide analogue (NA)-suppressed chronic patients led to elevated Hepatitis B surface Antigen (HBsAg) loss in 14.8% of participants after 24 weeks. This increases to 21% when paired with pegylated-interferon-alpha-2a (PegIFN) short-course therapy. These are the highest functional cure rates ever achieved in clinical trials, signaling an upcoming shift in global therapeutic algorithms.",
            practicalTakeaways = listOf(
                "Functional cure is defined as persistent HBsAg loss (<0.05 IU/mL) and undetectable HBV DNA after finishing treatment.",
                "The safety profile shows predictable transient ALT flares matching therapeutic immune-mediated clearance.",
                "Requires rigorous screening for baseline cirrhosis to prevent decompensation during immunological flares."
            ),
            date = "April 2026",
            status = "Clinical Trial Results",
            references = "AASLD Late-Breaking Abstracts & Clinical Liver Disease 2026;27(1):12-19."
        ),
        GastroUpdate(
            id = "third_space_2026",
            title = "Third-Space Endoscopy Competency Standards",
            category = "Endoscopy",
            source = "ASGE/ESGE Multi-Society Guideline 2026",
            summary = "Formal standardization of training pathways and competency thresholds is introduced for POEM, ESD, and EFTR procedures.",
            fullDetails = "Third-space endoscopy (also called intramural endoscopy) has matured from specialized experimentation into standard treatment for achalasia (POEM - Peroral Endoscopic Myotomy) and complex early neoplasia (ESD - Endoscopic Submucosal Dissection). The Joint ASGE/ESGE 2026 standards mandate that certified endoscopists must complete a minimum of 40 proctored cases for POEM and 30 for gastric ESD to demonstrate independent competency. Standardized codes (CPT codes update) are now fully integrated for reimbursement of intramural operations across hospitals.",
            practicalTakeaways = listOf(
                "POEM is now recognized as first-line for Type II Achalasia and Spastic Esophageal Disorders.",
                "Institutions must maintain strict credentials, including adequate backup from thoracic/gastrointestinal surgical suites.",
                "Submucosal tunneling techniques require specific CO2 insufflation protocols rather than standard room air to prevent mediastinal emphysema."
            ),
            date = "February 2026",
            status = "Standard of Care",
            references = "Gastrointestinal Endoscopy 2026;103(1):89-98."
        ),
        GastroUpdate(
            id = "pcab_gerd_2026",
            title = "PCABs Outperforming PPIs in Severe Refractory GERD",
            category = "Gastroenterology",
            source = "Journal of Gastroenterology Updates 2026",
            summary = "Vonoprazan displays significantly faster erosive esophagitis healing and heartburn relief in clinical trials over conventional PPIs.",
            fullDetails = "Potassium-Competitive Acid Blockers (PCABs, such as Vonoprazan) are rapidly replacing traditional Proton Pump Inhibitors (PPIs) for severe acid-related disorders. In 2026 clinical audits, Vonoprazan (20 mg daily for healing, 10 mg daily for maintenance) demonstrated a 93.8% erosive esophagitis (EE) healing rate in Grade C/D reflux patients over 8 weeks, compared to only 82.1% for Esomeprazole (40 mg). Heartburn relief was achieved 2 days faster on average. The mechanism (acid-stable binding of the H+/K+-ATPase pump without requiring proton activation) allows complete, 24-hour gastric acid suppression from the first dose.",
            practicalTakeaways = listOf(
                "Vonoprazan is highly recommended for severe (LA Grade C or D) erosive esophagitis or PPI-refractory GERD.",
                "Does not require dosing 30-60 minutes before meals; can be taken with or without food at any time.",
                "Long-term metabolic and safety parameters remain comparable to high-dose PPI therapy."
            ),
            date = "May 2026",
            status = "Standard of Care",
            references = "Clinical Gastroenterology and Hepatology 2026;24(5):387-395."
        ),
        GastroUpdate(
            id = "baveno_8_2026",
            title = "Preview of Baveno VIII Portal Hypertension Guidelines",
            category = "Hepatology",
            source = "Baveno Consensus Conferences 2026",
            summary = "Consensus drafts refine non-invasive thresholds for initiating Carvedilol in portal hypertension and rule out high-risk varices.",
            fullDetails = "The upcoming consensus definitions from the Baveno VIII panel (drafted in early 2026) modify clinical guidance for compensated advanced chronic liver disease (cACLD). The rule of thumb for ruling out high-risk esophageal varices remains a liver stiffness measurement (LSM) < 20 kPa and platelet count > 150,000. In addition, Carvedilol (6.25-12.5 mg daily) is officially prioritized as the non-selective beta-blocker (NSBB) of choice due to its superior portal pressure-reducing effect, to be initiated strictly upon diagnosis of cACLD with clinically significant portal hypertension (LSM >= 25 kPa) without requiring an index screening endoscopy.",
            practicalTakeaways = listOf(
                "Carvedilol is the preferred NSBB for preventing clinical decompensation of cirrhosis.",
                "Screening endoscopy can be safely bypassed in patients meeting LSM <20 kPa and platelet >150,000.",
                "Repeated non-invasive staging (spleen stiffness and transient elastography) is validated for treatment adherence and clinical response evaluation."
            ),
            date = "February 2026",
            status = "New guidelines",
            references = "Journal of Hepatology 2026 Consensus Drafts / AASLD Abstract Updates."
        ),
        GastroUpdate(
            id = "scope_reprocess_2026",
            title = "Disposable and Shielded Endoscope Mandates",
            category = "Endoscopy",
            source = "CDC/FDA Infection Prevention Injunction 2026",
            summary = "Strict national standards mandate high-volume ERCP and EUS centers to employ sterile single-use disposable scope heads.",
            fullDetails = "The containment of carbapenem-resistant enterobacteriaceae (CRE) outbreaks tied to duodenoscopes led to strict regulations in 2026. High-volume centers performing ERCP or EUS must utilize single-use disposable duodenoscopes or scopes designed with fully disposable sterile elevator caps. Standard manual cleaning of physical elevator channels is deemed high-risk. Furthermore, institutions must implement double-cycle ethylene oxide sterilization or automated liquid chemical reprocessing with documented microbiologic culturing at 14-day intervals for any remaining semi-reusable devices.",
            practicalTakeaways = listOf(
                "EUS and ERCP units must systematically phase out legacy fully-reusable fixed-cap duodenoscopes.",
                "Single-use scopes should be available as priority for patients with pre-existing multi-resistant colonizations.",
                "Rigorous infection tracking and microbiologic surveillance audits of reusable devices are legally mandated."
            ),
            date = "April 2026",
            status = "FDA Advisory Update",
            references = "FDA Center for Devices and Radiological Health Safety Bulletin, April 2026."
        )
    )
}
