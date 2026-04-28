import { describe, it, expect } from "vitest";
import fc from "fast-check";
import {
  validateRegistration,
  hasValidationErrors,
} from "../../../lib/validation";

// Feature: workout-coach-ui-mvp1, Property 1: Registration client-side validation rejects invalid inputs

describe("Registration client-side validation", () => {
  it("should reject emails that do not contain exactly one @ with non-empty local and domain parts separated by a dot", () => {
    const invalidEmail = fc.oneof(
      // empty string
      fc.constant(""),
      // no @ at all
      fc.stringOf(fc.char().filter((c) => c !== "@" && c !== " ")).filter(
        (s) => s.length > 0
      ),
      // missing domain (ends with @)
      fc.string({ minLength: 1 }).map((s) => s.replace(/[@\s]/g, "a") + "@"),
      // missing dot in domain part
      fc
        .tuple(
          fc.string({ minLength: 1 }).map((s) => s.replace(/[@\s]/g, "a")),
          fc.string({ minLength: 1 }).map((s) => s.replace(/[@.\s]/g, "a"))
        )
        .map(([local, domain]) => `${local}@${domain}`)
    );

    fc.assert(
      fc.property(invalidEmail, fc.string(), (email, password) => {
        const errors = validateRegistration(email, password);
        expect(errors.email).toBeDefined();
      }),
      { numRuns: 100 }
    );
  });

  it("should reject passwords shorter than 8 characters", () => {
    const shortPassword = fc.string({ minLength: 0, maxLength: 7 });

    fc.assert(
      fc.property(fc.string(), shortPassword, (email, password) => {
        const errors = validateRegistration(email, password);
        expect(errors.password).toBeDefined();
      }),
      { numRuns: 100 }
    );
  });

  it("should accept valid email and password combinations with no errors", () => {
    const validEmail = fc
      .tuple(
        fc.stringOf(fc.char().filter((c) => !" @".includes(c)), {
          minLength: 1,
          maxLength: 20,
        }),
        fc.stringOf(fc.char().filter((c) => !" @.".includes(c)), {
          minLength: 1,
          maxLength: 10,
        }),
        fc.stringOf(fc.char().filter((c) => !" @.".includes(c)), {
          minLength: 1,
          maxLength: 10,
        })
      )
      .map(([local, domain, tld]) => `${local}@${domain}.${tld}`);

    const validPassword = fc.string({ minLength: 8, maxLength: 64 });

    fc.assert(
      fc.property(validEmail, validPassword, (email, password) => {
        const errors = validateRegistration(email, password);
        expect(hasValidationErrors(errors)).toBe(false);
      }),
      { numRuns: 100 }
    );
  });
});
