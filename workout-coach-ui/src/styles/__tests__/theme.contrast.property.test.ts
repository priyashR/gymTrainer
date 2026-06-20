import { describe, it, expect } from "vitest";
import fc from "fast-check";

/**
 * **Validates: Requirements 1.5**
 *
 * Property: All semantically valid text/background token combinations in the
 * dark mode theme must meet WCAG AA contrast ratio guidelines:
 *   - Normal text (body copy): contrast ratio ≥ 4.5:1
 *   - Large text (headings, labels, UI controls ≥ 18px or 14px bold): contrast ratio ≥ 3:1
 *
 * Token pairing rules:
 *   - Standard text tokens are rendered on dark background surfaces
 *   - --color-fab-text is only used on --color-fab-bg (button label on button background)
 *   - Accent/status colours used as text are rendered on dark backgrounds
 */

// Dark backgrounds where standard text content is rendered
const DARK_BACKGROUNDS = [
  { name: "--color-bg-primary", hex: "#121212" },
  { name: "--color-bg-surface", hex: "#1e1e1e" },
  { name: "--color-bg-card", hex: "#2a2a2a" },
  { name: "--color-bg-editor", hex: "#0d1117" },
] as const;

// Text tokens used for body copy (normal text ≥ 4.5:1)
const NORMAL_TEXT_TOKENS = [
  { name: "--color-text-primary", hex: "#f5f5f5" },
  { name: "--color-text-secondary", hex: "#a0a0a0" },
] as const;

// Accent/status colours used as text labels or large UI elements (large text ≥ 3:1)
const LARGE_TEXT_TOKENS = [
  { name: "--color-accent", hex: "#4fc3f7" },
  { name: "--color-accent-hover", hex: "#29b6f6" },
  { name: "--color-success", hex: "#66bb6a" },
  { name: "--color-error", hex: "#ef5350" },
  { name: "--color-warning", hex: "#ffa726" },
  { name: "--color-crossfit", hex: "#ff7043" },
] as const;

// FAB-specific pairing (fab-text is dark text on light fab-bg)
const FAB_PAIR = {
  text: { name: "--color-fab-text", hex: "#121212" },
  bg: { name: "--color-fab-bg", hex: "#4fc3f7" },
} as const;

/**
 * Parse a hex colour string to its RGB components (0-255).
 */
function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const cleaned = hex.replace("#", "");
  return {
    r: parseInt(cleaned.substring(0, 2), 16),
    g: parseInt(cleaned.substring(2, 4), 16),
    b: parseInt(cleaned.substring(4, 6), 16),
  };
}

/**
 * Convert an sRGB component (0-255) to its linearised value.
 * Per WCAG 2.1: https://www.w3.org/TR/WCAG21/#dfn-relative-luminance
 */
function linearize(channel: number): number {
  const srgb = channel / 255;
  return srgb <= 0.03928
    ? srgb / 12.92
    : Math.pow((srgb + 0.055) / 1.055, 2.4);
}

/**
 * Calculate the relative luminance of a colour.
 */
function relativeLuminance(hex: string): number {
  const { r, g, b } = hexToRgb(hex);
  return (
    0.2126 * linearize(r) + 0.7152 * linearize(g) + 0.0722 * linearize(b)
  );
}

/**
 * Calculate the WCAG contrast ratio between two colours.
 * Returns a value ≥ 1 (e.g. 4.5 means 4.5:1).
 */
function contrastRatio(foreground: string, background: string): number {
  const lumFg = relativeLuminance(foreground);
  const lumBg = relativeLuminance(background);
  const lighter = Math.max(lumFg, lumBg);
  const darker = Math.min(lumFg, lumBg);
  return (lighter + 0.05) / (darker + 0.05);
}

// Build semantically valid pair sets
const normalTextPairs = NORMAL_TEXT_TOKENS.flatMap((text) =>
  DARK_BACKGROUNDS.map((bg) => ({ text, bg }))
);

const largeTextPairs = LARGE_TEXT_TOKENS.flatMap((text) =>
  DARK_BACKGROUNDS.map((bg) => ({ text, bg }))
);

const allLargeTextPairsIncludingFab = [...largeTextPairs, FAB_PAIR];

describe("Dark mode tokens WCAG AA contrast", () => {
  it("normal text tokens on dark backgrounds meet ≥ 4.5:1 contrast ratio", () => {
    const pairArbitrary = fc.constantFrom(...normalTextPairs);

    fc.assert(
      fc.property(pairArbitrary, (pair) => {
        const ratio = contrastRatio(pair.text.hex, pair.bg.hex);
        expect(
          ratio,
          `${pair.text.name} (${pair.text.hex}) on ${pair.bg.name} (${pair.bg.hex}) ` +
            `has contrast ratio ${ratio.toFixed(2)}:1, expected ≥ 4.5:1 (normal text)`
        ).toBeGreaterThanOrEqual(4.5);
      }),
      { numRuns: 100 }
    );
  });

  it("accent/status text tokens and FAB text on their respective backgrounds meet ≥ 3:1 contrast ratio (large text)", () => {
    const pairArbitrary = fc.constantFrom(...allLargeTextPairsIncludingFab);

    fc.assert(
      fc.property(pairArbitrary, (pair) => {
        const ratio = contrastRatio(pair.text.hex, pair.bg.hex);
        expect(
          ratio,
          `${pair.text.name} (${pair.text.hex}) on ${pair.bg.name} (${pair.bg.hex}) ` +
            `has contrast ratio ${ratio.toFixed(2)}:1, expected ≥ 3:1 (large text)`
        ).toBeGreaterThanOrEqual(3.0);
      }),
      { numRuns: 100 }
    );
  });
});
