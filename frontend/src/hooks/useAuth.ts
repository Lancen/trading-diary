"use client";

import { create } from "zustand";
import * as authApi from "@/lib/auth";
import type { UserInfoVO } from "@/lib/auth";

const isBrowser = (): boolean => typeof window !== "undefined";

function getStoredToken(key: string): string | null {
  if (!isBrowser()) return null;
  return localStorage.getItem(key);
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserInfoVO | null;
  isLoading: boolean;
  isDev: boolean;

  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshAuth: () => Promise<void>;
  fetchUser: () => Promise<void>;
  setLoading: (loading: boolean) => void;
}

export const useAuth = create<AuthState>()((set, get) => ({
  accessToken: getStoredToken("accessToken"),
  refreshToken: getStoredToken("refreshToken"),
  user: null,
  isLoading: true,
  isDev: process.env.NEXT_PUBLIC_DEV_AUTO_LOGIN === "true",

  login: async (username: string, password: string) => {
    const tokenVO = await authApi.login(username, password);
    if (isBrowser()) {
      localStorage.setItem("accessToken", tokenVO.accessToken);
      localStorage.setItem("refreshToken", tokenVO.refreshToken);
    }
    set({
      accessToken: tokenVO.accessToken,
      refreshToken: tokenVO.refreshToken,
    });

    const user = await authApi.getMe();
    set({ user });
  },

  logout: async () => {
    try {
      await authApi.logout();
    } catch {
      // 吞掉错误 — 令牌可能已失效
    }
    if (isBrowser()) {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("refreshToken");
    }
    set({
      accessToken: null,
      refreshToken: null,
      user: null,
      isLoading: false,
    });
    if (isBrowser()) {
      window.location.href = "/login";
    }
  },

  refreshAuth: async () => {
    const currentRefreshToken = get().refreshToken;
    if (!currentRefreshToken) return;

    const tokenVO = await authApi.refreshToken(currentRefreshToken);
    if (isBrowser()) {
      localStorage.setItem("accessToken", tokenVO.accessToken);
      localStorage.setItem("refreshToken", tokenVO.refreshToken);
    }
    set({
      accessToken: tokenVO.accessToken,
      refreshToken: tokenVO.refreshToken,
    });
  },

  fetchUser: async () => {
    try {
      const user = await authApi.getMe();
      set({ user, isLoading: false });
    } catch {
      set({ isLoading: false });
    }
  },

  setLoading: (loading: boolean) => {
    set({ isLoading: loading });
  },
}));
