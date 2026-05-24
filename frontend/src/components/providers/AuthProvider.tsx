"use client";

import { useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const accessToken = useAuth((s) => s.accessToken);
  const isDev = useAuth((s) => s.isDev);
  const fetchUser = useAuth((s) => s.fetchUser);
  const setLoading = useAuth((s) => s.setLoading);

  useEffect(() => {
    if (isDev) {
      // 开发模式：后端 AutoLoginFilter 处理认证，直接获取当前用户。
      fetchUser();
    } else if (!accessToken) {
      // 无已存储令牌 — 用户需要登录。
      setLoading(false);
    } else {
      // 有已存储令牌 — 通过获取用户信息验证。
      fetchUser();
    }
    // 仅在挂载时执行
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <>{children}</>;
}
