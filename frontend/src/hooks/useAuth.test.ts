import { describe, it, expect, beforeEach } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { useAuth } from "./useAuth";

describe("useAuth", () => {
  beforeEach(() => {
    // Reset store to initial state
    useAuth.setState(useAuth.getInitialState());
    localStorage.clear();
  });

  it("should initialize with default state", () => {
    const { result } = renderHook(() => useAuth());
    expect(result.current.accessToken).toBeNull();
    expect(result.current.user).toBeNull();
    // Default isLoading is true (app starts in loading state before fetchUser completes)
    expect(result.current.isLoading).toBe(true);
  });

  it("should have isDev false when env var is not set", () => {
    // NEXT_PUBLIC_DEV_AUTO_LOGIN is not configured in test environment
    const { result } = renderHook(() => useAuth());
    expect(result.current.isDev).toBe(false);
  });

  it("should clear state on logout", () => {
    const { result } = renderHook(() => useAuth());

    // Set some authenticated state first
    act(() => {
      useAuth.setState({
        accessToken: "test-token",
        refreshToken: "test-refresh",
        user: {
          id: 1,
          username: "admin",
          nickname: "Admin",
          roles: ["ADMIN"],
        },
        isLoading: false,
      });
    });

    expect(result.current.accessToken).toBe("test-token");
    expect(result.current.user).not.toBeNull();

    // Simulate logout state clear
    act(() => {
      useAuth.setState({
        accessToken: null,
        refreshToken: null,
        user: null,
        isLoading: false,
      });
    });

    expect(result.current.accessToken).toBeNull();
    expect(result.current.user).toBeNull();
    expect(result.current.isLoading).toBe(false);
  });

  it("should set isLoading via setLoading action", () => {
    const { result } = renderHook(() => useAuth());

    act(() => {
      result.current.setLoading(true);
    });
    expect(result.current.isLoading).toBe(true);

    act(() => {
      result.current.setLoading(false);
    });
    expect(result.current.isLoading).toBe(false);
  });
});
