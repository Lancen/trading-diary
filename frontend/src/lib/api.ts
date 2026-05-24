import ky from "ky";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

const isBrowser = (): boolean => typeof window !== "undefined";

let isRefreshing = false;

const api = ky.create({
  prefixUrl: API_BASE_URL,
  timeout: 10000,
  hooks: {
    beforeRequest: [
      (request) => {
        if (isBrowser()) {
          const token = localStorage.getItem("accessToken");
          if (token) {
            request.headers.set("Authorization", `Bearer ${token}`);
          }
        }
      },
    ],
    afterResponse: [
      async (request, _options, response) => {
        // Only handle 401, skip auth endpoints
        if (response.status !== 401 || request.url.includes("/auth/refresh")) {
          return;
        }

        // Prevent concurrent refresh attempts
        if (isRefreshing) {
          return;
        }

        isRefreshing = true;

        try {
          const storedRefreshToken = isBrowser()
            ? localStorage.getItem("refreshToken")
            : null;

          if (!storedRefreshToken) {
            throw new Error("No refresh token available");
          }

          const refreshResponse = await ky
            .post(`${API_BASE_URL}/api/v1/auth/refresh`, {
              json: { refreshToken: storedRefreshToken },
              timeout: 10000,
            })
            .json<{
              code: number;
              data: { accessToken: string; refreshToken: string; expiresIn: number };
            }>();

          if (refreshResponse.code === 200 && refreshResponse.data) {
            localStorage.setItem("accessToken", refreshResponse.data.accessToken);
            localStorage.setItem("refreshToken", refreshResponse.data.refreshToken);

            // Retry the original request with the new token
            request.headers.set(
              "Authorization",
              `Bearer ${refreshResponse.data.accessToken}`
            );
            return ky(request);
          }
        } catch {
          // Refresh failed — fall through to cleanup
        } finally {
          isRefreshing = false;
        }

        // Clear tokens and redirect to login
        if (isBrowser()) {
          localStorage.removeItem("accessToken");
          localStorage.removeItem("refreshToken");
          window.location.href = "/login";
        }
      },
    ],
  },
});

export default api;
