"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import api from "@/lib/api";
import { useToast } from "@/hooks/use-toast";

interface JobStatus {
  status: string | null;
  startedAt: string | null;
  completedAt: string | null;
  recordCount: number | null;
  errorMsg: string | null;
}

interface CollectionLog {
  id: number;
  dataType: string;
  jobType: string;
  status: string;
  recordCount: number | null;
  errorMsg: string | null;
  requestUrl: string | null;
  requestParams: string | null;
  remark: string | null;
  startedAt: string | null;
  completedAt: string | null;
  tradeDate: string | null;
}

function PipelineStep({ label, time, count, done, failed }: {
  label: string; time: string | null; count: string; done: boolean; failed?: boolean;
}) {
  return (
    <div className="text-center flex-1">
      <div className={`mx-auto flex h-10 w-10 items-center justify-center rounded-full text-lg font-bold ${
        failed ? "bg-red-500 text-white" : done ? "bg-green-500 text-white" : "bg-gray-200 text-gray-400"
      }`}>
        {failed ? "✗" : done ? "✓" : "-"}
      </div>
      <p className="mt-1 text-sm font-bold">{label}</p>
      <p className="text-xs text-gray-500">{time}</p>
      <p className="text-xs text-gray-400">{count}</p>
    </div>
  );
}

export default function StocksCollectionPage() {
  const [fetchStatus, setFetchStatus] = useState<JobStatus | null>(null);
  const [cleanseStatus, setCleanseStatus] = useState<JobStatus | null>(null);
  const [logs, setLogs] = useState<CollectionLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [triggering, setTriggering] = useState(false);
  const [bfOpen, setBfOpen] = useState(false);
  const [bfStart, setBfStart] = useState("");
  const [bfEnd, setBfEnd] = useState("");
  const toast = useToast((s) => s.toast);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [statusRes, logsRes] = await Promise.all([
        api.get("api/v1/admin/collection/status").json<{ code: number; data: any[] }>(),
        api.get("api/v1/admin/collection/logs", { searchParams: { dataType: "STOCK_INFO", limit: 20 } }).json<{ code: number; data: CollectionLog[] }>(),
      ]);
      const stockInfo = (statusRes.data || []).find((s: any) => s.dataType === "STOCK_INFO");
      setFetchStatus(stockInfo?.lastFetch || null);
      setCleanseStatus(stockInfo?.lastCleanse || null);
      setLogs(logsRes.data || []);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  async function handleTrigger() {
    setTriggering(true);
    try {
      const res = await api.post("api/v1/admin/collection/trigger/STOCK_INFO").json<{ code: number; msg?: string; data?: string }>();
      if (res.code === 200) {
        toast("采集任务已提交", "success");
        setTimeout(() => fetchData(), 5000);
      } else {
        toast(`触发失败: ${res.msg || ""}`, "error");
      }
    } catch (e) { toast("触发请求失败", "error"); }
    finally { setTriggering(false); }
  }

  async function handleBackfill() {
    if (!bfStart || !bfEnd) { toast("请选择日期范围", "error"); return; }
    try {
      const res = await api.post("api/v1/admin/collection/backfill", {
        json: { dataType: "STOCK_DAILY", startDate: bfStart, endDate: bfEnd },
      }).json<{ code: number; msg?: string }>();
      if (res.code === 200) {
        toast("补采任务已提交", "success");
        setBfOpen(false);
        setTimeout(() => fetchData(), 5000);
      } else {
        toast(`补采失败: ${res.msg || ""}`, "error");
      }
    } catch (e) { toast("补采请求失败", "error"); }
  }

  function formatTime(iso: string | null): string {
    if (!iso) return "-";
    return new Date(iso).toLocaleString("zh-CN", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
  }

  const fetchDone = fetchStatus?.status === "SUCCESS";
  const cleanseDone = cleanseStatus?.status === "SUCCESS";
  const fetchFailed = fetchStatus?.status === "FAILED";
  const cleanseFailed = cleanseStatus?.status === "FAILED";

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2 text-sm text-gray-500">
        <Link href="/admin/collection" className="hover:underline">← 返回数据采集</Link>
        <span className="text-gray-300">|</span>
        <h1 className="text-2xl font-bold text-gray-900">股票行情</h1>
      </div>

      {/* 采集管线状态 */}
      <div className="rounded-lg border border-green-200 bg-green-50 p-6">
        <p className="mb-4 text-sm text-gray-500">采集管线状态</p>
        <div className="flex items-center">
          <PipelineStep label="采集" time={formatTime(fetchStatus?.completedAt ?? null)} count={fetchStatus?.recordCount ? `${fetchStatus.recordCount}条` : "-"} done={fetchDone} failed={fetchFailed} />
          <span className="px-4 text-gray-300 text-xl">→</span>
          <PipelineStep label="清洗" time={formatTime(cleanseStatus?.completedAt ?? null)} count={cleanseStatus?.recordCount ? `${cleanseStatus.recordCount}条` : "-"} done={cleanseDone} failed={cleanseFailed} />
        </div>
        {(fetchFailed || cleanseFailed) && (
          <p className="mt-3 text-sm text-red-600">{fetchStatus?.errorMsg || cleanseStatus?.errorMsg}</p>
        )}
      </div>

      {/* 操作按钮 */}
      <div className="flex gap-3">
        <button onClick={handleTrigger} disabled={triggering}
          className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50">
          {triggering ? "提交中..." : "触发采集"}
        </button>
        <button onClick={() => setBfOpen(true)}
          className="rounded-lg border px-5 py-2 text-sm font-medium hover:bg-gray-50">
          历史补采
        </button>
      </div>

      {/* 采集日志 */}
      <div className="rounded-lg border">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <h3 className="font-semibold">采集日志</h3>
          <span className="text-xs text-gray-500">最近 {logs.length} 条</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-gray-50 text-left text-xs text-gray-500">
                <th className="px-4 py-2">日期</th>
                <th className="px-4 py-2">阶段</th>
                <th className="px-4 py-2">状态</th>
                <th className="px-4 py-2">地址</th>
                <th className="px-4 py-2">参数</th>
                <th className="px-4 py-2">记录数</th>
                <th className="px-4 py-2">备注</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan={7} className="px-4 py-4 text-center text-gray-400">加载中...</td></tr>
              ) : logs.length === 0 ? (
                <tr><td colSpan={7} className="px-4 py-4 text-center text-gray-400">无日志</td></tr>
              ) : logs.map((log) => (
                <tr key={log.id} className={`border-b ${log.status === "FAILED" ? "bg-red-50" : ""}`}>
                  <td className="px-4 py-2 font-mono text-xs">{log.tradeDate || "-"}</td>
                  <td className="px-4 py-2">
                    <span className={`rounded px-1.5 py-0.5 text-xs font-medium ${log.jobType === "FETCH" ? "bg-blue-100 text-blue-700" : "bg-purple-100 text-purple-700"}`}>
                      {log.jobType === "FETCH" ? "采集" : "清洗"}
                    </span>
                  </td>
                  <td className="px-4 py-2">
                    {log.status === "SUCCESS" ? <span className="text-green-600">成功</span> :
                     log.status === "FAILED" ? <span className="text-red-600">失败</span> :
                     <span className="text-gray-500">{log.status}</span>}
                  </td>
                  <td className="px-4 py-2 font-mono text-xs text-gray-500 max-w-[200px] truncate">{log.requestUrl || "-"}</td>
                  <td className="px-4 py-2 font-mono text-xs text-gray-500 max-w-[150px] truncate">{log.requestParams || "-"}</td>
                  <td className="px-4 py-2 text-center">{log.recordCount ?? "-"}</td>
                  <td className="px-4 py-2 text-xs max-w-[200px] truncate">
                    {log.status === "FAILED" ? <span className="text-red-600">{log.errorMsg || log.remark || "-"}</span> :
                     <span className="text-gray-500">{log.remark || "-"}</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* 历史补采弹窗 */}
      {bfOpen && (
        <div className="fixed inset-0 z-40 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/40" onClick={() => setBfOpen(false)} />
          <div className="relative z-50 w-full max-w-md rounded-xl bg-white p-6 shadow-2xl">
            <h2 className="mb-4 text-lg font-bold">历史数据补采</h2>
            <div className="space-y-4">
              <p className="text-sm text-gray-500">日线数据（STOCK_DAILY）</p>
              <div className="flex gap-4">
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700">开始日期</label>
                  <input type="date" value={bfStart} onChange={(e) => setBfStart(e.target.value)} className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
                </div>
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700">结束日期</label>
                  <input type="date" value={bfEnd} onChange={(e) => setBfEnd(e.target.value)} className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
                </div>
              </div>
            </div>
            <div className="mt-6 flex justify-end gap-3">
              <button onClick={() => setBfOpen(false)} className="rounded-lg border px-4 py-2 text-sm hover:bg-gray-50">取消</button>
              <button onClick={handleBackfill} className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:opacity-90">开始补采</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
