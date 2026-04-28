import axios from "axios";
import apiClient from "./apiClient";
import type {
  AccessTokenResponse,
  FieldError,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
} from "../types/auth";

export function login(data: LoginRequest): Promise<AccessTokenResponse> {
  return apiClient
    .post<AccessTokenResponse>("/auth/login", data)
    .then((res) => res.data);
}

export function register(data: RegisterRequest): Promise<RegisterResponse> {
  return apiClient
    .post<RegisterResponse>("/auth/register", data)
    .then((res) => res.data);
}

export function refresh(): Promise<AccessTokenResponse> {
  return apiClient
    .post<AccessTokenResponse>("/auth/refresh")
    .then((res) => res.data);
}

export function extractApiError(
  error: unknown
): { message: string; fieldErrors?: FieldError[] } {
  if (axios.isAxiosError(error) && error.response?.data) {
    const data = error.response.data;
    if (data.errors && Array.isArray(data.errors)) {
      return { message: data.error, fieldErrors: data.errors };
    }
    return { message: data.message || "An unexpected error occurred" };
  }
  return {
    message: "Unable to connect. Please check your connection and try again.",
  };
}
