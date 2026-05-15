"use client";

import { useState } from "react";
import api from "@/lib/api";
import { useToast } from "@/hooks/use-toast";

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

interface BackfillForm {
  dataType: string;
  exchange: string;
  startDate: string;
  endDate: string;
}

function BackfillDialog({
  open,
  onClose,
  initialValues,
}: {
  open: boolean;
  onClose: () => void;
  initialValues: BackfillForm | null;
}) {
  const toast = useToast((s) => s.toast);
  const today = new Date().toISOString().slice(0, 10);
  const [dataType, setDataType] = useState(initialValues?.dataType || "MARGIN_DAILY_SSE");
  const [exchange, setExchange] = useState(initialValues?.exchange || "SSE");
  const [startDate, setStartDate] = useState(initialValues?.startDate || today);
  const [endDate, setEndDate] = useState(initialValues?.endDate || today);
  const [submitting, setSubmitting] = useState(false);

  // Reset form when initialValues change
  const [lastInit, setLastInit] = useState<string>("");
  const initKey = JSON.stringify(initialValues);
  if (initKey !== lastInit) {
    setLastInit(initKey);
    setDataType(initialValues?.dataType || "MARGIN_DAILY_SSE");
    setExchange(initialValues?.exchange || "SSE");
    setStartDate(initialValues?.startDate || today);
    setEndDate(initialValues?.endDate || today);
  }

  if (!open) return null;

  async function handleSubmit() {
    setSubmitting(true);
    try {
      const res = await api.post("api/v1/admin/collection/backfill", {
        json: { dataType, exchange, startDate, endDate },
      }).json<{ code: number; msg?: string }>();
      if (res.code === 200) {
        toast(`补采已提交: ${dataType} ${exchange} ${startDate}~${endDate}`, "success");
        onClose();
      } else {
        toast(`补采失败: ${res.msg || "未知错误"}`, "error");
      }
    } catch (e) {
      toast("补采请求失败", "error");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative z-50 w-full max-w-md rounded-xl bg-white p-6 shadow-2xl">
        <h2 className="mb-4 text-lg font-bold">补采数据</h2>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700">数据类型</label>
            <select
              value={dataType}
              onChange={(e) => setDataType(e.target.value)}
              className="mt-1 w-full rounded-lg border px-3 py-2 text-sm"
            >
              <option value="MARGIN_DAILY_SSE">MARGIN_DAILY_SSE</option>
              <option value="MARGIN_DAILY_SZSE">MARGIN_DAILY_SZSE</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700">交易所</label>
            <select
              value={exchange}
              onChange={(e) => setExchange(e.target.value)}
              className="mt-1 w-full rounded-lg border px-3 py-2 text-sm"
            >
              <option value="SSE">SSE</option>
              <option value="SZSE">SZSE</option>
            </select>
          </div>

          <div className="flex gap-4">
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700">开始日期</label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="mt-1 w-full rounded-lg border px-3 py-2 text-sm"
              />
            </div>
            <div className="flex-1">
              <label className="block text-sm font-medium text-gray-700">结束日期</label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="mt-1 w-full rounded-lg border px-3 py-2 text-sm"
              />
            </div>
          </div>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button
            onClick={onClose}
            disabled={submitting}
            className="rounded-lg border px-4 py-2 text-sm hover:bg-gray-50"
          >
            取消
          </button>
          <button
            onClick={handleSubmit}
            disabled={submitting}
            className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50"
          >
            {submitting ? "提交中..." : "提交补采"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function MarginPage() {
  const today = new Date().toISOString().slice(0, 10);
  const [start, setStart] = useState(today);
  const [end, setEnd] = useState(today);
  const [exchange, setExchange] = useState("SSE");
  const [report, setReport] = useState<GapReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogValues, setDialogValues] = useState<BackfillForm | null>(null);

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

  function openBackfillDialog(prefill?: Partial<BackfillForm>) {
    const dataType = exchange === "SZSE" ? "MARGIN_DAILY_SZSE" : "MARGIN_DAILY_SSE";
    setDialogValues({
      dataType: prefill?.dataType || dataType,
      exchange: prefill?.exchange || exchange,
      startDate: prefill?.startDate || start,
      endDate: prefill?.endDate || end,
    });
    setDialogOpen(true);
  }

  function handleDialogClose() {
    setDialogOpen(false);
    // Refresh gaps after backfill dialog closes so we can see updated status
    fetchGaps();
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Margin Data Completeness</h1>
        <button
          onClick={() => openBackfillDialog()}
          className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90"
        >
          补采
        </button>
      </div>

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
                {report.weeks.map((week, i) => {
                  const needsBackfill = week.status === "PARTIAL" || week.status === "MISSING";
                  return (
                    <tr key={i} className="hover:bg-gray-50">
                      <td className="border px-3 py-2">{week.weekStart} ~ {week.weekEnd}</td>
                      <td className="border px-3 py-2">{exchange}</td>
                      <td className="border px-3 py-2 text-right">{week.expectedDays}</td>
                      <td className="border px-3 py-2 text-right">{week.collectedDays}</td>
                      <td className="border px-3 py-2 text-xs text-red-600">{week.missingDates.length > 0 ? week.missingDates.join(", ") : "-"}</td>
                      <td className="border px-3 py-2 text-center">{statusEmoji(week.status)}</td>
                      <td className="border px-3 py-2 text-center">
                        {needsBackfill && (
                          <button
                            onClick={() =>
                              openBackfillDialog({
                                startDate: week.weekStart,
                                endDate: week.weekEnd,
                              })
                            }
                            className="rounded bg-primary px-3 py-1 text-xs font-medium text-primary-foreground hover:opacity-90"
                          >
                            补采缺失
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
                {report.weeks.length === 0 && (
                  <tr><td colSpan={7} className="border px-3 py-4 text-center text-gray-400">No trading days in range</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <BackfillDialog
        open={dialogOpen}
        onClose={handleDialogClose}
        initialValues={dialogValues}
      />
    </div>
  );
}
