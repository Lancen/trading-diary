"use client";

import { useApiQuery } from "@/lib/hooks";
import type { CollectionStatus, CookieStatus } from "@/lib/types";
import { statusVariant, statusLabel, formatTime, dataTypeLabel } from "@/lib/format";

function Badge({ variant, children }: { variant: string; children: React.ReactNode }) {
  const colors: Record<string, string> = {
    default: "bg-gray-100 text-gray-800",
    secondary: "bg-blue-100 text-blue-800",
    destructive: "bg-red-100 text-red-800",
    outline: "bg-white text-gray-600 border",
  };
  return <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${colors[variant] || colors.outline}`}>{children}</span>;
}

export default function CollectionPage() {
  const { data: statusData, isLoading } = useApiQuery<CollectionStatus[]>(
    ["admin", "collection", "status"],
    "api/v1/admin/collection/status"
  );

  const { data: cookieData } = useApiQuery<CookieStatus>(
    ["admin", "collection", "cookie"],
    "api/v1/admin/collection/cookie-status"
  );

  const statuses = statusData?.data || [];
  const cookie = cookieData?.data;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">数据采集</h1>

      {cookie && (
        <div className="rounded-lg border bg-white p-4 flex items-center gap-4">
          <Badge variant={cookie.hasCookie ? "default" : "destructive"}>
            {cookie.hasCookie ? "Cookie 有效" : "Cookie 缺失"}
          </Badge>
          {cookie.hasCookie && <span className="text-xs text-gray-400 truncate max-w-[300px]">{cookie.cookiePreview}</span>}
          {cookie.updatedAt && <span className="text-xs text-gray-400">更新于 {formatTime(cookie.updatedAt)}</span>}
        </div>
      )}

      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b bg-gray-50 text-xs text-gray-500">
              <th className="px-4 py-2 text-left">数据类型</th>
              <th className="px-4 py-2 text-left">最近采集</th>
              <th className="px-4 py-2 text-left">采集状态</th>
              <th className="px-4 py-2 text-left">最近清洗</th>
              <th className="px-4 py-2 text-left">清洗状态</th>
              <th className="px-4 py-2 text-left">数据截止</th>
              <th className="px-4 py-2 w-20">操作</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">加载中...</td></tr>
            ) : statuses.length === 0 ? (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">无数据</td></tr>
            ) : statuses.map((s) => (
              <tr key={s.dataType} className="border-b hover:bg-gray-50">
                <td className="px-4 py-2 font-medium">{s.dataTypeLabel || dataTypeLabel(s.dataType)}</td>
                <td className="px-4 py-2 text-xs">{s.lastFetch?.startedAt ? formatTime(s.lastFetch.startedAt) : "-"}</td>
                <td className="px-4 py-2"><Badge variant={statusVariant(s.lastFetch?.status)}>{statusLabel(s.lastFetch?.status)}</Badge></td>
                <td className="px-4 py-2 text-xs">{s.lastCleanse?.startedAt ? formatTime(s.lastCleanse.startedAt) : "-"}</td>
                <td className="px-4 py-2"><Badge variant={statusVariant(s.lastCleanse?.status)}>{statusLabel(s.lastCleanse?.status)}</Badge></td>
                <td className="px-4 py-2 text-xs">{s.lastDataDate || "-"}</td>
                <td className="px-4 py-2">
                  <a href={`/admin/collection/${s.dataType}`} className="text-xs text-blue-600 hover:underline">详情</a>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
