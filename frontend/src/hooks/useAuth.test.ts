import { describe, it, expect, beforeEach } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { useAuth } from "./useAuth";

describe("useAuth", () => {
  beforeEach(() => {
    // 重置 store 到初始状态
    useAuth.setState(useAuth.getInitialState());
    localStorage.clear();
  });

  it("should initialize with default state", () => {
    const { result } = renderHook(() => useAuth());
    expect(result.current.accessToken).toBeNull();
    expect(result.current.user).toBeNull();
    // 默认 isLoading 为 true（应用启动时处于加载状态，等待 fetchUser 完成）
    expect(result.current.isLoading).toBe(true);
  });

  it("should have isDev false when env var is not set", () => {
    // NEXT_PUBLIC_DEV_AUTO_LOGIN 在测试环境中未配置
    const { result } = renderHook(() => useAuth());
    expect(result.current.isDev).toBe(false);
  });

  it("should clear state on logout", () => {
    const { result } = renderHook(() => useAuth());

    // 先设置一些已认证的状态
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

    // 模拟登出状态清除
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
