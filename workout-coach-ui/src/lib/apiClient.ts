import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from "axios";

type AccessTokenGetter = () => string | null;
type OnRefreshFailure = () => void;
type OnTokenRefreshed = (newToken: string) => void;

let getAccessToken: AccessTokenGetter = () => null;
let onRefreshFailure: OnRefreshFailure = () => {};
let onTokenRefreshed: OnTokenRefreshed = () => {};

export function setAccessTokenGetter(getter: AccessTokenGetter): void {
  getAccessToken = getter;
}

export function setOnRefreshFailure(callback: OnRefreshFailure): void {
  onRefreshFailure = callback;
}

export function setOnTokenRefreshed(callback: OnTokenRefreshed): void {
  onTokenRefreshed = callback;
}

const apiClient = axios.create({
  baseURL: "/api/v1",
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

// --- Request interceptor: attach Authorization header ---
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => Promise.reject(error)
);

// --- Response interceptor: 401 refresh + retry ---
const AUTH_ENDPOINTS = ["/auth/login", "/auth/register", "/auth/refresh"];

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null): void {
  failedQueue.forEach(({ resolve, reject }) => {
    if (token) {
      resolve(token);
    } else {
      reject(error);
    }
  });
  failedQueue = [];
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as AxiosRequestConfig & {
      _retry?: boolean;
    };

    if (!originalRequest) {
      return Promise.reject(error);
    }

    const requestUrl = originalRequest.url ?? "";
    const isAuthEndpoint = AUTH_ENDPOINTS.some((ep) =>
      requestUrl.includes(ep)
    );

    if (error.response?.status !== 401 || isAuthEndpoint || originalRequest._retry) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then((token) => {
        originalRequest.headers = {
          ...originalRequest.headers,
          Authorization: `Bearer ${token}`,
        };
        return apiClient(originalRequest);
      });
    }

    originalRequest._retry = true;
    isRefreshing = true;

    try {
      const { data } = await apiClient.post<{ accessToken: string }>(
        "/auth/refresh"
      );
      const newToken = data.accessToken;
      onTokenRefreshed(newToken);
      processQueue(null, newToken);

      originalRequest.headers = {
        ...originalRequest.headers,
        Authorization: `Bearer ${newToken}`,
      };
      return apiClient(originalRequest);
    } catch (refreshError) {
      processQueue(refreshError, null);
      onRefreshFailure();
      return Promise.reject(refreshError);
    } finally {
      isRefreshing = false;
    }
  }
);

export default apiClient;
