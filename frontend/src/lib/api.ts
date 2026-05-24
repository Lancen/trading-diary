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
        // 仅处理 401，跳过认证端点
        if (response.status !== 401 || request.url.includes("/auth/refresh")) {
          return;
        }

        // 防止并发刷新
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

            // 用新令牌重试原始请求
            request.headers.set(
              "Authorization",
              `Bearer ${refreshResponse.data.accessToken}`
            );
            return ky(request);
          }
        } catch {
          // 刷新失败 — 继续执行清理
        } finally {
          isRefreshing = false;
        }

        // 清除令牌并重定向到登录页
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
