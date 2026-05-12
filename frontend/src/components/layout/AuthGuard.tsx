"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const isLoading = useAuth((s) => s.isLoading);
  const isDev = useAuth((s) => s.isDev);
  const user = useAuth((s) => s.user);

  useEffect(() => {
    if (!isLoading && !isDev && !user) {
      router.replace("/login");
    }
  }, [isLoading, isDev, user, router]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!isDev && !user) {
    return null;
  }

  return <>{children}</>;
}
