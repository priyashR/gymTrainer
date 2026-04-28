// --- Request DTOs ---

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

// --- Response DTOs ---

export interface AccessTokenResponse {
  accessToken: string;
  tokenType: string; // Always "Bearer"
  expiresIn: number; // Seconds (900 = 15 minutes)
}

export interface RegisterResponse {
  id: string; // UUID
  email: string;
  createdAt: string; // ISO-8601
}

// --- Error Response DTOs (matching Auth Service shapes) ---

export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  path: string;
  timestamp: string;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ValidationErrorResponse {
  status: number;
  error: string; // "Validation Failed"
  errors: FieldError[];
  path: string;
  timestamp: string;
}

// --- Auth State ---

export interface AuthState {
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}
