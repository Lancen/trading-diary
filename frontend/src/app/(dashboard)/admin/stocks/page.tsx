"use client";

import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useApiQuery, keys } from "@/lib/hooks";
import type { StockItem, PaginatedData } from "@/lib/types";
import { fmtWan } from "@/lib/format";

type SortField = "changePct" | "volume" | "marginBalance" | "marginChange" | "shortBalance" | "shortChange";

const SORT_FIELDS: { key: SortField; label: string }[] = [
  { key: "changePct", label: "涨跌幅" },
  { key: "volume", label: "成交量" },
  { key: "marginBalance", label: "融资余额" },
  { key: "marginChange", label: "融资变化" },
  { key: "shortBalance", label: "融券余额" },
  { key: "shortChange", label: "融券变化" },
];

export default function StocksPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [keyword, setKeyword] = useState(searchParams.get("keyword") || "");
  const [industry, setIndustry] = useState(searchParams.get("industry") || "");
  const [concept, setConcept] = useState(searchParams.get("concept") || "");
  const [tradeDate, setTradeDate] = useState("");
  const [sortBy, setSortBy] = useState<SortField>("changePct");
  const [sortDir, setSortDir] = useState("desc");
  const [page, setPage] = useState(1);
  const pageSize = 50;

  const params: Record<string, string | number> = { sortBy, sortDir, page, size: pageSize };
  if (keyword) params.keyword = keyword;
  if (industry) params.industry = industry;
  if (concept) params.concept = concept;
  if (tradeDate) params.tradeDate = tradeDate;

  const { data, isLoading, refetch } = useApiQuery<PaginatedData<StockItem>>(
    keys.stocks(params),
    "api/v1/admin/stocks/list",
    params,
  );

  const records = data?.records || [];
  const total = data?.total || 0;

  function handleSort(field: SortField) {
    if (sortBy === field) {
      setSortDir(sortDir === "desc" ? "asc" : "desc");
    } else {
      setSortBy(field);
      setSortDir("desc");
    }
    setPage(1);
  }

  function sortArrow(field: SortField): string {
    if (sortBy !== field) return "↕";
    return sortDir === "desc" ? "↓" : "↑";
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">股票数据</h1>
      <p className="text-sm text-gray-500">浏览已采集的A股行情数据</p>

      <div className="flex flex-wrap gap-3 items-end">
        <div>
          <label className="block text-xs text-gray-500 mb-1">股票代码/名称</label>
          <input value={keyword} onChange={(e) => { setKeyword(e.target.value); setPage(1); }} placeholder="输入搜索" className="rounded-lg border px-3 py-2 text-sm w-40" />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">行业</label>
          <input value={industry} onChange={(e) => { setIndustry(e.target.value); setPage(1); }} placeholder="行业名称" className="rounded-lg border px-3 py-2 text-sm w-32" />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">概念</label>
          <input value={concept} onChange={(e) => { setConcept(e.target.value); setPage(1); }} placeholder="概念名称" className="rounded-lg border px-3 py-2 text-sm w-32" />
        </div>
        <div>
          <label className="block text-xs text-gray-500 mb-1">日期(留空=最新)</label>
          <input type="date" value={tradeDate} onChange={(e) => setTradeDate(e.target.value)} className="rounded-lg border px-3 py-2 text-sm w-36" />
        </div>
        <button onClick={() => refetch()} className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:opacity-90">查询</button>
      </div>

      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm min-w-[1100px]">
          <thead>
            <tr className="border-b bg-gray-50 text-left text-xs text-gray-500">
              <th className="px-3 py-2">代码</th>
              <th className="px-3 py-2">名称</th>
              <th className="px-3 py-2">行业</th>
              <th className="px-3 py-2">概念</th>
              <th className="px-3 py-2 text-right">收盘</th>
              {SORT_FIELDS.map((f) => (
                <th key={f.key} className="px-3 py-2 text-right cursor-pointer select-none hover:bg-gray-100" onClick={() => handleSort(f.key)}>
                  {f.label} <span className="text-blue-600">{sortArrow(f.key)}</span>
                </th>
              ))}
              <th className="px-3 py-2">日期</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={12} className="px-4 py-8 text-center text-gray-400">加载中...</td></tr>
            ) : records.length === 0 ? (
              <tr><td colSpan={12} className="px-4 py-8 text-center text-gray-400">无数据</td></tr>
            ) : records.map((item) => (
              <tr key={item.stockCode + item.tradeDate} className="border-b hover:bg-gray-50 cursor-pointer" onClick={() => router.push(`/admin/stocks/${item.stockCode}`)}>
                <td className="px-3 py-2 font-mono text-blue-600">{item.stockCode}</td>
                <td className="px-3 py-2 text-blue-600 font-medium">{item.stockName}</td>
                <td className="px-3 py-2"><span className="rounded bg-blue-50 px-1.5 py-0.5 text-xs text-blue-700">{item.industry || "-"}</span></td>
                <td className="px-3 py-2 text-xs text-gray-500 max-w-[150px] truncate">{item.concepts || "-"}</td>
                <td className="px-3 py-2 text-right">{item.close}</td>
                <td className={`px-3 py-2 text-right ${item.changePct > 0 ? "text-red-600" : item.changePct < 0 ? "text-green-600" : "text-gray-500"}`}>
                  {item.changePct > 0 ? "+" : ""}{item.changePct?.toFixed(2)}%
                </td>
                <td className="px-3 py-2 text-right">{fmtWan(item.volume, "万")}</td>
                <td className="px-3 py-2 text-right text-blue-600">{fmtWan(item.marginBalance, "亿")}</td>
                <td className={`px-3 py-2 text-right ${item.marginChange > 0 ? "text-red-600" : item.marginChange < 0 ? "text-green-600" : "text-gray-500"}`}>
                  {item.marginChange > 0 ? "↑" : item.marginChange < 0 ? "↓" : ""}{fmtWan(Math.abs(item.marginChange), "亿")}
                </td>
                <td className="px-3 py-2 text-right">{fmtWan(item.shortBalance, "亿")}</td>
                <td className={`px-3 py-2 text-right ${item.shortChange > 0 ? "text-red-600" : item.shortChange < 0 ? "text-green-600" : "text-gray-500"}`}>
                  {item.shortChange > 0 ? "↑" : item.shortChange < 0 ? "↓" : ""}{fmtWan(Math.abs(item.shortChange), "亿")}
                </td>
                <td className="px-3 py-2 text-xs">{item.tradeDate || "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between text-sm text-gray-500">
        <span>共 {total} 只股票 · 按 {SORT_FIELDS.find(f => f.key === sortBy)?.label} {sortDir === "desc" ? "↓" : "↑"}</span>
        <div className="flex gap-1">
          <button onClick={() => setPage(page - 1)} disabled={page <= 1} className="rounded border px-3 py-1 text-sm disabled:opacity-30">上一页</button>
          <span className="px-3 py-1">{page}</span>
          <button onClick={() => setPage(page + 1)} disabled={page * pageSize >= total} className="rounded border px-3 py-1 text-sm disabled:opacity-30">下一页</button>
        </div>
      </div>
    </div>
  );
}
