"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import api from "@/lib/api";

interface Item {
  code: string; name: string; stockCount: number;
  marginBalance: number; marginChange: number;
  shortBalance: number; shortChange: number;
  snapDate: string | null;
}

type SortField = "stockCount" | "marginBalance" | "marginChange" | "shortBalance" | "shortChange";
const SORT_FIELDS: { key: SortField; label: string }[] = [
  { key: "stockCount", label: "股票数" }, { key: "marginBalance", label: "融资余额" },
  { key: "marginChange", label: "融资变化" }, { key: "shortBalance", label: "融券余额" }, { key: "shortChange", label: "融券变化" },
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

export default function IndustriesPage() {
  const router = useRouter();
  const [data, setData] = useState<Item[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState("");
  const [sortBy, setSortBy] = useState<SortField>("marginBalance");
  const [sortDir, setSortDir] = useState("desc");
  const [page, setPage] = useState(1);
  const [scraping, setScraping] = useState<Set<string>>(new Set());
  const pollRef = useRef<Map<string, ReturnType<typeof setInterval>>>(new Map());

  useEffect(() => {
    return () => {
      pollRef.current.forEach(v => clearInterval(v));
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

  const handleScrape = async (code: string) => {
    setScraping(prev => new Set(prev).add(code));
    try {
      await api.post("api/v1/admin/market/constituents/scrape", {
        searchParams: { type: "industry", code },
      }).json();
      const timer = setInterval(async () => {
        try {
          const res = await api.get("api/v1/admin/market/constituents/scrape/status", {
            searchParams: { type: "industry", code },
          }).json<{ code: number; data: { status: string } }>();
          if (res.data?.status === "success" || res.data?.status === "failed") {
            clearInterval(timer);
            pollRef.current.delete(code);
            setScraping(prev => { const n = new Set(prev); n.delete(code); return n; });
            if (res.data?.status === "success") await fetchData();
          }
        } catch {
          clearInterval(timer);
          pollRef.current.delete(code);
          setScraping(prev => { const n = new Set(prev); n.delete(code); return n; });
        }
      }, 3000);
      pollRef.current.set(code, timer);
    } catch {
      setScraping(prev => { const n = new Set(prev); n.delete(code); return n; });
    }
  };

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">行业列表</h1>
      <div className="flex gap-3 items-end">
        <div><label className="block text-xs text-gray-500 mb-1">行业名称</label><input value={keyword} onChange={(e) => { setKeyword(e.target.value); setPage(1); }} placeholder="输入搜索" className="rounded-lg border px-3 py-2 text-sm w-40" /></div>
        <button onClick={fetchData} className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:opacity-90">查询</button>
      </div>
      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm min-w-[900px]">
          <thead><tr className="border-b bg-gray-50 text-left text-xs text-gray-500">
            <th className="px-3 py-2">编码</th><th className="px-3 py-2">名称</th>
            <th className="px-3 py-2">最后采集</th><th className="px-3 py-2">操作</th>
            {SORT_FIELDS.map((f) => <th key={f.key} onClick={() => handleSort(f.key)} className="px-3 py-2 text-right cursor-pointer select-none hover:bg-gray-100">{f.label} <span className="text-blue-600">{arrow(f.key)}</span></th>)}
          </tr></thead>
          <tbody>
            {loading ? <tr><td colSpan={9} className="px-4 py-8 text-center text-gray-400">加载中...</td></tr> :
             data.length === 0 ? <tr><td colSpan={9} className="px-4 py-8 text-center text-gray-400">无数据</td></tr> :
             data.map((item) => {
               const stale = isStale(item.snapDate);
               return (
              <tr key={item.code} className={`border-b hover:bg-gray-50${stale ? " bg-amber-50" : ""}`}>
                <td className="px-3 py-2 font-mono text-xs text-gray-500">{item.code}</td>
                <td className="px-3 py-2 text-blue-600 cursor-pointer hover:underline" onClick={() => router.push(`/admin/industries/${item.code}`)}>
                  {stale && <span className="inline-block mr-1 text-amber-500" title="超过30天未更新">&#9888;</span>}
                  {item.name}
                </td>
                <td className={`px-3 py-2 text-xs ${stale ? "text-amber-600 font-medium" : "text-gray-500"}`}>{fmtDate(item.snapDate)}</td>
                <td className="px-3 py-2">
                  <button
                    onClick={() => handleScrape(item.code)}
                    disabled={scraping.has(item.code)}
                    className={`rounded px-2 py-0.5 text-xs ${scraping.has(item.code) ? "bg-gray-200 text-gray-400 cursor-not-allowed" : "bg-blue-100 text-blue-700 hover:bg-blue-200"}`}
                  >
                    {scraping.has(item.code) ? "采集中" : "采集"}
                  </button>
                </td>
                <td className="px-3 py-2 text-center text-blue-600 cursor-pointer underline" onClick={() => router.push(`/admin/stocks?industry=${item.name}`)}>{item.stockCount}</td>
                <td className="px-3 py-2 text-right text-blue-600 cursor-pointer" onClick={() => router.push(`/admin/margin-stats/industry/${item.code}`)}>{fmt(item.marginBalance)}</td>
                <td className={`px-3 py-2 text-right cursor-pointer ${item.marginChange > 0 ? "text-red-600" : "text-green-600"}`} onClick={() => router.push(`/admin/margin-stats/industry/${item.code}`)}>{item.marginChange > 0 ? "↑" : "↓"}{fmt(Math.abs(item.marginChange))}</td>
                <td className="px-3 py-2 text-right cursor-pointer" onClick={() => router.push(`/admin/margin-stats/industry/${item.code}`)}>{fmt(item.shortBalance)}</td>
                <td className={`px-3 py-2 text-right cursor-pointer ${item.shortChange > 0 ? "text-red-600" : "text-green-600"}`} onClick={() => router.push(`/admin/margin-stats/industry/${item.code}`)}>{item.shortChange > 0 ? "↑" : "↓"}{fmt(Math.abs(item.shortChange))}</td>
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
