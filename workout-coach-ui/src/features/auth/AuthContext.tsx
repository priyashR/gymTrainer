import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useNavigate } from "react-router-dom";
import * as authApi from "../../lib/authApi";
import {
  setAccessTokenGetter,
  setOnRefreshFailure,
  setOnTokenRefreshed,
} from "../../lib/apiClient";
import type { RegisterResponse } from "../../types/auth";

export interface AuthContextValue {
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<RegisterResponse>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();
  const tokenRef = useRef<string | null>(null);

  // Keep ref in sync so the apiClient getter always reads the latest token
  tokenRef.current = accessToken;

  const isAuthenticated = accessToken !== null;

  // Wire apiClient hooks once on mount
  useEffect(() => {
    setAccessTokenGetter(() => tokenRef.current);

    setOnRefreshFailure(() => {
      setAccessToken(null);
      navigate("/login", { replace: true });
    });

    setOnTokenRefreshed((newToken: string) => {
      setAccessToken(newToken);
    });
  }, [navigate]);

  // Silent refresh on mount
  useEffect(() => {
    let cancelled = false;
    authApi
      .refresh()
      .then((res) => {
        if (!cancelled) {
          setAccessToken(res.accessToken);
        }
      })
      .catch(() => {
        // Refresh failed — user is not authenticated
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(
    async (email: string, password: string): Promise<void> => {
      const res = await authApi.login({ email, password });
      setAccessToken(res.accessToken);
    },
    []
  );

  const register = useCallback(
    async (email: string, password: string): Promise<RegisterResponse> => {
      return authApi.register({ email, password });
    },
    []
  );

  const logout = useCallback(() => {
    setAccessToken(null);
    navigate("/login", { replace: true });
  }, [navigate]);

  const value = useMemo<AuthContextValue>(
    () => ({
      accessToken,
      isAuthenticated,
      isLoading,
      login,
      register,
      logout,
    }),
    [accessToken, isAuthenticated, isLoading, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
