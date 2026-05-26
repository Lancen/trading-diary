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

type TabType = "daily" | "fixed";

const DAILY_TYPES = [
  "TRADE_CALENDAR",
  "STOCK_INFO",
  "MARGIN_DAILY_SSE",
  "MARGIN_DAILY_SZSE",
  "MARGIN_MACRO_SSE",
  "MARGIN_MACRO_SZSE",
  "MARKET_INDEX_DAILY",
  "INDUSTRY_INDEX_DAILY",
  "CONCEPT_INDEX_DAILY",
];

const FIXED_TYPES = ["INDUSTRY_NAME", "CONCEPT_NAME"];

const FLOW_STEPS = [
  {
    step: 1,
    dataTypes: ["TRADE_CALENDAR"],
    label: "交易日历",
    desc: "获取交易日信息，为后续采集提供日期基准",
  },
  {
    step: 2,
    dataTypes: ["STOCK_INFO"],
    label: "股票行情",
    desc: "采集全市场股票行情，含日线数据（STOCK_DAILY联动）",
  },
  {
    step: 3,
    dataTypes: ["MARGIN_DAILY_SSE", "MARGIN_DAILY_SZSE"],
    label: "两融明细",
    desc: "沪深两市融资融券明细数据",
  },
  {
    step: 4,
    dataTypes: ["MARGIN_MACRO_SSE", "MARGIN_MACRO_SZSE"],
    label: "两融总量",
    desc: "沪深两市融资融券总量数据",
  },
  {
    step: 5,
    dataTypes: ["MARKET_INDEX_DAILY"],
    label: "宽基指数",
    desc: "上证、深证、创业板等宽基指数日线",
  },
  {
    step: 6,
    dataTypes: ["INDUSTRY_INDEX_DAILY", "CONCEPT_INDEX_DAILY"],
    label: "板块指数",
    desc: "行业/概念板块指数日线（依赖板块分类数据）",
  },
];

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

function FlowStepNode({
  step,
  label,
  desc,
  dataTypes,
  statusList,
  isLast,
  onClick,
}: {
  step: typeof FLOW_STEPS[number];
  label: string;
  desc: string;
  dataTypes: string[];
  statusList: CollectionStatus[];
  isLast: boolean;
  onClick: (dataType: string) => void;
}) {
  const hasFailed = dataTypes.some((dt) => {
    const s = statusList.find((x) => x.dataType === dt);
    return s?.lastFetch?.status === "FAILED" || s?.lastCleanse?.status === "FAILED";
  });
  const hasRunning = dataTypes.some((dt) => {
    const s = statusList.find((x) => x.dataType === dt);
    return s?.lastFetch?.status === "RUNNING" || s?.lastCleanse?.status === "RUNNING";
  });
  const allSuccess = dataTypes.every((dt) => {
    const s = statusList.find((x) => x.dataType === dt);
    return s && (!s.lastFetch || s.lastFetch.status === "SUCCESS") && (!s.lastCleanse || s.lastCleanse.status === "SUCCESS");
  });

  let borderColor = "border-gray-200";
  let bgColor = "bg-white";
  let stepBg = "bg-gray-100 text-gray-500";
  if (hasFailed) { borderColor = "border-red-300"; bgColor = "bg-red-50"; stepBg = "bg-red-500 text-white"; }
  else if (hasRunning) { borderColor = "border-blue-300"; bgColor = "bg-blue-50"; stepBg = "bg-blue-500 text-white"; }
  else if (allSuccess) { borderColor = "border-green-300"; bgColor = "bg-green-50"; stepBg = "bg-green-500 text-white"; }

  return (
    <div className="flex items-stretch">
      <div className="flex flex-col items-center">
        <div className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-bold ${stepBg}`}>
          {step.step}
        </div>
        {!isLast && <div className="w-0.5 flex-1 bg-gray-200 my-1" />}
      </div>
      <div className={`ml-3 mb-4 flex-1 rounded-lg border p-3 ${borderColor} ${bgColor}`}>
        <div className="flex items-center justify-between mb-1">
          <span className="font-semibold text-sm">{label}</span>
          <div className="flex gap-1">
            {dataTypes.map((dt) => {
              const s = statusList.find((x) => x.dataType === dt);
              return <StatusBadge key={dt} status={s?.lastFetch?.status ?? null} />;
            })}
          </div>
        </div>
        <p className="text-xs text-gray-500 mb-2">{desc}</p>
        <div className="flex gap-2 flex-wrap">
          {dataTypes.map((dt) => {
            const s = statusList.find((x) => x.dataType === dt);
            return (
              <button
                key={dt}
                onClick={() => onClick(dt)}
                className="rounded border border-blue-200 bg-blue-50 px-2 py-0.5 text-xs text-blue-700 hover:bg-blue-100 transition-colors"
              >
                {s?.dataTypeLabel || dt}
                <span className="ml-1 text-blue-400">→</span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export default function CollectionHubPage() {
  const [statusList, setStatusList] = useState<CollectionStatus[]>([]);
  const [loading, setLoading] = useState(true);
  const [constituentFiles, setConstituentFiles] = useState<ConstituentFile[]>([]);
  const [cookieStatus, setCookieStatus] = useState<CookieStatus>({ hasCookie: false, cookiePreview: "", updatedAt: null });
  const [cookieInput, setCookieInput] = useState("");
  const [savingCookie, setSavingCookie] = useState(false);
  const [activeTab, setActiveTab] = useState<TabType>("daily");
  const [triggeringDaily, setTriggeringDaily] = useState(false);
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

  async function handleTriggerDaily() {
    setTriggeringDaily(true);
    try {
      const res = await api.post("api/v1/admin/collection/trigger-daily").json<{ code: number; data: string }>();
      if (res.code === 200) {
        setTimeout(() => fetchStatus(), 3000);
      }
    } catch (e) { console.error(e); }
    finally { setTriggeringDaily(false); }
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
    MARKET_INDEX_DAILY: "/admin/collection/MARKET_INDEX_DAILY",
    INDUSTRY_INDEX_DAILY: "/admin/collection/INDUSTRY_INDEX_DAILY",
    CONCEPT_INDEX_DAILY: "/admin/collection/CONCEPT_INDEX_DAILY",
  };

  const handleCardClick = (dataType: string) => {
    const target = routeMap[dataType];
    if (target) router.push(target);
  };

  const constituentCount = constituentFiles.length;
  const latestConstituent = constituentFiles[0]?.fetchedDate || "-";

  const dailyStatusList = statusList.filter(s => DAILY_TYPES.includes(s.dataType));
  const fixedStatusList = statusList.filter(s => FIXED_TYPES.includes(s.dataType));

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

      {/* Tab切换 */}
      <div className="flex rounded-lg border overflow-hidden">
        <button
          onClick={() => setActiveTab("daily")}
          className={`px-6 py-2.5 text-sm font-medium transition-colors ${
            activeTab === "daily"
              ? "bg-blue-600 text-white"
              : "bg-white text-gray-600 hover:bg-gray-100"
          }`}
        >
          每日采集
        </button>
        <button
          onClick={() => setActiveTab("fixed")}
          className={`px-6 py-2.5 text-sm font-medium transition-colors ${
            activeTab === "fixed"
              ? "bg-blue-600 text-white"
              : "bg-white text-gray-600 hover:bg-gray-100"
          }`}
        >
          固定采集
        </button>
      </div>

      {loading ? (
        <div className="py-8 text-center text-gray-500">加载中...</div>
      ) : activeTab === "daily" ? (
        <DailyCollectionTab
          statusList={dailyStatusList}
          triggeringDaily={triggeringDaily}
          onTriggerDaily={handleTriggerDaily}
          onCardClick={handleCardClick}
        />
      ) : (
        <FixedCollectionTab
          statusList={fixedStatusList}
          constituentCount={constituentCount}
          latestConstituent={latestConstituent}
          onCardClick={handleCardClick}
          onConstituentsClick={() => router.push("/admin/collection/constituents")}
        />
      )}
    </div>
  );
}

function DailyCollectionTab({
  statusList,
  triggeringDaily,
  onTriggerDaily,
  onCardClick,
}: {
  statusList: CollectionStatus[];
  triggeringDaily: boolean;
  onTriggerDaily: () => void;
  onCardClick: (dataType: string) => void;
}) {
  return (
    <div className="space-y-6">
      {/* 一键采集 */}
      <div className="flex items-center gap-4">
        <button
          onClick={onTriggerDaily}
          disabled={triggeringDaily}
          className="rounded-lg bg-gradient-to-r from-blue-600 to-indigo-600 px-6 py-3 text-sm font-bold text-white shadow-md hover:from-blue-700 hover:to-indigo-700 disabled:opacity-50 transition-all"
        >
          {triggeringDaily ? (
            <span className="inline-flex items-center gap-2">
              <span className="animate-spin inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full" />
              采集中...
            </span>
          ) : (
            "⚡ 一键采集"
          )}
        </button>
        <span className="text-xs text-gray-400">
          按依赖顺序依次执行全部每日采集任务（共 {DAILY_TYPES.length} 个）
        </span>
      </div>

      {/* 流程图 */}
      <div className="rounded-lg border bg-white p-5">
        <h2 className="font-semibold mb-4 text-sm text-gray-700">采集流程</h2>
        <div>
          {FLOW_STEPS.map((step, idx) => (
            <FlowStepNode
              key={step.step}
              step={step}
              label={step.label}
              desc={step.desc}
              dataTypes={step.dataTypes}
              statusList={statusList}
              isLast={idx === FLOW_STEPS.length - 1}
              onClick={onCardClick}
            />
          ))}
        </div>
      </div>

      {/* 详细状态卡片 */}
      <div>
        <h2 className="font-semibold mb-3 text-sm text-gray-700">采集状态明细</h2>
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-3">
          {statusList.map((item) => {
            const isFailed = item.lastFetch?.status === "FAILED" || item.lastCleanse?.status === "FAILED";
            const isRunning = item.lastFetch?.status === "RUNNING" || item.lastCleanse?.status === "RUNNING";
            return (
              <div
                key={item.dataType}
                onClick={() => onCardClick(item.dataType)}
                className={"rounded-lg border p-3 transition-shadow hover:shadow-md cursor-pointer " +
                  (isFailed ? "border-red-300 bg-red-50" : isRunning ? "border-blue-300 bg-blue-50" : "bg-white border-gray-200")
                }
              >
                <div className="flex items-center justify-between">
                  <h3 className="font-medium text-sm">{item.dataTypeLabel}</h3>
                  <StatusBadge status={item.lastFetch?.status ?? null} />
                </div>
                <div className="mt-1.5 space-y-0.5">
                  <div className="text-xs text-gray-500">
                    采集: {formatTime(item.lastFetch?.completedAt ?? null)}
                  </div>
                  <div className="text-xs text-gray-500">
                    数据: {formatTime(item.lastDataDate)}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

function FixedCollectionTab({
  statusList,
  constituentCount,
  latestConstituent,
  onCardClick,
  onConstituentsClick,
}: {
  statusList: CollectionStatus[];
  constituentCount: number;
  latestConstituent: string;
  onCardClick: (dataType: string) => void;
  onConstituentsClick: () => void;
}) {
  return (
    <div className="space-y-4">
      <div className="rounded-lg border bg-amber-50 p-4">
        <p className="text-sm text-amber-800">
          固定采集数据无需每日更新，通常在板块分类发生变化时手动触发即可。
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {statusList.map((item) => {
          const isFailed = item.lastFetch?.status === "FAILED";
          return (
            <div
              key={item.dataType}
              onClick={() => onCardClick(item.dataType)}
              className={"rounded-lg border p-4 transition-shadow hover:shadow-md cursor-pointer " +
                (isFailed ? "border-red-300 bg-red-50" : "bg-white border-gray-200")
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
            </div>
          );
        })}

        <div
          onClick={onConstituentsClick}
          className="cursor-pointer rounded-lg border border-gray-200 bg-white p-4 transition-shadow hover:shadow-md"
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
    </div>
  );
}
