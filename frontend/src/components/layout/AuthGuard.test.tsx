import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom/vitest";
import { AuthGuard } from "./AuthGuard";

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    replace: vi.fn(),
  }),
}));

const { mockUseAuth, mockState } = vi.hoisted(() => {
  const mockUseAuth = vi.fn();
  const mockState = {
    isLoading: false,
    user: null as unknown,
    isDev: false,
  };
  return { mockUseAuth, mockState };
});

vi.mock("@/hooks/useAuth", () => ({
  useAuth: mockUseAuth,
}));

describe("AuthGuard", () => {
  beforeEach(() => {
    mockState.isLoading = false;
    mockState.user = null;
    mockState.isDev = false;
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
    expect(container.firstChild).toBeNull();
  });
});
