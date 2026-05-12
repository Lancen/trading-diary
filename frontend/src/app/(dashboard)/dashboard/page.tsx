"use client";

import { useAuth } from "@/hooks/useAuth";
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from "@/components/ui/card";

export default function DashboardPage() {
  const user = useAuth((s) => s.user);

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold">控制台</h1>

      <Card>
        <CardHeader>
          <CardTitle>欢迎使用 Trading Diary</CardTitle>
          <CardDescription>
            记录每一笔交易，持续改进投资决策
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            <p>
              <span className="font-medium">用户名：</span>
              {user?.nickname || user?.username || "未知用户"}
            </p>
            <p>
              <span className="font-medium">角色：</span>
              {user?.roles?.length ? user.roles.join("、") : "无"}
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
