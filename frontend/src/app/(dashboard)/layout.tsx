"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { AuthGuard } from "@/components/layout/AuthGuard";
import { useAuth } from "@/hooks/useAuth";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Toaster } from "@/components/ui/toast";

function isActive(pathname: string, href: string, exact: boolean = false) {
  if (exact) return pathname === href;
  return pathname === href || pathname.startsWith(href + "/");
}

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthGuard>
      <DashboardInner>{children}</DashboardInner>
    </AuthGuard>
  );
}

function DashboardInner({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const user = useAuth((s) => s.user);
  const logout = useAuth((s) => s.logout);

  const initials =
    user?.nickname?.charAt(0) || user?.username?.charAt(0) || "U";

  async function handleLogout() {
    await logout();
  }

  return (
    <div className="flex min-h-screen">
      {/* Sidebar */}
      <aside className="flex w-64 flex-col border-r bg-gray-50 p-4">
        <div className="mb-8">
          <Link href="/dashboard" className="text-xl font-bold">
            Trading Diary
          </Link>
        </div>

        <nav className="flex-1 space-y-1">
          <Link
            href="/dashboard"
            className={`block rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
              pathname === "/dashboard"
                ? "bg-primary text-primary-foreground"
                : "text-gray-700 hover:bg-gray-200"
            }`}
          >
            控制台
          </Link>

          <div className="pt-4">
            <div className="mb-1 px-3 text-xs font-semibold uppercase text-gray-400">
              基础数据管理
            </div>
            <Link
              href="/admin/collection"
              className={`block rounded-lg px-6 py-2 text-sm font-medium transition-colors ${
                isActive(pathname, "/admin/collection") &&
                !pathname.startsWith("/admin/collection/margin")
                  ? "bg-primary text-primary-foreground"
                  : "text-gray-700 hover:bg-gray-200"
              }`}
            >
              数据采集
            </Link>
            <Link
              href="/admin/collection/margin"
              className={`block rounded-lg px-6 py-2 text-sm font-medium transition-colors ${
                isActive(pathname, "/admin/collection/margin", true)
                  ? "bg-primary text-primary-foreground"
                  : "text-gray-700 hover:bg-gray-200"
              }`}
            >
              两融完整性
            </Link>
          </div>

          <div className="pt-4">
            <div className="mb-1 px-3 text-xs font-semibold uppercase text-gray-400">
              数据浏览
            </div>
            <Link
              href="/admin/stocks"
              className={`block rounded-lg px-6 py-2 text-sm font-medium transition-colors ${
                isActive(pathname, "/admin/stocks")
                  ? "bg-primary text-primary-foreground"
                  : "text-gray-700 hover:bg-gray-200"
              }`}
            >
              股票数据
            </Link>
            <Link
              href="/admin/concepts"
              className={`block rounded-lg px-6 py-2 text-sm font-medium transition-colors ${
                isActive(pathname, "/admin/concepts", true)
                  ? "bg-primary text-primary-foreground"
                  : "text-gray-700 hover:bg-gray-200"
              }`}
            >
              概念列表
            </Link>
            <Link
              href="/admin/industries"
              className={`block rounded-lg px-6 py-2 text-sm font-medium transition-colors ${
                isActive(pathname, "/admin/industries", true)
                  ? "bg-primary text-primary-foreground"
                  : "text-gray-700 hover:bg-gray-200"
              }`}
            >
              行业列表
            </Link>
            <Link
              href="/admin/margin-stats"
              className={`block rounded-lg px-6 py-2 text-sm font-medium transition-colors ${
                isActive(pathname, "/admin/margin-stats")
                  ? "bg-primary text-primary-foreground"
                  : "text-gray-700 hover:bg-gray-200"
              }`}
            >
              融资统计
            </Link>
          </div>
        </nav>

        <Button
          variant="outline"
          className="w-full"
          onClick={handleLogout}
        >
          退出登录
        </Button>
      </aside>

      {/* Main area */}
      <div className="flex flex-1 flex-col">
        {/* Top bar */}
        <header className="flex items-center justify-end border-b px-6 py-3">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <button className="flex items-center gap-2 rounded-full transition-opacity hover:opacity-80">
                <Avatar className="h-8 w-8">
                  <AvatarFallback>{initials}</AvatarFallback>
                </Avatar>
                <span className="text-sm font-medium">
                  {user?.nickname || user?.username || ""}
                </span>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={handleLogout}>
                退出登录
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </header>

        <main className="flex-1 p-6">{children}</main>
      </div>
      <Toaster />
    </div>
  );
}
