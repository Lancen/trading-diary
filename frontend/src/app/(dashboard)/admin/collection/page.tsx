"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import api from "@/lib/api";

interface JobStatus {
  status: string;
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

function StatusBadge({ status }: { status: string | null }) {
  if (!status) return <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-400">未触发</span>;
  if (status === "SUCCESS") return <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-700">正常</span>;
  if (status === "FAILED") return <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs text-red-700">异常</span>;
  if (status === "RUNNING") return <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs text-blue-700">采集中</span>;
  return <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-500">{status}</span>;
}

function formatTime(iso: string | null): string {
  if (!iso) return "-";
  return new Date(iso).toLocaleString("zh-CN", {
    month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit",
  });
}

export default function CollectionHubPage() {
  const [statusList, setStatusList] = useState<CollectionStatus[]>([]);
  const [loading, setLoading] = useState(true);
  const [constituentFiles, setConstituentFiles] = useState<any[]>([]);
  const router = useRouter();

  useEffect(() => { fetchStatus(); fetchConstituentFiles(); }, []);

  async function fetchStatus() {
    setLoading(true);
    try {
      const res = await api.get("api/v1/admin/collection/status").json<{ code: number; data: CollectionStatus[] }>();
      setStatusList(res.data || []);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }

  async function fetchConstituentFiles() {
    try {
      const res = await api.get("api/v1/admin/collection/constituents/files").json<{ code: number; data: any[] }>();
      setConstituentFiles(res.data || []);
    } catch (e) { console.error(e); }
  }

  const normalCount = statusList.filter(s =>
    (!s.lastFetch || s.lastFetch.status === "SUCCESS") &&
    (!s.lastCleanse || s.lastCleanse.status === "SUCCESS")
  ).length;
  const errorCount = statusList.filter(s =>
    s.lastFetch?.status === "FAILED" || s.lastCleanse?.status === "FAILED"
  ).length;

  const handleCardClick = (dataType: string) => {
    if (dataType === "STOCK_INFO") {
      router.push("/admin/collection/stocks");
    }
  };

  const constituentCount = constituentFiles.length;
  const latestConstituent = constituentFiles[0]?.fetchedDate || "-";

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">数据采集</h1>
        <div className="flex items-center gap-3">
          <span className="text-sm text-gray-500">
            正常 <span className="font-bold text-green-700">{normalCount}</span> · 异常 <span className="font-bold text-red-700">{errorCount}</span>
          </span>
          <button onClick={() => { fetchStatus(); fetchConstituentFiles(); }} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90">刷新</button>
        </div>
      </div>

      {loading ? (
        <div className="py-8 text-center text-gray-500">加载中...</div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {statusList.map((item) => {
            const hasError = item.lastFetch?.errorMsg || item.lastCleanse?.errorMsg;
            const isClickable = item.dataType === "STOCK_INFO";
            return (
              <div
                key={item.dataType}
                onClick={() => handleCardClick(item.dataType)}
                className={"rounded-lg border p-4 transition-shadow hover:shadow-md " +
                  (hasError ? "border-red-300 bg-red-50" : "bg-white") +
                  (isClickable ? " cursor-pointer border-blue-300" : "")
                }
              >
                <div className="flex items-center justify-between">
                  <h3 className="font-semibold">{item.dataTypeLabel}</h3>
                  {hasError ? (
                    <StatusBadge status="FAILED" />
                  ) : (
                    <StatusBadge status="SUCCESS" />
                  )}
                </div>
                <div className="mt-2 space-y-1 text-sm">
                  {item.lastFetch && (
                    <div className="flex justify-between text-xs text-gray-500">
                      <span>采集: {formatTime(item.lastFetch.completedAt)}</span>
                      <span>{item.lastFetch.recordCount ?? 0} 条</span>
                    </div>
                  )}
                  {item.lastCleanse && item.lastCleanse.status === "SUCCESS" && (
                    <div className="flex justify-between text-xs text-gray-500">
                      <span>清洗: {formatTime(item.lastCleanse.completedAt)}</span>
                      <span>{item.lastCleanse.recordCount ?? 0} 条</span>
                    </div>
                  )}
                </div>
                {isClickable && (
                  <p className="mt-2 text-xs text-blue-600">→ 查看详情</p>
                )}
              </div>
            );
          })}

          {/* 成分股卡片 */}
          <div
            onClick={() => router.push("/admin/collection/constituents")}
            className="cursor-pointer rounded-lg border border-blue-300 bg-white p-4 transition-shadow hover:shadow-md"
          >
            <div className="flex items-center justify-between">
              <h3 className="font-semibold">成分股数据</h3>
              <StatusBadge status="SUCCESS" />
            </div>
            <div className="mt-2 space-y-1 text-sm">
              <div className="text-xs text-gray-500">{constituentCount} 个文件 · 最近 {latestConstituent}</div>
            </div>
            <p className="mt-2 text-xs text-blue-600">→ 管理</p>
          </div>
        </div>
      )}
    </div>
  );
}
