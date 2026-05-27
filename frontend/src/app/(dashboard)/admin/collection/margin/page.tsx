"use client";

import { useState } from "react";
import { useApiQuery, useApiMutation } from "@/lib/hooks";
import type { GapReport, BackfillForm } from "@/lib/types";

export default function MarginDataPage() {
  const [backfillStart, setBackfillStart] = useState("");
  const [backfillEnd, setBackfillEnd] = useState("");

  const { data: sseGapData, isLoading: sseLoading } = useApiQuery<GapReport>(
    ["admin", "collection", "gap", "MARGIN_DAILY_SSE"],
    "api/v1/admin/collection/gap-report",
    { dataType: "MARGIN_DAILY_SSE" }
  );

  const { data: szseGapData, isLoading: szseLoading } = useApiQuery<GapReport>(
    ["admin", "collection", "gap", "MARGIN_DAILY_SZSE"],
    "api/v1/admin/collection/gap-report",
    { dataType: "MARGIN_DAILY_SZSE" }
  );

  const backfillMutation = useApiMutation<void, BackfillForm>(
    "post",
    "api/v1/admin/collection/backfill",
    [
      ["admin", "collection", "gap", "MARGIN_DAILY_SSE"],
      ["admin", "collection", "gap", "MARGIN_DAILY_SZSE"],
    ]
  );

  const sseGap = sseGapData?.data;
  const szseGap = szseGapData?.data;

  function handleBackfill(dataType: string) {
    if (!backfillStart || !backfillEnd) return;
    backfillMutation.mutate({ dataType, startDate: backfillStart, endDate: backfillEnd });
  }

  return (
    <div className="space-y-6">
      <a href="/admin/collection" className="text-sm text-gray-400 hover:text-blue-600">← 数据采集</a>
      <h1 className="text-2xl font-bold">两融数据完整性</h1>

      {/* 补采表单 */}
      <div className="rounded-lg border bg-white p-4">
        <h2 className="text-sm font-semibold mb-3">补采数据</h2>
        <div className="flex gap-3 items-end">
          <div>
            <label className="block text-xs text-gray-500 mb-1">开始日期</label>
            <input type="date" value={backfillStart} onChange={(e) => setBackfillStart(e.target.value)} className="rounded border px-3 py-2 text-sm" />
          </div>
          <div>
            <label className="block text-xs text-gray-500 mb-1">结束日期</label>
            <input type="date" value={backfillEnd} onChange={(e) => setBackfillEnd(e.target.value)} className="rounded border px-3 py-2 text-sm" />
          </div>
          <button onClick={() => handleBackfill("MARGIN_DAILY_SSE")} disabled={backfillMutation.isPending} className="rounded-lg bg-blue-600 px-4 py-2 text-sm text-white hover:opacity-90 disabled:opacity-50">补采沪市</button>
          <button onClick={() => handleBackfill("MARGIN_DAILY_SZSE")} disabled={backfillMutation.isPending} className="rounded-lg bg-blue-600 px-4 py-2 text-sm text-white hover:opacity-90 disabled:opacity-50">补采深市</button>
        </div>
      </div>

      {/* 沪市缺口 */}
      {sseGap && (
        <div className="rounded-lg border bg-white p-4">
          <h2 className="text-sm font-semibold mb-2">沪市两融缺口</h2>
          <div className="text-sm mb-3">
            完整周 {sseGap.completeWeeks} / {sseGap.totalWeeks}，
            缺失 {sseGap.missingWeeks} 周，不完整 {sseGap.partialWeeks} 周
          </div>
          <GapWeeks weeks={sseGap.weeks} />
        </div>
      )}

      {/* 深市缺口 */}
      {szseGap && (
        <div className="rounded-lg border bg-white p-4">
          <h2 className="text-sm font-semibold mb-2">深市两融缺口</h2>
          <div className="text-sm mb-3">
            完整周 {szseGap.completeWeeks} / {szseGap.totalWeeks}，
            缺失 {szseGap.missingWeeks} 周，不完整 {szseGap.partialWeeks} 周
          </div>
          <GapWeeks weeks={szseGap.weeks} />
        </div>
      )}
    </div>
  );
}

function GapWeeks({ weeks }: { weeks: GapReport["weeks"] }) {
  if (weeks.length === 0) return <div className="text-sm text-gray-400">无缺口数据</div>;
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-xs">
        <thead>
          <tr className="border-b text-gray-500">
            <th className="px-2 py-1 text-left">周起始</th>
            <th className="px-2 py-1 text-left">周结束</th>
            <th className="px-2 py-1 text-right">应采</th>
            <th className="px-2 py-1 text-right">已采</th>
            <th className="px-2 py-1 text-left">状态</th>
            <th className="px-2 py-1 text-left">缺失日期</th>
          </tr>
        </thead>
        <tbody>
          {weeks.map((w) => (
            <tr key={w.weekStart} className={`border-b ${w.status === "MISSING" ? "bg-red-50" : w.status === "PARTIAL" ? "bg-yellow-50" : ""}`}>
              <td className="px-2 py-1">{w.weekStart}</td>
              <td className="px-2 py-1">{w.weekEnd}</td>
              <td className="px-2 py-1 text-right">{w.expectedDays}</td>
              <td className="px-2 py-1 text-right">{w.collectedDays}</td>
              <td className="px-2 py-1">{w.status}</td>
              <td className="px-2 py-1 text-red-500">{w.missingDates.join(", ")}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
