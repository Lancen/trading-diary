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
  if (status === "COMPLETE") return "完整";
  if (status === "PARTIAL") return "部分缺失";
  if (status === "MISSING") return "未采集";
  return status;
}

const DATA_TYPE_OPTIONS: Record<string, string> = {
  MARGIN_DAILY_SSE: "两融明细(沪市)",
  MARGIN_DAILY_SZSE: "两融明细(深市)",
  MARGIN_MACRO_SSE: "两融总量(沪市)",
  MARGIN_MACRO_SZSE: "两融总量(深市)",
};

const DATA_TYPE_TO_EXCHANGE: Record<string, string> = {
  MARGIN_DAILY_SSE: "SSE",
  MARGIN_DAILY_SZSE: "SZSE",
  MARGIN_MACRO_SSE: "SSE",
  MARGIN_MACRO_SZSE: "SZSE",
};

interface BackfillForm {
  dataType: string;
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
  const [startDate, setStartDate] = useState(initialValues?.startDate || today);
  const [endDate, setEndDate] = useState(initialValues?.endDate || today);
  const [submitting, setSubmitting] = useState(false);

  // initialValues 变化时重置表单
  const [lastInit, setLastInit] = useState<string>("");
  const initKey = JSON.stringify(initialValues);
  if (initKey !== lastInit) {
    setLastInit(initKey);
    setDataType(initialValues?.dataType || "MARGIN_DAILY_SSE");
    setStartDate(initialValues?.startDate || today);
    setEndDate(initialValues?.endDate || today);
  }

  if (!open) return null;

  async function handleSubmit() {
    setSubmitting(true);
    const exchange = DATA_TYPE_TO_EXCHANGE[dataType] || "SSE";
    try {
      const res = await api.post("api/v1/admin/collection/backfill", {
        json: { dataType, exchange, startDate, endDate },
      }).json<{ code: number; msg?: string }>();
      if (res.code === 200) {
        toast(`补采已提交: ${DATA_TYPE_OPTIONS[dataType]} ${startDate}~${endDate}`, "success");
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
              {Object.entries(DATA_TYPE_OPTIONS).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
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
  const [dataType, setDataType] = useState("MARGIN_DAILY_SSE");
  const [report, setReport] = useState<GapReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogValues, setDialogValues] = useState<BackfillForm | null>(null);

  async function fetchGaps() {
    setLoading(true);
    try {
      const res = await api.get("api/v1/admin/collection/gaps", {
        searchParams: { start, end, dataType },
      }).json<{ code: number; data: GapReport }>();
      setReport(res.data || null);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }

  function openBackfillDialog(prefill?: Partial<BackfillForm>) {
    setDialogValues({
      dataType: prefill?.dataType || dataType,
      startDate: prefill?.startDate || start,
      endDate: prefill?.endDate || end,
    });
    setDialogOpen(true);
  }

  function handleDialogClose() {
    setDialogOpen(false);
    // 补采对话框关闭后刷新缺口数据，查看更新状态
    fetchGaps();
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">两融数据完整性</h1>
        <button
          onClick={() => openBackfillDialog()}
          className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90"
        >
          补采
        </button>
      </div>

      <div className="flex flex-wrap items-end gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700">开始日期</label>
          <input type="date" value={start} onChange={(e) => setStart(e.target.value)} className="mt-1 rounded-lg border px-3 py-2 text-sm" />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">结束日期</label>
          <input type="date" value={end} onChange={(e) => setEnd(e.target.value)} className="mt-1 rounded-lg border px-3 py-2 text-sm" />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">数据类型</label>
          <select value={dataType} onChange={(e) => setDataType(e.target.value)} className="mt-1 rounded-lg border px-3 py-2 text-sm">
            {Object.entries(DATA_TYPE_OPTIONS).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </div>
        <button onClick={fetchGaps} disabled={loading} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50">
          {loading ? "检查中..." : "检测缺口"}
        </button>
      </div>

      {report && (
        <div className="space-y-4">
          <div className="flex gap-4">
            <div className="rounded-lg border bg-green-50 px-4 py-2 text-sm">完整: {report.completeWeeks}</div>
            <div className="rounded-lg border bg-yellow-50 px-4 py-2 text-sm">部分缺失: {report.partialWeeks}</div>
            <div className="rounded-lg border bg-red-50 px-4 py-2 text-sm">未采集: {report.missingWeeks}</div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full border text-sm">
              <thead>
                <tr className="bg-gray-50">
                  <th className="border px-3 py-2 text-left">周</th>
                  <th className="border px-3 py-2 text-left">交易所</th>
                  <th className="border px-3 py-2 text-right">应采</th>
                  <th className="border px-3 py-2 text-right">已采</th>
                  <th className="border px-3 py-2 text-left">缺失日期</th>
                  <th className="border px-3 py-2 text-center">状态</th>
                  <th className="border px-3 py-2 text-center">操作</th>
                </tr>
              </thead>
              <tbody>
                {report.weeks.map((week, i) => {
                  const needsBackfill = week.status === "PARTIAL" || week.status === "MISSING";
                  return (
                    <tr key={i} className="hover:bg-gray-50">
                      <td className="border px-3 py-2">{week.weekStart} ~ {week.weekEnd}</td>
                      <td className="border px-3 py-2">{DATA_TYPE_OPTIONS[dataType]}</td>
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
                  <tr><td colSpan={7} className="border px-3 py-4 text-center text-gray-400">此范围无交易日</td></tr>
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
