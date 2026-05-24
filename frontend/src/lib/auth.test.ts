import { describe, it, expect, beforeEach, vi } from "vitest";

// 模拟 api 模块，记录各方法调用
const mockPost = vi.fn();
const mockGet = vi.fn();

vi.mock("./api", () => ({
  default: {
    post: (...args: unknown[]) => mockPost(...args),
    get: (...args: unknown[]) => mockGet(...args),
  },
}));

describe("auth 模块", () => {
  beforeEach(() => {
    vi.resetModules();
    mockPost.mockReset();
    mockGet.mockReset();
  });

  it("login() 调用正确的端点并传递凭据", async () => {
    const tokenData = {
      accessToken: "at-123",
      refreshToken: "rt-456",
      expiresIn: 3600,
    };
    mockPost.mockReturnValue({
      json: vi.fn().mockResolvedValue({ code: 200, data: tokenData }),
    });

    const { login } = await import("./auth");
    const result = await login("admin", "password123");

    expect(mockPost).toHaveBeenCalledWith(
      "api/v1/auth/login",
      expect.objectContaining({
        json: { username: "admin", password: "password123" },
      }),
    );
    expect(result).toEqual(tokenData);
  });

  it("refreshToken() 调用正确的端点并传递 refresh token", async () => {
    const tokenData = {
      accessToken: "new-at",
      refreshToken: "new-rt",
      expiresIn: 3600,
    };
    mockPost.mockReturnValue({
      json: vi.fn().mockResolvedValue({ code: 200, data: tokenData }),
    });

    const { refreshToken } = await import("./auth");
    const result = await refreshToken("old-rt-value");

    expect(mockPost).toHaveBeenCalledWith(
      "api/v1/auth/refresh",
      expect.objectContaining({
        json: { refreshToken: "old-rt-value" },
      }),
    );
    expect(result).toEqual(tokenData);
  });

  it("logout() 调用正确的端点", async () => {
    mockPost.mockReturnValue({
      json: vi.fn().mockResolvedValue({ code: 200, data: null }),
    });

    const { logout } = await import("./auth");
    await logout();

    expect(mockPost).toHaveBeenCalledWith("api/v1/auth/logout");
  });

  it("getMe() 调用正确的端点", async () => {
    const userData = {
      id: 1,
      username: "admin",
      nickname: "管理员",
      roles: ["ADMIN"],
    };
    mockGet.mockReturnValue({
      json: vi.fn().mockResolvedValue({ code: 200, data: userData }),
    });

    const { getMe } = await import("./auth");
    const result = await getMe();

    expect(mockGet).toHaveBeenCalledWith("api/v1/auth/me");
    expect(result).toEqual(userData);
  });
});
