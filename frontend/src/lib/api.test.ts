/* eslint-disable @typescript-eslint/no-explicit-any */
import { describe, it, expect, beforeEach, vi } from "vitest";

const { mockKyCreate, mockKyPost, mockKyFn } = vi.hoisted(() => {
  const mockKyCreate = vi.fn();
  const mockKyPost = vi.fn();
  const mockKyFn = vi.fn(() => "retried-request");
  return { mockKyCreate, mockKyPost, mockKyFn };
});

vi.mock("ky", () => ({
  default: Object.assign(
    (...args: unknown[]) => mockKyFn(...args),
    {
      create: (...args: unknown[]) => mockKyCreate(...args),
      post: (...args: unknown[]) => mockKyPost(...args),
    },
  ),
}));

describe("api", () => {
  let capturedHooks: {
    beforeRequest: Array<(req: any) => void>;
    afterResponse: Array<(req: any, opts: any, res: any) => Promise<any>>;
  };

  beforeEach(() => {
    localStorage.clear();
    mockKyCreate.mockReset();
    mockKyPost.mockReset();
    mockKyFn.mockReset();
    mockKyFn.mockReturnValue("retried-request");

    capturedHooks = { beforeRequest: [], afterResponse: [] };
    mockKyCreate.mockImplementation((options: any) => {
      capturedHooks.beforeRequest = options.hooks.beforeRequest;
      capturedHooks.afterResponse = options.hooks.afterResponse;
      return options;
    });
  });

  async function loadApi() {
    vi.resetModules();
    const mod = await import("./api");
    return mod.default;
  }

  it("使用正确的 base URL 创建 ky 实例", async () => {
    await loadApi();
    expect(mockKyCreate).toHaveBeenCalled();
    const callArgs = mockKyCreate.mock.calls[0][0];
    expect(callArgs.prefixUrl).toBe("http://localhost:8080");
    expect(callArgs.timeout).toBe(10000);
  });

  it("令牌存在时在请求头中包含 Authorization", async () => {
    await loadApi();
    localStorage.setItem("accessToken", "test-token-123");

    const mockRequest = { headers: { set: vi.fn() } };
    capturedHooks.beforeRequest[0](mockRequest);

    expect(mockRequest.headers.set).toHaveBeenCalledWith(
      "Authorization",
      "Bearer test-token-123",
    );
  });

  it("令牌不存在时不设置 Authorization 头", async () => {
    await loadApi();

    const mockRequest = { headers: { set: vi.fn() } };
    capturedHooks.beforeRequest[0](mockRequest);

    expect(mockRequest.headers.set).not.toHaveBeenCalled();
  });

  it("401 响应时尝试刷新令牌", async () => {
    const refreshJson = vi.fn().mockResolvedValue({
      code: 200,
      data: {
        accessToken: "new-access-token",
        refreshToken: "new-refresh-token",
        expiresIn: 3600,
      },
    });
    mockKyPost.mockReturnValue({ json: refreshJson });

    await loadApi();
    localStorage.setItem("refreshToken", "old-refresh-token");

    const mockRequest = {
      url: "http://localhost:8080/api/v1/some-resource",
      headers: { set: vi.fn() },
    };
    const mockResponse = { status: 401 };

    await capturedHooks.afterResponse[0](mockRequest, {}, mockResponse);

    expect(mockKyPost).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/auth/refresh",
      expect.objectContaining({
        json: { refreshToken: "old-refresh-token" },
      }),
    );
    expect(localStorage.getItem("accessToken")).toBe("new-access-token");
    expect(localStorage.getItem("refreshToken")).toBe("new-refresh-token");
    expect(mockRequest.headers.set).toHaveBeenCalledWith(
      "Authorization",
      "Bearer new-access-token",
    );
  });

  it("刷新失败时清除令牌并重定向到登录页", async () => {
    mockKyPost.mockReturnValue({
      json: vi.fn().mockRejectedValue(new Error("刷新失败")),
    });

    const originalLocation = window.location;
    delete (window as any).location;
    (window as any).location = { href: "" };

    await loadApi();
    localStorage.setItem("accessToken", "old-access");
    localStorage.setItem("refreshToken", "old-refresh");

    const mockRequest = {
      url: "http://localhost:8080/api/v1/some-resource",
      headers: { set: vi.fn() },
    };
    const mockResponse = { status: 401 };

    await capturedHooks.afterResponse[0](mockRequest, {}, mockResponse);

    expect(localStorage.getItem("accessToken")).toBeNull();
    expect(localStorage.getItem("refreshToken")).toBeNull();
    expect(window.location.href).toBe("/login");

    (window as any).location = originalLocation;
  });

  it("非 401 响应不触发刷新逻辑", async () => {
    await loadApi();

    const mockRequest = {
      url: "http://localhost:8080/api/v1/some-resource",
      headers: { set: vi.fn() },
    };
    const mockResponse = { status: 200 };

    const result = await capturedHooks.afterResponse[0](
      mockRequest,
      {},
      mockResponse,
    );

    expect(result).toBeUndefined();
  });

  it("刷新端点自身 401 不触发递归刷新", async () => {
    await loadApi();

    const mockRequest = {
      url: "http://localhost:8080/api/v1/auth/refresh",
      headers: { set: vi.fn() },
    };
    const mockResponse = { status: 401 };

    const result = await capturedHooks.afterResponse[0](
      mockRequest,
      {},
      mockResponse,
    );

    expect(result).toBeUndefined();
  });
});
