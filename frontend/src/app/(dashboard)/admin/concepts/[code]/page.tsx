"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import api from "@/lib/api";
import KlineMarginOverlay, { KlinePoint, MarginPoint } from "@/components/chart/KlineMarginOverlay";

interface SectorIndexDaily {
  tradeDate: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
  amount: number;
  changePct: number;
}

interface SectorMarginDaily {
  tradeDate: string;
  marginBalance: number;
  shortBalance: number;
  totalBalance: number;
}

interface StockItem {
  stockCode: string;
  stockName: string;
}

type ScrapeStatus = "idle" | "scraping" | "success" | "failed";

export default function ConceptDetailPage() {
  const { code } = useParams<{ code: string }>();
  const router = useRouter();
  const [klines, setKlines] = useState<KlinePoint[]>([]);
  const [margins, setMargins] = useState<MarginPoint[]>([]);
  const [stocks, setStocks] = useState<StockItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [scrapeStatus, setScrapeStatus] = useState<ScrapeStatus>("idle");
  const [scrapeMsg, setScrapeMsg] = useState("");
  const [range, setRange] = useState("3m");
  const [period, setPeriod] = useState<"daily" | "weekly" | "monthly">("daily");
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchStocks = useCallback(async () => {
    try {
      const stockRes = await api.get(`api/v1/admin/market/concepts/${code}/stocks`)
        .json<{ code: number; data: StockItem[] }>();
      setStocks(stockRes.data || []);
    } catch {
      setStocks([]);
    }
  }, [code]);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const now = new Date();
      let startDate = "";
      if (range === "1m") { const d = new Date(now); d.setMonth(d.getMonth() - 1); startDate = d.toISOString().slice(0, 10); }
      else if (range === "3m") { const d = new Date(now); d.setMonth(d.getMonth() - 3); startDate = d.toISOString().slice(0, 10); }
      else if (range === "6m") { const d = new Date(now); d.setMonth(d.getMonth() - 6); startDate = d.toISOString().slice(0, 10); }
      else if (range === "1y") { const d = new Date(now); d.setFullYear(d.getFullYear() - 1); startDate = d.toISOString().slice(0, 10); }

      const klineRes = await api.get("api/v1/admin/sector-index-daily", {
        searchParams: { sectorType: "CONCEPT", sectorCode: code, startDate },
      }).json<{ code: number; data: SectorIndexDaily[] }>();

      setKlines((klineRes.data || []).map(k => ({
        tradeDate: k.tradeDate, open: k.open, high: k.high, low: k.low, close: k.close, volume: k.volume,
      })));

      try {
        const marginRes = await api.get("api/v1/admin/sector-margin", {
          searchParams: { sectorType: "CONCEPT", sectorCode: code, startDate },
        }).json<{ code: number; data: SectorMarginDaily[] }>();
        setMargins((marginRes.data || []).map(m => ({
          tradeDate: m.tradeDate, marginBalance: m.marginBalance, shortBalance: m.shortBalance,
        })));
      } catch {
        setMargins([]);
      }

      await fetchStocks();
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }, [code, range, fetchStocks]);

  useEffect(() => { fetchData(); }, [fetchData]);

  useEffect(() => {
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, []);

  const startPolling = useCallback(() => {
    if (pollRef.current) clearInterval(pollRef.current);
    pollRef.current = setInterval(async () => {
      try {
        const res = await api.get("api/v1/admin/market/constituents/scrape/status", {
          searchParams: { type: "concept", code },
        }).json<{ code: number; data: { status: string; relationCount?: number; snapDate?: string; error?: string } }>();
        const status = res.data?.status;
        if (status === "success") {
          setScrapeStatus("success");
          setScrapeMsg(`抓取成功：${res.data.relationCount ?? 0} 条新增，日期 ${res.data.snapDate ?? ""}`);
          if (pollRef.current) clearInterval(pollRef.current);
          pollRef.current = null;
          await fetchStocks();
        } else if (status === "failed") {
          setScrapeStatus("failed");
          setScrapeMsg(`抓取失败: ${res.data?.error ?? "未知错误"}`);
          if (pollRef.current) clearInterval(pollRef.current);
          pollRef.current = null;
        }
      } catch {
        if (pollRef.current) clearInterval(pollRef.current);
        pollRef.current = null;
        setScrapeStatus("failed");
        setScrapeMsg("状态查询失败");
      }
    }, 3000);
  }, [code, fetchStocks]);

  const handleScrape = async () => {
    setScrapeStatus("scraping");
    setScrapeMsg("正在从同花顺抓取成分股...");
    try {
      await api.post("api/v1/admin/market/constituents/scrape", {
        searchParams: { type: "concept", code },
      }).json<{ code: number; data: { status: string } }>();
      startPolling();
    } catch (e: unknown) {
      setScrapeStatus("failed");
      setScrapeMsg(`启动抓取失败: ${e instanceof Error ? e.message : "未知错误"}`);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2 text-sm text-gray-500">
        <button onClick={() => router.back()} className="hover:underline">← 返回概念列表</button>
        <span className="text-gray-300">|</span>
        <h1 className="text-2xl font-bold text-gray-900">概念详情 {code}</h1>
      </div>

      <div className="rounded-lg border p-4">
        <div className="flex flex-wrap gap-2 items-center mb-3">
          <span className="font-semibold text-sm">概念指数K线</span>
          <span className="text-gray-300">|</span>
          {["1m", "3m", "6m", "1y"].map((r) => <button key={r} onClick={() => setRange(r)} className={`rounded px-2 py-1 text-xs ${range === r ? "bg-blue-600 text-white" : "border hover:bg-gray-50"}`}>{r === "1m" ? "1月" : r === "3m" ? "3月" : r === "6m" ? "6月" : "1年"}</button>)}
          <span className="text-gray-300">|</span>
          {(["daily", "weekly", "monthly"] as const).map((p) => <button key={p} onClick={() => setPeriod(p)} className={`rounded px-2 py-1 text-xs ${period === p ? "bg-blue-600 text-white" : "border hover:bg-gray-50"}`}>{p === "daily" ? "日K" : p === "weekly" ? "周K" : "月K"}</button>)}
        </div>
        {loading ? <div className="h-[380px] flex items-center justify-center text-gray-500">加载中...</div> : <KlineMarginOverlay klines={klines} margins={margins} period={period} />}
      </div>

      <div className="rounded-lg border p-4">
        <div className="flex items-center justify-between mb-2">
          <h3 className="font-semibold text-sm">成分股 ({stocks.length})</h3>
          <button
            onClick={handleScrape}
            disabled={scrapeStatus === "scraping"}
            className={`rounded px-3 py-1 text-xs ${scrapeStatus === "scraping" ? "bg-gray-300 text-gray-500 cursor-not-allowed" : "bg-blue-600 text-white hover:bg-blue-700"}`}
          >
            {scrapeStatus === "scraping" ? "抓取中..." : "抓取成分股"}
          </button>
        </div>
        {scrapeMsg && (
          <p className={`text-xs mb-2 ${scrapeStatus === "success" ? "text-green-600" : scrapeStatus === "failed" ? "text-red-500" : "text-blue-500"}`}>{scrapeMsg}</p>
        )}
        {stocks.length > 0 ? (
          <div className="grid grid-cols-4 gap-2">
            {stocks.map(s => (
              <button key={s.stockCode} onClick={() => router.push(`/admin/stocks/${s.stockCode}`)} className="rounded border px-3 py-2 text-left text-sm hover:bg-gray-50">
                <span className="text-xs text-gray-500">{s.stockCode}</span>
                <span className="ml-2">{s.stockName}</span>
              </button>
            ))}
          </div>
        ) : !loading && (
          <p className="text-sm text-gray-400">暂无成分股数据，点击&ldquo;抓取成分股&rdquo;从同花顺获取</p>
        )}
      </div>
    </div>
  );
}
