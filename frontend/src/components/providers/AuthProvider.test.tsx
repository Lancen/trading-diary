import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";
import { AuthProvider } from "./AuthProvider";

const { mockUseAuth, mockState } = vi.hoisted(() => {
  const mockUseAuth = vi.fn();
  const mockState = {
    accessToken: null as string | null,
    isDev: false,
    fetchUser: vi.fn(),
    setLoading: vi.fn(),
  };
  return { mockUseAuth, mockState };
});

vi.mock("@/hooks/useAuth", () => ({
  useAuth: mockUseAuth,
}));

describe("AuthProvider", () => {
  beforeEach(() => {
    mockState.accessToken = null;
    mockState.isDev = false;
    mockState.fetchUser = vi.fn();
    mockState.setLoading = vi.fn();
    mockUseAuth.mockImplementation(
      (selector?: (s: typeof mockState) => unknown) => {
        return selector ? selector(mockState) : mockState;
      },
    );
  });

  it("已认证时渲染子组件", () => {
    mockState.accessToken = "valid-token";
    render(
      <AuthProvider>
        <div>子内容</div>
      </AuthProvider>,
    );
    expect(screen.getByText("子内容")).toBeInTheDocument();
  });

  it("无令牌且非开发模式时调用 setLoading(false)", () => {
    render(
      <AuthProvider>
        <div>子内容</div>
      </AuthProvider>,
    );
    expect(mockState.setLoading).toHaveBeenCalledWith(false);
    expect(mockState.fetchUser).not.toHaveBeenCalled();
  });

  it("有令牌时在挂载时获取用户信息", () => {
    mockState.accessToken = "valid-token";
    render(
      <AuthProvider>
        <div>子内容</div>
      </AuthProvider>,
    );
    expect(mockState.fetchUser).toHaveBeenCalledTimes(1);
  });

  it("开发模式下在挂载时获取用户信息", () => {
    mockState.isDev = true;
    render(
      <AuthProvider>
        <div>子内容</div>
      </AuthProvider>,
    );
    expect(mockState.fetchUser).toHaveBeenCalledTimes(1);
  });

  it("获取用户失败时仍渲染子组件", () => {
    mockState.accessToken = "bad-token";
    mockState.fetchUser = vi.fn().mockRejectedValue(new Error("网络错误"));
    mockUseAuth.mockImplementation(
      (selector?: (s: typeof mockState) => unknown) => {
        return selector ? selector(mockState) : mockState;
      },
    );
    render(
      <AuthProvider>
        <div>子内容</div>
      </AuthProvider>,
    );
    expect(screen.getByText("子内容")).toBeInTheDocument();
  });
});
