Act as a Principal Mobile UI Engineer and Design Systems Expert. 
Build a mobile screen in [Jetpack Compose / Flutter - اختر أحدهما] following the strict "Vivid+Co" obsidian aesthetic guidelines below. Do not deviate from these rules.

--- DESIGN TOKENS ---
Canvas & Surfaces:
- Page Canvas (Background): #101010 (Deep Obsidian)
- Content Surface: #495764 (Graphite Veil)
- Hairline Dividers: 1dp solid #403f3f (Ash Border)
- High-contrast Card (Rare): #fffdf9 (Bone White)

Typography (Neue Montreal / Inter Fallback):
- RULE: Authority comes from scale, NOT weight. Strictly use Weight 400 (Regular) across 98% of the UI.
- EXCEPTION: Weight 700 (Bold) is ONLY permitted for 24sp subheadings. Never make Display or Title text bold.
- Display Hero: 52sp, Weight 400, line-height 1.0 (52sp), letter-spacing -0.02em, color #fffdf9.
- Subtitle: 18sp, Weight 400, line-height 1.4, color #fffdf9.
- Case Study Title: 22sp, Weight 400, line-height 1.2, letter-spacing -0.01em, color #fffdf9.
- Metadata / Labels: 14sp, Weight 400, line-height 1.2, color #6f879c (Fog Blue).
- Nav / Button Action: 13sp, Weight 400, letter-spacing +0.02em, ALL CAPS, color #fffdf9.

The Signature "Prism Artifact":
- The ONLY chromatic element in the entire UI is the RGB-split prism artifact: Red (#ff2a2a), Cyan (#2a7fff), Lime (#2aff2a) bleeding caustics around dark geometric cubes (#000000 cores with #fffdf9 specular highlights).
- NEVER leak prism colors into buttons, badges, or general UI elements.

Interaction & Elevation:
- ZERO box-shadows / drop-shadows across all components (Strictly flat elevation).
- NO filled CTA buttons or colorful accents. 
- Border Radius: 0dp for buttons and standard containers; 5dp strictly reserved for the Outlined Contact button; 15dp for rare cards.

--- SCREEN REQUIREMENTS ---
Build a complete, responsive mobile screen containing:
1. Top Bar: Wordmark "VIVID+CO" (13sp, uppercase) on the left, and a transparent Outlined Button "CONTACT" (1dp solid #fffdf9 border, 5dp corner radius, 13sp text) on the right.
2. Hero Section: Centered/staggered Prism chromatic graphic, overlaid with a sculptural, tightly stacked multi-line headline in 52sp Weight 400, followed by an 18sp subtitle.
3. Hairline Separator: Full-width 1dp line (#403f3f).
4. Content Section: List of interactive rows (e.g., project titles or feature items) at 22sp (#fffdf9), accompanied by metadata tags in 14sp Fog Blue (#6f879c), separated by 1dp hairline dividers.

Output production-ready, clean, idiomatic code with no external UI library dependencies other than native framework components.
