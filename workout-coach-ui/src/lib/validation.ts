export interface ValidationErrors {
  email?: string;
  password?: string;
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateRegistration(
  email: string,
  password: string
): ValidationErrors {
  const errors: ValidationErrors = {};

  if (!email || !EMAIL_REGEX.test(email)) {
    errors.email = "Must be a valid email address";
  }

  if (!password || password.length < 8) {
    errors.password = "Must be at least 8 characters";
  }

  return errors;
}

export function hasValidationErrors(errors: ValidationErrors): boolean {
  return Object.keys(errors).length > 0;
}
