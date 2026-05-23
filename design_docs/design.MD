---
name: Proton Finance
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#201f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353534'
  on-surface: '#e5e2e1'
  on-surface-variant: '#cdc3d4'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#313030'
  outline: '#978d9d'
  outline-variant: '#4b4452'
  surface-tint: '#dab9ff'
  primary: '#dab9ff'
  on-primary: '#460283'
  primary-container: '#bb86fc'
  on-primary-container: '#4c0f89'
  inverse-primary: '#7743b5'
  secondary: '#4ae183'
  on-secondary: '#003919'
  secondary-container: '#06bb63'
  on-secondary-container: '#00431f'
  tertiary: '#ffb4a9'
  on-tertiary: '#690001'
  tertiary-container: '#ff7462'
  on-tertiary-container: '#740002'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#eedbff'
  primary-fixed-dim: '#dab9ff'
  on-primary-fixed: '#2a0053'
  on-primary-fixed-variant: '#5e289b'
  secondary-fixed: '#6bfe9c'
  secondary-fixed-dim: '#4ae183'
  on-secondary-fixed: '#00210c'
  on-secondary-fixed-variant: '#005228'
  tertiary-fixed: '#ffdad5'
  tertiary-fixed-dim: '#ffb4a9'
  on-tertiary-fixed: '#410000'
  on-tertiary-fixed-variant: '#910807'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353534'
typography:
  display-lg:
    fontFamily: Hanken Grotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-md:
    fontFamily: Hanken Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  title-lg:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  headline-lg-mobile:
    fontFamily: Hanken Grotesk
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 36px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  container-margin: 20px
  gutter: 16px
---

## Brand & Style

The design system is engineered to evoke a sense of precision, security, and forward-thinking financial mastery. It targets a demographic of tech-native individuals who demand a high-performance interface that feels both institutional and cutting-edge.

The visual style is a sophisticated evolution of **Material Design 3**, blending its structural logic with **Glassmorphism** and high-fidelity depth. By utilizing a "Dark-First" philosophy, the system minimizes eye strain during frequent expense logging while allowing vibrant semantic colors to pop with maximum clarity. The aesthetic relies on deep, obsidian-like surfaces, subtle translucency for secondary widgets, and high-energy gradients for motivational milestones. The emotional response is one of calm control—transforming complex data into an intuitive, tactile experience.

## Colors

This design system utilizes a high-contrast dark palette designed for clarity and emotional resonance.

- **Foundational Neutrals:** The background is anchored in "Deep Black" (#0A0A0A) to create an infinite depth effect, while primary surfaces use "Charcoal" (#1E1E1E) to define the Material 3 container hierarchy.
- **Semantic Logic:** Financial status is immediately glanceable. **Emerald Green (#2ECC71)** is reserved strictly for income, positive cash flow, and under-budget indicators. **Coral Red (#E74C3C)** is utilized for expenses, debt, and over-budget warnings.
- **Accents & Gradients:** A vibrant transition from **Electric Purple to Cyan** is used exclusively for goal progress and "leveling up" moments, providing a futuristic contrast to the sober charcoal backgrounds.
- **Glassmorphism:** Secondary widgets use a semi-transparent layer of the surface color with a 20px background blur and a 1px white-overlay border at 10% opacity.

## Typography

The typography system prioritizes legibility of financial figures.

- **Headlines:** Use **Hanken Grotesk** for a sharp, contemporary feel that signals modernity. Large display sizes for account balances should use a tighter letter-spacing (-0.02em) to feel cohesive.
- **Body & UI:** **Inter** is the workhorse for all transactional data and descriptions, providing a neutral, highly readable sans-serif experience.
- **Financial Labels:** **JetBrains Mono** is used sparingly for secondary data points like timestamps, transaction IDs, or percentage changes, evoking a "data-stream" aesthetic that reinforces precision.
- **Contrast:** High-contrast white (#FFFFFF) is used for primary figures, while secondary text should drop to 70% opacity (Medium Emphasis) to maintain visual hierarchy.

## Layout & Spacing

This design system follows a **12-column fluid grid** for desktop and a **4-column grid** for mobile.

- **Touch Targets:** Minimum touch targets are set to 48x48dp, ensuring accessibility in a mobile-first environment.
- **Rhythm:** An 8px linear scale governs all padding and margins.
- **Card Spacing:** Use 16px gutters between cards on mobile. On larger displays, cards should span specific column counts (e.g., 4 columns for small widgets, 6 for main charts) to maintain a structured but breathable dashboard.
- **Safe Areas:** A 20px horizontal margin is enforced on mobile devices to prevent content from crowding the edge of the screen.

## Elevation & Depth

Hierarchy is defined through **Tonal Elevation** and **Glassmorphism** rather than traditional heavy shadows.

- **Level 0 (Base):** Deep Charcoal (#0A0A0A) background.
- **Level 1 (Cards):** Surface color (#1E1E1E) with a subtle 1px border (#FFFFFF at 5% opacity).
- **Level 2 (Overlays):** Semi-transparent glass widgets with 24px background blur and a subtle glow shadow (0px 4px 20px, rgba(0,0,0,0.5)).
- **Interactive States:** Buttons and active cards use a subtle "inner glow" or a slight increase in surface brightness to indicate pressability, mimicking physical light reacting to a raised surface.

## Shapes

The shape language is "Soft-Modern," utilizing generous radii to counteract the technical "hardness" of a dark-mode financial app.

- **Primary Containers:** Standard cards and containers use a **20px radius** (rounded-lg).
- **Interactive Elements:** Buttons and input fields use a **16px radius**.
- **Small Components:** Chips and tags use a **pill-shaped (full radius)** design to distinguish them from structural cards.
- **Visual Continuity:** All outlines and borders must be consistent 1px strokes to maintain a clean, architectural feel.

## Components

- **Elevated Cards:** The core unit of the design system. Must have a 20px corner radius and 16px internal padding. Use a subtle gradient stroke for "Gold" or "Premium" features.
- **Action Buttons:** Primary actions use the Electric Purple gradient with white text. Secondary actions use the ghost-style outline with high-contrast white text.
- **Outlined Icons:** Use 24px modern, thin-stroke outlined icons. Icons should be monochrome white, except when indicating semantic status (e.g., a green "plus" for income).
- **Budget Progress Bars:** Use a thick (12px) track with the Electric Purple to Cyan gradient. The track background should be the surface color with 10% opacity.
- **Transaction Lists:** Clean rows with no dividers; use vertical spacing (12px) and subtle hover/tap states to define boundaries.
- **Financial Widgets:** Utilize Glassmorphism (blur + transparency) for floating widgets like "Quick Add" or "Currency Converter" to keep the user focused on the main dashboard layers.