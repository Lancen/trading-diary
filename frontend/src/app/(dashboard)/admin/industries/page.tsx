"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import api from "@/lib/api";

interface Item {
  code: string; name: string; stockCount: number;
  marginBalance: number; marginChange: number;
  shortBalance: number; shortChange: number;
  volumePct: number | null;
  snapDate: string | null;
  pinned: boolean; pinOrder: number | null;
}

type SortField = "stockCount" | "marginBalance" | "marginChange" | "shortBalance" | "shortChange" | "volumePct";
const SORT_FIELDS: { key: SortField; label: string }[] = [
  { key: "stockCount", label: "股票数" }, { key: "marginBalance", label: "融资余额" },
  { key: "marginChange", label: "融资变化" }, { key: "shortBalance", label: "融券余额" }, { key: "shortChange", label: "融券变化" },
  { key: "volumePct", label: "成交占比" },
];

function fmt(val: number | null, unit = "亿"): string {
  if (val == null) return "-";
  return (val / 1e8).toFixed(2) + unit;
}

function isStale(snapDate: string | null): boolean {
  if (!snapDate) return true;
  const d = new Date(snapDate);
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  return diffMs > 30 * 24 * 60 * 60 * 1000;
}

function fmtDate(snapDate: string | null): string {
  if (!snapDate) return "从未采集";
  return snapDate.slice(0, 10);
}

type ScrapeState = "scraping" | "success" | "failed";

export default function IndustriesPage() {
  const router = useRouter();
  const [data, setData] = useState<Item[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState("");
  const [sortBy, setSortBy] = useState<SortField>("marginBalance");
  const [sortDir, setSortDir] = useState("desc");
  const [page, setPage] = useState(1);
  const [scrapeStates, setScrapeStates] = useState<Map<string, ScrapeState>>(new Map());
  const pollRef = useRef<Map<string, ReturnType<typeof setInterval>>>(new Map());
  const revertTimers = useRef<Map<string, ReturnType<typeof setTimeout>>>(new Map());

  useEffect(() => {
    return () => {
      pollRef.current.forEach(v => clearInterval(v));
      revertTimers.current.forEach(v => clearTimeout(v));
    };
  }, []);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string | number> = { sortBy, sortDir, page, size: 50 };
      if (keyword) params.keyword = keyword;
      const res = await api.get("api/v1/admin/market/industries", { searchParams: params }).json<{ code: number; data: { records: Item[]; total: number } }>();
      setData(res.data?.records || []);
      setTotal(res.data?.total || 0);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }, [keyword, sortBy, sortDir, page]);

  useEffect(() => { fetchData(); }, [fetchData]);

  function handleSort(f: SortField) { if (sortBy === f) setSortDir(sortDir === "desc" ? "asc" : "desc"); else { setSortBy(f); setSortDir("desc"); } setPage(1); }
  function arrow(f: SortField) { return sortBy !== f ? "↕" : sortDir === "desc" ? "↓" : "↑"; }

  const setScrapeState = (code: string, state: ScrapeState) => {
    setScrapeStates(prev => new Map(prev).set(code, state));
  };

  const clearScrapeState = (code: string) => {
    setScrapeStates(prev => { const n = new Map(prev); n.delete(code); return n; });
    revertTimers.current.delete(code);
  };

  const scheduleRevert = (code: string, delay = 3000) => {
    const existing = revertTimers.current.get(code);
    if (existing) clearTimeout(existing);
    const timer = setTimeout(() => clearScrapeState(code), delay);
    revertTimers.current.set(code, timer);
  };

  const handleScrape = async (code: string) => {
    setScrapeState(code, "scraping");
    try {
      await api.post("api/v1/admin/market/constituents/scrape", { searchParams: { type: "industry", code } }).json();
      const timer = setInterval(async () => {
        try {
          const res = await api.get("api/v1/admin/market/constituents/scrape/status", { searchParams: { type: "industry", code } }).json<{ code: number; data: { status: string; snapDate?: string; relationCount?: number; error?: string } }>();
          const st = res.data?.status;
          if (st === "success" || st === "failed") {
            clearInterval(timer); pollRef.current.delete(code);
            if (st === "success") {
              setScrapeState(code, "success");
              setData(prev => prev.map(item => {
                if (item.code !== code) return item;
                return {
                  ...item,
                  stockCount: res.data.relationCount ?? item.stockCount,
                  snapDate: res.data.snapDate ?? item.snapDate,
                };
              }));
            } else {
              setScrapeState(code, "failed");
            }
            scheduleRevert(code);
          }
        } catch { clearInterval(timer); pollRef.current.delete(code); setScrapeState(code, "failed"); scheduleRevert(code); }
      }, 3000);
      pollRef.current.set(code, timer);
    } catch { setScrapeState(code, "failed"); scheduleRevert(code); }
  };

  const handlePin = async (code: string, pinned: boolean) => {
    try {
      await api.post("api/v1/admin/market/pin", { json: { type: "industry", code, pinned: String(pinned) } }).json();
      await fetchData();
    } catch (e) { console.error(e); }
  };

  const handleMovePin = async (code: string, direction: "up" | "down") => {
    const pinnedItems = data.filter(d => d.pinned);
    const idx = pinnedItems.findIndex(d => d.code === code);
    if (idx < 0) return;
    const swapIdx = direction === "up" ? idx - 1 : idx + 1;
    if (swapIdx < 0 || swapIdx >= pinnedItems.length) return;
    const newOrder = [...pinnedItems];
    [newOrder[idx], newOrder[swapIdx]] = [newOrder[swapIdx], newOrder[idx]];
    const codes = newOrder.map(d => d.code);
    try {
      await api.post("api/v1/admin/market/pin/reorder", { json: { type: "industry", codes } }).json();
      await fetchData();
    } catch (e) { console.error(e); }
  };

  const pinnedItems = data.filter(d => d.pinned);
  const pinnedIdx = (code: string) => pinnedItems.findIndex(d => d.code === code);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">行业列表</h1>
      <div className="flex gap-3 items-end">
        <div><label className="block text-xs text-gray-500 mb-1">行业名称</label><input value={keyword} onChange={(e) => { setKeyword(e.target.value); setPage(1); }} placeholder="输入搜索" className="rounded-lg border px-3 py-2 text-sm w-40" /></div>
        <button onClick={fetchData} className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:opacity-90">查询</button>
      </div>
      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm min-w-[1000px]">
          <thead><tr className="border-b bg-gray-50 text-left text-xs text-gray-500">
            <th className="px-3 py-2 w-10">置顶</th>
            <th className="px-3 py-2">编码</th><th className="px-3 py-2">名称</th>
            <th className="px-3 py-2">最后采集</th>
            <th className="px-3 py-2">采集页</th>
            <th className="px-3 py-2">操作</th>
            {SORT_FIELDS.map((f) => <th key={f.key} onClick={() => handleSort(f.key)} className="px-3 py-2 text-right cursor-pointer select-none hover:bg-gray-100">{f.label} <span className="text-blue-600">{arrow(f.key)}</span></th>)}
          </tr></thead>
          <tbody>
            {loading ? <tr><td colSpan={12} className="px-4 py-8 text-center text-gray-400">加载中...</td></tr> :
             data.length === 0 ? <tr><td colSpan={12} className="px-4 py-8 text-center text-gray-400">无数据</td></tr> :
             data.map((item) => {
               const stale = isStale(item.snapDate);
               const pidx = pinnedIdx(item.code);
               return (
              <tr key={item.code} className={`border-b hover:bg-gray-50${item.pinned ? " bg-blue-50" : stale ? " bg-amber-50" : ""}`}>
                <td className="px-3 py-2 text-center">
                  {item.pinned ? (
                    <div className="flex flex-col items-center gap-0.5">
                      <button onClick={() => handlePin(item.code, false)} className="text-orange-500 hover:text-orange-700 text-xs" title="取消置顶">📌</button>
                      <div className="flex gap-0.5">
                        <button onClick={() => handleMovePin(item.code, "up")} disabled={pidx <= 0} className="text-gray-400 hover:text-gray-700 disabled:opacity-20 text-xs" title="上移">▲</button>
                        <button onClick={() => handleMovePin(item.code, "down")} disabled={pidx >= pinnedItems.length - 1} className="text-gray-400 hover:text-gray-700 disabled:opacity-20 text-xs" title="下移">▼</button>
                      </div>
                    </div>
                  ) : (
                    <button onClick={() => handlePin(item.code, true)} className="text-gray-300 hover:text-orange-500 text-xs" title="置顶">📌</button>
                  )}
                </td>
                <td className="px-3 py-2 font-mono text-xs text-gray-500">{item.code}</td>
                <td className="px-3 py-2 text-blue-600 cursor-pointer hover:underline" onClick={() => router.push(`/admin/industries/${item.code}?name=${encodeURIComponent(item.name)}`)}>
                  {stale && !item.pinned && <span className="inline-block mr-1 text-amber-500" title="超过30天未更新">&#9888;</span>}
                  {item.name}
                </td>
                <td className={`px-3 py-2 text-xs ${stale ? "text-amber-600 font-medium" : "text-gray-500"}`}>{fmtDate(item.snapDate)}</td>
                <td className="px-3 py-2 text-xs">
                  <a href={`https://q.10jqka.com.cn/thshy/detail/code/${item.code}/`} target="_blank" rel="noopener noreferrer" className="text-blue-500 hover:underline">同花顺</a>
                </td>
                <td className="px-3 py-2">
                  {(() => {
                    const ss = scrapeStates.get(item.code);
                    if (ss === "scraping") return <span className="inline-flex items-center gap-1 rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-500"><span className="animate-spin inline-block w-3 h-3 border-2 border-gray-400 border-t-transparent rounded-full" />采集中</span>;
                    if (ss === "success") return <span className="rounded bg-green-100 px-2 py-0.5 text-xs text-green-700">✓ 成功</span>;
                    if (ss === "failed") return <span className="rounded bg-red-100 px-2 py-0.5 text-xs text-red-700">✗ 失败</span>;
                    return <button onClick={() => handleScrape(item.code)} className="rounded px-2 py-0.5 text-xs bg-blue-100 text-blue-700 hover:bg-blue-200">采集</button>;
                  })()}
                </td>
                <td className="px-3 py-2 text-center text-blue-600 cursor-pointer underline" onClick={() => router.push(`/admin/stocks?industry=${item.name}`)}>{item.stockCount}</td>
                <td className="px-3 py-2 text-right text-blue-600 cursor-pointer" onClick={() => router.push(`/admin/margin-stats/industry/${item.code}`)}>{fmt(item.marginBalance)}</td>
                <td className={`px-3 py-2 text-right cursor-pointer ${item.marginChange > 0 ? "text-red-600" : "text-green-600"}`} onClick={() => router.push(`/admin/margin-stats/industry/${item.code}`)}>{item.marginChange > 0 ? "↑" : "↓"}{fmt(Math.abs(item.marginChange))}</td>
                <td className="px-3 py-2 text-right cursor-pointer" onClick={() => router.push(`/admin/margin-stats/industry/${item.code}`)}>{fmt(item.shortBalance)}</td>
                <td className={`px-3 py-2 text-right cursor-pointer ${item.shortChange > 0 ? "text-red-600" : "text-green-600"}`} onClick={() => router.push(`/admin/margin-stats/industry/${item.code}`)}>{item.shortChange > 0 ? "↑" : "↓"}{fmt(Math.abs(item.shortChange))}</td>
                <td className="px-3 py-2 text-right font-medium">{item.volumePct != null ? item.volumePct.toFixed(2) + "%" : "-"}</td>
              </tr>
            );})}
          </tbody>
        </table>
      </div>
      <div className="flex items-center justify-between text-sm text-gray-500">
        <span>共 {total} 个行业</span>
        <div className="flex gap-1">
          <button onClick={() => setPage(page - 1)} disabled={page <= 1} className="rounded border px-3 py-1 text-sm disabled:opacity-30">上一页</button>
          <span className="px-3 py-1">{page}</span>
          <button onClick={() => setPage(page + 1)} disabled={page * 50 >= total} className="rounded border px-3 py-1 text-sm disabled:opacity-30">下一页</button>
        </div>
      </div>
    </div>
  );
}