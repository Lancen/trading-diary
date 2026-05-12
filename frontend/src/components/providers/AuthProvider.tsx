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
      // Dev mode: backend AutoLoginFilter handles auth,
      // just fetch the current user directly.
      fetchUser();
    } else if (!accessToken) {
      // No stored token — user needs to log in.
      setLoading(false);
    } else {
      // Has stored token — verify it by fetching user.
      fetchUser();
    }
    // Run once on mount
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <>{children}</>;
}
