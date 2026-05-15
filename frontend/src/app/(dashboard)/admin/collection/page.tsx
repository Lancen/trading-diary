"use client";

import { useEffect, useState } from "react";
import api from "@/lib/api";

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
  if (!status) return <span className="text-gray-400">NOT_TRIGGERED</span>;
  if (status === "SUCCESS") return <span className="text-green-600">SUCCESS</span>;
  if (status === "FAILED") return <span className="text-red-600">FAILED</span>;
  if (status === "RUNNING") return <span className="text-blue-600">RUNNING</span>;
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

  useEffect(() => { fetchStatus(); }, []);

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

  const runningCount = statusList.filter(s => s.lastFetch?.status === "RUNNING" || s.lastCleanse?.status === "RUNNING").length;
  const successCount = statusList.filter(s => s.lastFetch?.status === "SUCCESS" && (!s.lastCleanse || s.lastCleanse.status === "SUCCESS")).length;
  const failedCount = statusList.filter(s => s.lastFetch?.status === "FAILED" || s.lastCleanse?.status === "FAILED").length;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Collection Status</h1>
        <button onClick={fetchStatus} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90">Refresh</button>
      </div>
      <div className="flex gap-4">
        <div className="rounded-lg border bg-green-50 px-4 py-3"><div className="text-sm text-gray-600">Success</div><div className="text-2xl font-bold text-green-700">{successCount}</div></div>
        <div className="rounded-lg border bg-red-50 px-4 py-3"><div className="text-sm text-gray-600">Failed</div><div className="text-2xl font-bold text-red-700">{failedCount}</div></div>
        <div className="rounded-lg border bg-blue-50 px-4 py-3"><div className="text-sm text-gray-600">Running</div><div className="text-2xl font-bold text-blue-700">{runningCount}</div></div>
      </div>
      {loading ? <div className="py-8 text-center text-gray-500">Loading...</div> : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {statusList.map((item) => {
            const isExpanded = expandedType === item.dataType;
            const hasError = item.lastFetch?.errorMsg || item.lastCleanse?.errorMsg;
            return (
              <div key={item.dataType}>
                <div onClick={() => toggleExpand(item.dataType)} className={"cursor-pointer rounded-lg border p-4 transition-shadow hover:shadow-md " + (hasError ? "border-red-300 bg-red-50" : "bg-white")}>
                  <h3 className="font-semibold">{item.dataTypeLabel}</h3>
                  <div className="mt-2 space-y-1 text-sm">
                    <div><span className="text-gray-500">FETCH: </span><StatusIcon status={item.lastFetch?.status || null} /></div>
                    <div><span className="text-gray-500">CLEANSE: </span><StatusIcon status={item.lastCleanse?.status || null} /></div>
                    {item.lastFetch && <div className="text-xs text-gray-400">Last: {formatTime(item.lastFetch.completedAt)} | {item.lastFetch.recordCount ?? 0} records</div>}
                  </div>
                  {hasError && <div className="mt-2 text-xs text-red-600">{item.lastFetch?.errorMsg || item.lastCleanse?.errorMsg}</div>}
                  <div className="mt-2 flex gap-2">
                    <button onClick={(e) => { e.stopPropagation(); alert("Coming soon"); }} className="rounded bg-gray-100 px-3 py-1 text-xs hover:bg-gray-200">Re-collect</button>
                  </div>
                </div>
                {isExpanded && (
                  <div className="mt-2 rounded-lg border bg-gray-50 p-3">
                    <h4 className="mb-2 text-sm font-medium">Recent 5 Logs</h4>
                    {logsLoading ? <div className="text-xs text-gray-400">Loading...</div> : logs.length === 0 ? <div className="text-xs text-gray-400">No logs</div> : (
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
    </div>
  );
}
