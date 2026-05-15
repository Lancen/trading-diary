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
            Dashboard
          </Link>

          <div className="pt-4">
            <div className="mb-1 px-3 text-xs font-semibold uppercase text-gray-400">
              Data Collection
            </div>
            <Link
              href="/admin/collection"
              className={`block rounded-lg px-6 py-2 text-sm font-medium transition-colors ${
                pathname === "/admin/collection"
                  ? "bg-primary text-primary-foreground"
                  : "text-gray-700 hover:bg-gray-200"
              }`}
            >
              Collection Status
            </Link>
            <Link
              href="/admin/collection/margin"
              className={`block rounded-lg px-6 py-2 text-sm font-medium transition-colors ${
                pathname === "/admin/collection/margin"
                  ? "bg-primary text-primary-foreground"
                  : "text-gray-700 hover:bg-gray-200"
              }`}
            >
              Margin Completeness
            </Link>
          </div>
        </nav>

        <Button
          variant="outline"
          className="w-full"
          onClick={handleLogout}
        >
          Logout
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
                Logout
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </header>

        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}
