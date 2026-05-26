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
  lastDataDate: string | null;
}

interface ConstituentFile {
  filename: string;
  fetchedDate: string | null;
  industryCount: number;
  conceptCount: number;
  totalRelations: number;
  imported: boolean;
}

interface CookieStatus {
  hasCookie: boolean;
  cookiePreview: string;
  updatedAt: string | null;
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
  const [constituentFiles, setConstituentFiles] = useState<ConstituentFile[]>([]);
  const [cookieStatus, setCookieStatus] = useState<CookieStatus>({ hasCookie: false, cookiePreview: "", updatedAt: null });
  const [cookieInput, setCookieInput] = useState("");
  const [savingCookie, setSavingCookie] = useState(false);
  const router = useRouter();

  useEffect(() => { fetchStatus(); fetchConstituentFiles(); fetchCookieStatus(); }, []);

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
      const res = await api.get("api/v1/admin/collection/constituents/files").json<{ code: number; data: ConstituentFile[] }>();
      setConstituentFiles(res.data || []);
    } catch (e) { console.error(e); }
  }

  async function fetchCookieStatus() {
    try {
      const res = await api.get("api/v1/admin/collection/config/cookie").json<{ code: number; data: CookieStatus }>();
      setCookieStatus(res.data);
    } catch (e) { console.error(e); }
  }

  async function saveCookie() {
    setSavingCookie(true);
    try {
      await api.post("api/v1/admin/collection/config/cookie", { json: { cookie: cookieInput } });
      setCookieInput("");
      await fetchCookieStatus();
    } catch (e) { console.error(e); }
    finally { setSavingCookie(false); }
  }

  async function clearCookie() {
    setSavingCookie(true);
    try {
      await api.post("api/v1/admin/collection/config/cookie", { json: { cookie: "" } });
      await fetchCookieStatus();
    } catch (e) { console.error(e); }
    finally { setSavingCookie(false); }
  }

  const normalCount = statusList.filter(s =>
    (!s.lastFetch || s.lastFetch.status === "SUCCESS") &&
    (!s.lastCleanse || s.lastCleanse.status === "SUCCESS")
  ).length;
  const errorCount = statusList.filter(s =>
    s.lastFetch?.status === "FAILED" || s.lastCleanse?.status === "FAILED"
  ).length;

  const routeMap: Record<string, string> = {
    STOCK_INFO: "/admin/collection/STOCK_INFO",
    TRADE_CALENDAR: "/admin/collection/TRADE_CALENDAR",
    INDUSTRY_NAME: "/admin/collection/INDUSTRY_NAME",
    CONCEPT_NAME: "/admin/collection/CONCEPT_NAME",
    MARGIN_DAILY_SSE: "/admin/collection/MARGIN_DAILY_SSE",
    MARGIN_DAILY_SZSE: "/admin/collection/MARGIN_DAILY_SZSE",
    MARGIN_MACRO_SSE: "/admin/collection/MARGIN_MACRO_SSE",
    MARGIN_MACRO_SZSE: "/admin/collection/MARGIN_MACRO_SZSE",
  };

  const handleCardClick = (dataType: string) => {
    const target = routeMap[dataType];
    if (target) router.push(target);
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
          <button onClick={() => { fetchStatus(); fetchConstituentFiles(); fetchCookieStatus(); }} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90">刷新</button>
        </div>
      </div>

      {/* Cookie配置区域 */}
      <div className="rounded-lg border bg-white p-4">
        <div className="flex items-center justify-between mb-3">
          <h2 className="font-semibold">同花顺 Cookie 配置</h2>
          {cookieStatus.hasCookie ? (
            <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-700">已配置</span>
          ) : (
            <span className="rounded-full bg-yellow-100 px-2 py-0.5 text-xs text-yellow-700">未配置</span>
          )}
        </div>
        <div className="text-sm text-gray-500 mb-3">
          {cookieStatus.hasCookie ? (
            <span>当前: {cookieStatus.cookiePreview} · 更新时间: {formatTime(cookieStatus.updatedAt)}</span>
          ) : (
            <span>未配置Cookie时，成分股抓取只能获取部分数据（约前5页）</span>
          )}
        </div>
        <div className="flex gap-2">
          <textarea
            value={cookieInput}
            onChange={(e) => setCookieInput(e.target.value)}
            placeholder="粘贴完整的Cookie字符串..."
            className="flex-1 rounded-lg border px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
            rows={2}
          />
        </div>
        <div className="flex gap-2 mt-2">
          <button
            onClick={saveCookie}
            disabled={savingCookie || !cookieInput.trim()}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {savingCookie ? "保存中..." : "保存"}
          </button>
          {cookieStatus.hasCookie && (
            <button
              onClick={clearCookie}
              disabled={savingCookie}
              className="rounded-lg bg-gray-200 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-300"
            >
              清除
            </button>
          )}
        </div>
        <p className="mt-2 text-xs text-gray-400">
          获取方法：登录同花顺 → F12 → Application → Cookies → q.10jqka.com.cn → 复制全部Cookie
        </p>
      </div>

      {loading ? (
        <div className="py-8 text-center text-gray-500">加载中...</div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {statusList.map((item) => {
            const isFailed = item.lastFetch?.status === "FAILED";
            const target = routeMap[item.dataType];
            return (
              <div
                key={item.dataType}
                onClick={() => handleCardClick(item.dataType)}
                className={"rounded-lg border p-4 transition-shadow hover:shadow-md " +
                  (isFailed ? "border-red-300 bg-red-50" : "bg-white") +
                  (target ? " cursor-pointer border-blue-300" : "")
                }
              >
                <div className="flex items-center justify-between">
                  <h3 className="font-semibold">{item.dataTypeLabel}</h3>
                  <StatusBadge status={item.lastFetch?.status ?? null} />
                </div>
                <div className="mt-2 space-y-1 text-sm">
                  <div className="text-xs text-gray-500">
                    最新采集: {formatTime(item.lastFetch?.completedAt ?? null)}
                  </div>
                  <div className="text-xs text-gray-500">
                    数据时间: {formatTime(item.lastDataDate)}
                  </div>
                </div>
                {target && (
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
