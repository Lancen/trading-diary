"use client";

import { useEffect, useState } from "react";
import api from "@/lib/api";
import { useToast } from "@/hooks/use-toast";

interface JobStatus {
  status: string;
  startedAt: string | null;
  completedAt: string | null;
  recordCount: number | null;
  errorMsg: string | null;
}

interface CollectionStatus {
  dataType: string;
  dataTypeLabel: string;
  lastFetch: JobStatus | null;
  lastCleanse: JobStatus | null;
}

interface CollectionLog {
  id: number;
  dataType: string;
  jobType: string;
  status: string;
  recordCount: number | null;
  errorMsg: string | null;
  startedAt: string | null;
  completedAt: string | null;
}

function StatusIcon({ status }: { status: string | null }) {
  if (!status) return <span className="text-gray-400">未触发</span>;
  if (status === "SUCCESS") return <span className="text-green-600">成功</span>;
  if (status === "FAILED") return <span className="text-red-600">失败</span>;
  if (status === "RUNNING") return <span className="text-blue-600">运行中</span>;
  return <span className="text-gray-500">{status}</span>;
}

function formatTime(iso: string | null): string {
  if (!iso) return "-";
  const d = new Date(iso);
  return d.toLocaleString("zh-CN", {
    month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", second: "2-digit",
  });
}

export default function CollectionPage() {
  const [statusList, setStatusList] = useState<CollectionStatus[]>([]);
  const [expandedType, setExpandedType] = useState<string | null>(null);
  const [logs, setLogs] = useState<CollectionLog[]>([]);
  const [loading, setLoading] = useState(true);
  const [logsLoading, setLogsLoading] = useState(false);
  const [triggering, setTriggering] = useState<Set<string>>(new Set());
  const [constituentFiles, setConstituentFiles] = useState<any[]>([]);
  const [filesLoading, setFilesLoading] = useState(false);
  const [importingFile, setImportingFile] = useState<string | null>(null);
  const toast = useToast((s) => s.toast);

  useEffect(() => { fetchStatus(); fetchConstituentFiles(); }, []);

  async function fetchConstituentFiles() {
    setFilesLoading(true);
    try {
      const res = await api.get("api/v1/admin/collection/constituents/files").json<{ code: number; data: any[] }>();
      setConstituentFiles(res.data || []);
    } catch (e) { console.error(e); }
    finally { setFilesLoading(false); }
  }

  async function handleImportConstituents(filename: string) {
    setImportingFile(filename);
    try {
      const res = await api.post("api/v1/admin/collection/constituents/import", {
        json: { filename },
      }).json<{ code: number; data?: any; msg?: string }>();
      if (res.code === 200) {
        toast(`成分股导入成功: ${res.data?.industryRelations || 0} 行业 + ${res.data?.conceptRelations || 0} 概念`, "success");
      } else {
        toast(`导入失败: ${res.msg || "未知错误"}`, "error");
      }
    } catch (e) {
      toast("导入请求失败", "error");
    } finally {
      setImportingFile(null);
    }
  }

  async function fetchStatus() {
    setLoading(true);
    try {
      const res = await api.get("api/v1/admin/collection/status").json<{ code: number; data: CollectionStatus[] }>();
      setStatusList(res.data || []);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }

  async function fetchLogs(dataType: string) {
    setLogsLoading(true);
    try {
      const res = await api.get("api/v1/admin/collection/logs", { searchParams: { dataType, limit: 5 } }).json<{ code: number; data: CollectionLog[] }>();
      setLogs(res.data || []);
    } catch (e) { console.error(e); }
    finally { setLogsLoading(false); }
  }

  function toggleExpand(dataType: string) {
    if (expandedType === dataType) { setExpandedType(null); setLogs([]); }
    else { setExpandedType(dataType); fetchLogs(dataType); }
  }

  async function handleTrigger(dataType: string) {
    setTriggering((prev) => new Set(prev).add(dataType));
    try {
      const res = await api.post(`api/v1/admin/collection/trigger/${dataType}`).json<{ code: number; msg?: string }>();
      if (res.code === 200) {
        toast(`采集已触发: ${dataType}`, "success");
        fetchStatus();
      } else {
        toast(`触发失败: ${res.msg || "未知错误"}`, "error");
      }
    } catch (e) {
      toast(`触发请求失败: ${dataType}`, "error");
    } finally {
      setTriggering((prev) => {
        const next = new Set(prev);
        next.delete(dataType);
        return next;
      });
    }
  }

  const runningCount = statusList.filter(s => s.lastFetch?.status === "RUNNING" || s.lastCleanse?.status === "RUNNING").length;
  const successCount = statusList.filter(s => s.lastFetch?.status === "SUCCESS" && (!s.lastCleanse || s.lastCleanse.status === "SUCCESS")).length;
  const failedCount = statusList.filter(s => s.lastFetch?.status === "FAILED" || s.lastCleanse?.status === "FAILED").length;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">采集状态</h1>
        <button onClick={fetchStatus} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90">刷新</button>
      </div>
      <div className="flex gap-4">
        <div className="rounded-lg border bg-green-50 px-4 py-3"><div className="text-sm text-gray-600">成功</div><div className="text-2xl font-bold text-green-700">{successCount}</div></div>
        <div className="rounded-lg border bg-red-50 px-4 py-3"><div className="text-sm text-gray-600">失败</div><div className="text-2xl font-bold text-red-700">{failedCount}</div></div>
        <div className="rounded-lg border bg-blue-50 px-4 py-3"><div className="text-sm text-gray-600">运行中</div><div className="text-2xl font-bold text-blue-700">{runningCount}</div></div>
      </div>
      {loading ? <div className="py-8 text-center text-gray-500">加载中...</div> : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {statusList.map((item) => {
            const isExpanded = expandedType === item.dataType;
            const hasError = item.lastFetch?.errorMsg || item.lastCleanse?.errorMsg;
            const isTriggering = triggering.has(item.dataType);
            return (
              <div key={item.dataType}>
                <div onClick={() => toggleExpand(item.dataType)} className={"cursor-pointer rounded-lg border p-4 transition-shadow hover:shadow-md " + (hasError ? "border-red-300 bg-red-50" : "bg-white")}>
                  <h3 className="font-semibold">{item.dataTypeLabel}</h3>
                  <div className="mt-2 space-y-1 text-sm">
                    <div><span className="text-gray-500">采集: </span><StatusIcon status={item.lastFetch?.status || null} /></div>
                    <div><span className="text-gray-500">清洗: </span><StatusIcon status={item.lastCleanse?.status || null} /></div>
                    {item.lastFetch && <div className="text-xs text-gray-400">最近: {formatTime(item.lastFetch.completedAt)} | {item.lastFetch.recordCount ?? 0} 条</div>}
                  </div>
                  {hasError && <div className="mt-2 text-xs text-red-600">{item.lastFetch?.errorMsg || item.lastCleanse?.errorMsg}</div>}
                  <div className="mt-2 flex gap-2">
                    <button
                      onClick={(e) => { e.stopPropagation(); handleTrigger(item.dataType); }}
                      disabled={isTriggering}
                      className="rounded bg-primary px-3 py-1 text-xs font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50"
                    >
                      {isTriggering ? "采集中..." : "重新采集"}
                    </button>
                  </div>
                </div>
                {isExpanded && (
                  <div className="mt-2 rounded-lg border bg-gray-50 p-3">
                    <h4 className="mb-2 text-sm font-medium">最近 5 条日志</h4>
                    {logsLoading ? <div className="text-xs text-gray-400">加载中...</div> : logs.length === 0 ? <div className="text-xs text-gray-400">无日志</div> : (
                      <div className="space-y-2">
                        {logs.map((log) => (
                          <div key={log.id} className="rounded bg-white px-3 py-2 text-xs">
                            <div className="flex items-center gap-2">
                              <span className={"rounded px-1 font-medium " + (log.jobType === "FETCH" ? "bg-blue-100 text-blue-700" : "bg-purple-100 text-purple-700")}>{log.jobType}</span>
                              <StatusIcon status={log.status} />
                              <span className="text-gray-400">{formatTime(log.completedAt)}</span>
                              {log.recordCount != null && <span className="text-gray-500">{log.recordCount}条</span>}
                            </div>
                            {log.errorMsg && <div className="mt-1 text-red-600">{log.errorMsg}</div>}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* 成分股数据文件 */}
      <div className="rounded-lg border bg-white p-4">
        <div className="flex items-center justify-between mb-3">
          <h2 className="font-semibold">成分股数据</h2>
          <button onClick={fetchConstituentFiles} disabled={filesLoading} className="text-sm text-primary hover:underline">
            {filesLoading ? "加载中..." : "刷新"}
          </button>
        </div>
        <p className="text-xs text-gray-500 mb-2">数据来源: Playwright 抓取同花顺，存放在 data/constituents/ 目录。选择文件导入到数据库。</p>
        {constituentFiles.length === 0 ? (
          <div className="text-sm text-gray-400 py-4 text-center">暂无成分股数据文件</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-gray-500">
                  <th className="py-2 pr-4 font-medium">文件名</th>
                  <th className="py-2 pr-4 font-medium">采集日期</th>
                  <th className="py-2 pr-4 font-medium">行业</th>
                  <th className="py-2 pr-4 font-medium">概念</th>
                  <th className="py-2 pr-4 font-medium">关系总数</th>
                  <th className="py-2 font-medium">操作</th>
                </tr>
              </thead>
              <tbody>
                {constituentFiles.map((f: any) => (
                  <tr key={f.filename} className="border-b last:border-0">
                    <td className="py-2 pr-4 font-mono text-xs">{f.filename}</td>
                    <td className="py-2 pr-4">{f.fetchedDate || "-"}</td>
                    <td className="py-2 pr-4">{f.industryCount ?? "-"}</td>
                    <td className="py-2 pr-4">{f.conceptCount ?? "-"}</td>
                    <td className="py-2 pr-4">{f.totalRelations ?? "-"}</td>
                    <td className="py-2">
                      <button
                        onClick={() => handleImportConstituents(f.filename)}
                        disabled={importingFile === f.filename}
                        className="rounded bg-primary px-3 py-1 text-xs font-medium text-primary-foreground hover:opacity-90 disabled:opacity-50"
                      >
                        {importingFile === f.filename ? "导入中..." : "导入"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
