"use client";

import { useState } from "react";
import api from "@/lib/api";

interface WeekGap {
  weekStart: string;
  weekEnd: string;
  expectedDays: number;
  collectedDays: number;
  missingDates: string[];
  status: string;
}

interface GapReport {
  weeks: WeekGap[];
  totalWeeks: number;
  completeWeeks: number;
  partialWeeks: number;
  missingWeeks: number;
}

function statusEmoji(status: string): string {
  if (status === "COMPLETE") return "COMPLETE";
  if (status === "PARTIAL") return "PARTIAL";
  if (status === "MISSING") return "MISSING";
  return status;
}

export default function MarginPage() {
  const today = new Date().toISOString().slice(0, 10);
  const [start, setStart] = useState(today);
  const [end, setEnd] = useState(today);
  const [exchange, setExchange] = useState("SSE");
  const [report, setReport] = useState<GapReport | null>(null);
  const [loading, setLoading] = useState(false);

  async function fetchGaps() {
    setLoading(true);
    try {
      const res = await api.get("api/v1/admin/collection/gaps", {
        searchParams: { start, end, exchange },
      }).json<{ code: number; data: GapReport }>();
      setReport(res.data || null);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">Margin Data Completeness</h1>

      <div className="flex flex-wrap items-end gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700">Start Date</label>
          <input type="date" value={start} onChange={(e) => setStart(e.target.value)} className="mt-1 rounded-lg border px-3 py-2 text-sm" />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">End Date</label>
          <input type="date" value={end} onChange={(e) => setEnd(e.target.value)} className="mt-1 rounded-lg border px-3 py-2 text-sm" />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">Exchange</label>
          <select value={exchange} onChange={(e) => setExchange(e.target.value)} className="mt-1 rounded-lg border px-3 py-2 text-sm">
            <option value="SSE">SSE</option>
            <option value="SZSE">SZSE</option>
          </select>
        </div>
        <button onClick={fetchGaps} disabled={loading} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50">
          {loading ? "Checking..." : "Check Gaps"}
        </button>
      </div>

      {report && (
        <div className="space-y-4">
          <div className="flex gap-4">
            <div className="rounded-lg border bg-green-50 px-4 py-2 text-sm">Complete: {report.completeWeeks}</div>
            <div className="rounded-lg border bg-yellow-50 px-4 py-2 text-sm">Partial: {report.partialWeeks}</div>
            <div className="rounded-lg border bg-red-50 px-4 py-2 text-sm">Missing: {report.missingWeeks}</div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full border text-sm">
              <thead>
                <tr className="bg-gray-50">
                  <th className="border px-3 py-2 text-left">Week</th>
                  <th className="border px-3 py-2 text-left">Exchange</th>
                  <th className="border px-3 py-2 text-right">Expected</th>
                  <th className="border px-3 py-2 text-right">Collected</th>
                  <th className="border px-3 py-2 text-left">Missing Dates</th>
                  <th className="border px-3 py-2 text-center">Status</th>
                  <th className="border px-3 py-2 text-center">Action</th>
                </tr>
              </thead>
              <tbody>
                {report.weeks.map((week, i) => (
                  <tr key={i} className="hover:bg-gray-50">
                    <td className="border px-3 py-2">{week.weekStart} ~ {week.weekEnd}</td>
                    <td className="border px-3 py-2">{exchange}</td>
                    <td className="border px-3 py-2 text-right">{week.expectedDays}</td>
                    <td className="border px-3 py-2 text-right">{week.collectedDays}</td>
                    <td className="border px-3 py-2 text-xs text-red-600">{week.missingDates.length > 0 ? week.missingDates.join(", ") : "-"}</td>
                    <td className="border px-3 py-2 text-center">{statusEmoji(week.status)}</td>
                    <td className="border px-3 py-2 text-center">
                      {week.missingDates.length > 0 && (
                        <button onClick={() => alert("Coming soon")} className="text-xs text-primary hover:underline">Re-collect</button>
                      )}
                    </td>
                  </tr>
                ))}
                {report.weeks.length === 0 && (
                  <tr><td colSpan={7} className="border px-3 py-4 text-center text-gray-400">No trading days in range</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
