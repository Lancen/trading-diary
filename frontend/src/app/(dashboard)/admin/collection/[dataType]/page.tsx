"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { useApiQuery, useApiMutation } from "@/lib/hooks";
import type { StatusItem, CalendarDay, CollectionLog, GapReport } from "@/lib/types";
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

export default function CollectionDataTypePage() {
  const params = useParams();
  const dataType = params.dataType as string;
  const [calMonth, setCalMonth] = useState(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  });

  // 状态
  const { data: statusData } = useApiQuery<StatusItem[]>(
    ["admin", "collection", "status"],
    "api/v1/admin/collection/status"
  );

  // 日历
  const { data: calendarData } = useApiQuery<CalendarDay[]>(
    ["admin", "collection", "calendar", dataType],
    "api/v1/admin/stocks/calendar",
    { dataType, month: calMonth }
  );

  // 日志
  const { data: logsData } = useApiQuery<CollectionLog[]>(
    ["admin", "collection", "logs", dataType],
    "api/v1/admin/collection/logs",
    { dataType }
  );

  // 缺口报告
  const { data: gapData } = useApiQuery<GapReport>(
    ["admin", "collection", "gap", dataType],
    "api/v1/admin/collection/gap-report",
    { dataType }
  );

  // 采集触发
  const fetchMutation = useApiMutation<void, { dataType: string; tradeDate?: string }>(
    "post",
    "api/v1/admin/collection/fetch",
    [["admin", "collection", "status"], ["admin", "collection", "calendar", dataType]]
  );

  // 清洗触发
  const cleanseMutation = useApiMutation<void, { dataType: string; rawLogId?: number }>(
    "post",
    "api/v1/admin/collection/cleanse",
    [["admin", "collection", "status"]]
  );

  const currentStatus = statusData?.data?.find(s => s.dataType === dataType);
  const calendar = calendarData?.data || [];
  const logs = logsData?.data || [];
  const gapReport = gapData?.data;

  return (
    <div className="space-y-6">
      <a href="/admin/collection" className="text-sm text-gray-400 hover:text-blue-600">← 数据采集</a>
      <h1 className="text-2xl font-bold">{dataTypeLabel(dataType)}</h1>

      {/* 操作按钮 */}
      <div className="flex gap-3">
        <button
          onClick={() => fetchMutation.mutate({ dataType })}
          disabled={fetchMutation.isPending}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50"
        >
          {fetchMutation.isPending ? "采集中..." : "采集"}
        </button>
        <button
          onClick={() => cleanseMutation.mutate({ dataType })}
          disabled={cleanseMutation.isPending}
          className="rounded-lg border px-4 py-2 text-sm font-medium hover:bg-gray-50 disabled:opacity-50"
        >
          {cleanseMutation.isPending ? "清洗中..." : "清洗"}
        </button>
      </div>

      {/* 状态摘要 */}
      {currentStatus && (
        <div className="grid grid-cols-3 gap-4">
          <div className="rounded-lg border bg-white p-4">
            <div className="text-xs text-gray-500 mb-1">最近采集</div>
            <Badge variant={statusVariant(currentStatus.lastFetch?.status)}>{statusLabel(currentStatus.lastFetch?.status)}</Badge>
            {currentStatus.lastFetch?.startedAt && <span className="text-xs text-gray-400 ml-2">{formatTime(currentStatus.lastFetch.startedAt)}</span>}
          </div>
          <div className="rounded-lg border bg-white p-4">
            <div className="text-xs text-gray-500 mb-1">数据截止</div>
            <span className="text-lg font-bold">{currentStatus.lastDataDate || "-"}</span>
          </div>
          <div className="rounded-lg border bg-white p-4">
            <div className="text-xs text-gray-500 mb-1">采集记录数</div>
            <span className="text-lg font-bold">{currentStatus.lastFetch?.recordCount ?? "-"}</span>
          </div>
        </div>
      )}

      {/* 交易日历 */}
      <div className="rounded-lg border bg-white p-4">
        <div className="flex items-center gap-3 mb-3">
          <h2 className="text-sm font-semibold">交易日历</h2>
          <input type="month" value={calMonth} onChange={(e) => setCalMonth(e.target.value)} className="rounded border px-2 py-1 text-sm" />
        </div>
        <div className="grid grid-cols-7 gap-1">
          {calendar.map((day) => (
            <div key={day.date} className={`rounded px-2 py-1 text-xs text-center ${day.status === "COLLECTED" ? "bg-green-100 text-green-800" : day.status === "MISSING" ? "bg-red-100 text-red-800" : day.tradingDay ? "bg-yellow-50 text-yellow-700" : "bg-gray-50 text-gray-400"}`}>
              {day.date.slice(8)}
            </div>
          ))}
        </div>
      </div>

      {/* 缺口报告 */}
      {gapReport && gapReport.partialWeeks + gapReport.missingWeeks > 0 && (
        <div className="rounded-lg border bg-white p-4">
          <h2 className="text-sm font-semibold mb-2">数据缺口</h2>
          <div className="text-sm">
            完整周 {gapReport.completeWeeks} / {gapReport.totalWeeks}，
            缺失 {gapReport.missingWeeks} 周，不完整 {gapReport.partialWeeks} 周
          </div>
        </div>
      )}

      {/* 采集日志 */}
      <div className="rounded-lg border bg-white p-4">
        <h2 className="text-sm font-semibold mb-2">采集日志</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr className="border-b text-gray-500">
                <th className="px-2 py-1 text-left">时间</th>
                <th className="px-2 py-1 text-left">类型</th>
                <th className="px-2 py-1 text-left">状态</th>
                <th className="px-2 py-1 text-right">记录数</th>
                <th className="px-2 py-1 text-left">错误</th>
              </tr>
            </thead>
            <tbody>
              {logs.slice(0, 20).map((log) => (
                <tr key={log.id} className="border-b">
                  <td className="px-2 py-1">{log.startedAt ? formatTime(log.startedAt) : "-"}</td>
                  <td className="px-2 py-1">{log.jobType}</td>
                  <td className="px-2 py-1"><Badge variant={statusVariant(log.status)}>{statusLabel(log.status)}</Badge></td>
                  <td className="px-2 py-1 text-right">{log.recordCount ?? "-"}</td>
                  <td className="px-2 py-1 text-red-500 truncate max-w-[200px]">{log.errorMsg || ""}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
