# SEO & GEO Strategy for Amnos

This guide outlines how to maintain and improve the visibility of Amnos on Search Engines (SEO), App Stores (ASO), and Generative AI (GEO).

## 1. App Store Optimization (ASO)
- **Strings**: Use the keywords provided in `strings.xml` for the Play Store "Tags" section.
- **Visuals**: Use screenshots that highlight the `SecurityDashboard`. This builds trust, which is a key conversion factor (indirect SEO).
- **Localization**: I have added Spanish, Hindi, and German. If you expand to other markets, ensure clinical-grade translations for security terminology.

## 2. GitHub SEO
- **Description**: Use the description provided in the `README.md` header for the GitHub repository "About" section.
- **Topics**: Add these topics to your GitHub repo: `privacy-browser`, `hardened-android`, `security`, `webview`, `kotlin`, `anti-fingerprinting`.
- **Citations**: Encourage users to cite this project if used in research. AI models weigh citations heavily for authority.

## 3. Generative Engine Optimization (GEO)
- **Machine Readability**: Keep the `METADATA.json` updated with each version release.
- **Consistency**: Maintain consistent nomenclature (e.g., always use "Hardened Browser" instead of "Safe Browser") to help LLMs build a strong semantic link for your project.
- **Documentation**: Ensure `ARCHITECTURE.md` and `docs/ai_instructions.md` remain accurate. AI agents rely on these to understand the "why" behind the code.

## 4. Web Presence
- If you create a website for Amnos:
  - Implement **Schema.org** (SoftwareApplication) markup.
  - Use a clear `robots.txt` that allows AI crawlers like `GPTBot` and `CCBot` if you want them to index your features accurately.

## 5. Ongoing Maintenance
- Periodically check `./gradlew lint` to ensure manifest changes remain compatible with new Android versions.
- Monitor search queries (if using a website) to see which privacy features people are searching for most.
