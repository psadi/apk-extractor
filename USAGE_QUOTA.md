# AI Usage Quota & Cost Transparency 📊

[![AI Model](https://img.shields.io/badge/AI_Model-Gemini_Flash-8E75B2?logo=googlegemini&logoColor=white)](USAGE_QUOTA.md)
[![Input Tokens](https://img.shields.io/badge/Input_Tokens-114.0M-informational?logo=google&logoColor=white)](USAGE_QUOTA.md)
[![Output Tokens](https://img.shields.io/badge/Output_Tokens-385K-blueviolet)](USAGE_QUOTA.md)
[![AI Spend](https://img.shields.io/badge/AI_Spend-$14.61_USD-brightgreen?logo=googlecloud&logoColor=white)](USAGE_QUOTA.md)
[![License](https://img.shields.io/badge/License-AGPL_3.0-blue.svg)](LICENSE)

This document provides a transparent, verifiable accounting of the compute, token usage, and financial expenditures incurred during the autonomous mobile development of **APK Extractor**.

The entire project was architected, coded, compiled, tested, and released directly on an Android mobile device via **Termux** and Google's **Antigravity CLI (`agy`)** powered by **Gemini**.

---

## 📈 Executive Summary

| Metric | Cumulative Total |
| :--- | :--- |
| **Foundation Model** | **Google Gemini Flash (`gemini-3.8-flash`)** |
| **Development Environment** | Android Mobile via **Termux** (`aarch64` Linux) |
| **Total Model Invocations / Steps** | **922** generations |
| **Total Input Tokens (Prompt)** | **114,021,143** (~114.0M tokens) |
| ↳ *Non-cached Prompt Tokens* | 107,854,939 (94.6%) |
| ↳ *Context-cached Tokens* | 6,166,204 (5.4%) |
| **Total Output Tokens** | **384,714** (~384.7k tokens) |
| ↳ *Thinking / Reasoning Tokens* | 201,866 (52.5%) |
| ↳ *Code Generation & Tool Tokens* | 182,848 (47.5%) |
| **Total Tokens Processed** | **114,405,857** (~114.4M tokens) |
| **Peak Context Window Size** | **255,849** tokens (of 256k window) |
| **Total AI Compute Spend (USD)** | **$14.61 USD** |
| **Average Cost per Release** | **~$2.44 USD** |
| **Files Authored / Maintained** | **48** files |
| **Total Code Base** | **~4,100+** lines of Kotlin, Compose, XML, Gradle DSL |

---

## 💵 Pricing & Rate Model

Expenditures are computed using official **Google Gemini Flash** tiered API rates:

* **Tier 1 (Prompt Context $\le$ 128k tokens):**
  * Input (Non-cached): `$0.075` per 1,000,000 tokens
  * Context Caching: `$0.01875` per 1,000,000 tokens
  * Output (including thinking): `$0.30` per 1,000,000 tokens
* **Tier 2 (Prompt Context $>$ 128k tokens):**
  * Input (Non-cached): `$0.15` per 1,000,000 tokens
  * Context Caching: `$0.0375` per 1,000,000 tokens
  * Output (including thinking): `$0.60` per 1,000,000 tokens

---

## 🗓️ Historical Breakdown per Release

Every milestone represents a complete software engineering cycle: requirement analysis, codebase exploration, code authoring, local hermetic compilation via Gradle/AAPT2, device testing, git commit, version tagging, and GitHub Release asset packaging.

| Release / Milestone | Date (IST) | Key Scope & Deliverables | Invocations | Input Tokens | Cached Tokens | Output Tokens | Thinking Tokens | Spend (USD) | Cumulative Spend |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **v1.0.0** | 2026-09-08 04:35 | **Initial MVP & Architecture:** Jetpack Compose Material 3 UI, fast A–Z scroller, SAF DocumentFile & MediaStore extraction, baseline profiles, GitHub Actions CI/CD workflows, Play Store guide. | 329 | 42,214,644 | 1,624,668 | 101,538 | 63,842 | **$5.55** | **$5.55** |
| **v1.1.0** | 2026-09-08 05:13 | **In-App Installer & Splits:** Integrated native `PackageInstaller` split session installer, install unknown apps permissions flow, App Bundle generation, and Settings support links. | 151 | 17,013,192 | 865,306 | 75,202 | 40,262 | **$2.02** | **$7.57** |
| **v1.1.1** | 2026-09-08 05:31 | **Signing & Transparency:** Android release keystore creation, ColorOS Super Guard sideload compatibility, PayPal/GitHub Sponsor support, and donation pledge commitment. | 122 | 14,448,675 | 832,852 | 43,129 | 14,863 | **$1.85** | **$9.42** |
| **v1.1.2** | 2026-09-08 05:49 | **Split Bundler & Inspector:** `.apks` Zip bundle generator, magnifying glass architecture inspector (`Search` icon), post-extraction banner cleanup, and comprehensive `AGENT_TROUBLESHOOTING.md`. | 101 | 14,309,874 | 547,852 | 56,505 | 24,159 | **$1.82** | **$11.24** |
| **v1.1.3** | 2026-09-08 05:54 | **Feedback & Layout Fixes:** In-app User Feedback dialog (GitHub Issues & Google Play Store), responsive `FlowRow` chip wrapping with ellipsis for long version names, and experimental layout opt-ins. | 49 | 5,401,596 | 236,049 | 16,007 | 7,146 | **$0.76** | **$12.00** |
| **v1.2.0** | 2026-09-08 23:55 | **Auto-Refresh & Extracted Backups:** Real-time auto-refresh on foreground (`onResume`) & package broadcast (`ACTION_PACKAGE_ADDED/REMOVED`), dedicated **Extracted Backups** tab with search, one-tap install, split bundle installer, share, and delete. | 170 | 20,633,162 | 2,059,477 | 92,333 | 51,594 | **$2.61** | **$14.61** |
| **Total** | — | — | **922** | **114,021,143** | **6,166,204** | **384,714** | **201,866** | **$14.61** | **$14.61** |

---

## 🔒 100% Transparency & Donation Pledge

APK Extractor is completely free, open source, and ad-free.

> [!IMPORTANT]
> **Vote of Confidence Pledge:**
> **100% of all community donations and sponsorships solely fund the development of APK Extractor** — specifically paying for AI token usage, API expenditures, development tooling, and open-source infrastructure.
>
> There is zero ambiguity: no funds are diverted to personal profits or unrelated expenses. Every dollar contributed directly keeps this autonomous mobile development pipeline alive.

If you would like to support future updates and feature development:
* **GitHub Sponsors:** [github.com/sponsors/psadi](https://github.com/sponsors/psadi)
* **PayPal:** [paypal.me/psadithya](https://paypal.me/psadithya)

---

## 🔍 How to Independently Verify These Metrics

All telemetry and generation metadata are stored locally in the Antigravity conversation SQLite database at:
```
~/.gemini/antigravity-cli/conversations/<conversation-id>.db
```
The protobuf records inside `gen_metadata` contain exact token counts (`1.4.5` = prompt tokens, `1.4.2` = cached tokens, `1.4.3` = output tokens, `1.4.10` = thinking tokens) for every step of execution.
