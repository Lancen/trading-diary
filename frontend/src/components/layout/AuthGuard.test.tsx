import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";
import { AuthGuard } from "./AuthGuard";

// 模拟 next/navigation
vi.mock("next/navigation", () => ({
  useRouter: () => ({
    replace: vi.fn(),
  }),
}));

// 共享可变状态，useAuth mock 返回此对象（支持 selector 模式）
let mockState: { isLoading: boolean; user: unknown; isDev: boolean };

const mockUseAuth = vi.fn();
vi.mock("@/hooks/useAuth", () => ({
  useAuth: mockUseAuth,
}));

describe("AuthGuard", () => {
  beforeEach(() => {
    mockState = { isLoading: false, user: null, isDev: false };
    mockUseAuth.mockImplementation(
      (selector?: (s: typeof mockState) => unknown) => {
        return selector ? selector(mockState) : mockState;
      },
    );
  });

  it("should show loading spinner when isLoading", () => {
    mockState.isLoading = true;
    render(
      <AuthGuard>
        <div>Content</div>
      </AuthGuard>,
    );
    expect(screen.queryByText("Content")).not.toBeInTheDocument();
  });

  it("should render children when user is authenticated", () => {
    mockState.user = { id: 1, username: "admin" };
    render(
      <AuthGuard>
        <div>Content</div>
      </AuthGuard>,
    );
    expect(screen.getByText("Content")).toBeInTheDocument();
  });

  it("should render children in dev mode without user", () => {
    mockState.isDev = true;
    render(
      <AuthGuard>
        <div>Content</div>
      </AuthGuard>,
    );
    expect(screen.getByText("Content")).toBeInTheDocument();
  });

  it("should show nothing when not loading, not dev, and no user", () => {
    const { container } = render(
      <AuthGuard>
        <div>Content</div>
      </AuthGuard>,
    );
    // 当 !isDev 且 !user 且加载完成时，组件返回 null
    expect(container.firstChild).toBeNull();
  });
});
