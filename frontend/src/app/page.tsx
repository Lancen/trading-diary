"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";

export default function Home() {
  const router = useRouter();
  const isDev = useAuth((s) => s.isDev);
  const user = useAuth((s) => s.user);
  const isLoading = useAuth((s) => s.isLoading);

  const shouldRedirect = isDev || !!user;

  useEffect(() => {
    if (!isLoading && shouldRedirect) {
      router.replace("/dashboard");
    }
  }, [isLoading, shouldRedirect, router]);

  // Show spinner while initializing
  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  // Will redirect via useEffect
  if (shouldRedirect) {
    return null;
  }

  return (
    <main className="flex min-h-screen flex-col items-center justify-center p-24">
      <h1 className="text-4xl font-bold">Trading Diary</h1>
      <p className="mt-4 text-lg text-muted-foreground">
        交易日记应用 — 记录每一笔交易，持续改进投资决策
      </p>
      <Link href="/login" className="mt-8">
        <Button size="lg">立即登录</Button>
      </Link>
    </main>
  );
}
