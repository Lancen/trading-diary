"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useApiQuery } from "@/lib/hooks";
import type { RankingItem, SectorType, RankingSortField } from "@/lib/types";
import { fmtAmount, fmtChange, fmtChangeAbs, changeColor, fmtVolumePct } from "@/lib/format";

const SORT_OPTIONS: { key: RankingSortField; label: string }[] = [
  { key: "amount", label: "成交额" },
  { key: "amountChange", label: "成交额环比" },
  { key: "changePct", label: "涨幅" },
  { key: "changePctChange", label: "涨幅环比" },
];

export default function RankingsPage() {
  const router = useRouter();
  const [sectorType, setSectorType] = useState<SectorType>("INDUSTRY");
  const [tradeDate, setTradeDate] = useState("");
  const [sortBy, setSortBy] = useState<RankingSortField>("amount");
  const [sortDir, setSortDir] = useState<"desc" | "asc">("desc");

  const { data, isLoading } = useApiQuery<RankingItem[]>(
    ["admin", "sector-ranking", { sectorType, tradeDate, sortBy, sortDir }],
    "api/v1/admin/sector-ranking",
    { sectorType, tradeDate: tradeDate || undefined, sortBy, sortDir }
  );

  const items = data?.data || [];
  const displayDate = items.length > 0 ? items[0].tradeDate?.slice(0, 10) : "";

  function handleSort(f: RankingSortField) {
    if (sortBy === f) setSortDir(sortDir === "desc" ? "asc" : "desc");
    else { setSortBy(f); setSortDir("desc"); }
  }

  function arrow(f: RankingSortField) {
    if (sortBy !== f) return "↕";
    return sortDir === "desc" ? "↓" : "↑";
  }

  function handleTabChange(t: SectorType) {
    setSectorType(t);
    setSortBy("amount");
    setSortDir("desc");
  }

  const detailPath = (code: string) =>
    sectorType === "INDUSTRY" ? `/admin/industries/${code}` : `/admin/concepts/${code}`;

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">板块排名</h1>

      <div className="flex items-center gap-4 flex-wrap">
        <div className="flex rounded-lg border overflow-hidden">
          {(["INDUSTRY", "CONCEPT"] as SectorType[]).map((t) => (
            <button key={t} onClick={() => handleTabChange(t)} className={`px-5 py-2 text-sm font-medium transition-colors ${sectorType === t ? "bg-blue-600 text-white" : "bg-white text-gray-600 hover:bg-gray-100"}`}>
              {t === "INDUSTRY" ? "行业排名" : "概念排名"}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2">
          <label className="text-xs text-gray-500">交易日期</label>
          <input type="date" value={tradeDate} onChange={(e) => setTradeDate(e.target.value)} className="rounded-lg border px-3 py-2 text-sm" />
          {tradeDate && <button onClick={() => setTradeDate("")} className="text-xs text-gray-400 hover:text-gray-600">清除</button>}
        </div>

        <div className="flex items-center gap-2">
          <label className="text-xs text-gray-500">排序</label>
          <select value={sortBy} onChange={(e) => { setSortBy(e.target.value as RankingSortField); setSortDir("desc"); }} className="rounded-lg border px-3 py-2 text-sm">
            {SORT_OPTIONS.map((o) => <option key={o.key} value={o.key}>{o.label}</option>)}
          </select>
          <button onClick={() => setSortDir(sortDir === "desc" ? "asc" : "desc")} className="rounded-lg border px-3 py-2 text-sm hover:bg-gray-100" title={sortDir === "desc" ? "降序" : "升序"}>
            {sortDir === "desc" ? "↓ 降序" : "↑ 升序"}
          </button>
        </div>
      </div>

      {displayDate && (
        <div className="text-sm text-gray-500">
          数据日期：<span className="font-medium text-gray-700">{displayDate}</span>
        </div>
      )}

      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm min-w-[900px]">
          <thead>
            <tr className="border-b bg-gray-50 text-left text-xs text-gray-500">
              <th className="px-3 py-2 w-12 text-center">排名</th>
              <th className="px-3 py-2">名称</th>
              <th onClick={() => handleSort("amount")} className="px-3 py-2 text-right cursor-pointer select-none hover:bg-gray-100">
                成交额 <span className="text-blue-600">{arrow("amount")}</span>
              </th>
              <th onClick={() => handleSort("amountChange")} className="px-3 py-2 text-right cursor-pointer select-none hover:bg-gray-100">
                成交额环比 <span className="text-blue-600">{arrow("amountChange")}</span>
              </th>
              <th onClick={() => handleSort("changePct")} className="px-3 py-2 text-right cursor-pointer select-none hover:bg-gray-100">
                涨幅 <span className="text-blue-600">{arrow("changePct")}</span>
              </th>
              <th onClick={() => handleSort("changePctChange")} className="px-3 py-2 text-right cursor-pointer select-none hover:bg-gray-100">
                涨幅环比 <span className="text-blue-600">{arrow("changePctChange")}</span>
              </th>
              <th className="px-3 py-2 text-right">成交占比</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">加载中...</td></tr>
            ) : items.length === 0 ? (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">无数据</td></tr>
            ) : items.map((item, idx) => (
              <tr key={item.sectorCode} className={`border-b hover:bg-gray-50${idx < 3 ? " bg-amber-50/50" : ""}`}>
                <td className="px-3 py-2 text-center font-medium">
                  {idx < 3 ? (
                    <span className={`inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold text-white ${idx === 0 ? "bg-yellow-500" : idx === 1 ? "bg-gray-400" : "bg-amber-700"}`}>
                      {idx + 1}
                    </span>
                  ) : (
                    <span className="text-gray-500">{idx + 1}</span>
                  )}
                </td>
                <td className="px-3 py-2">
                  <span className="text-blue-600 cursor-pointer hover:underline" onClick={() => router.push(`${detailPath(item.sectorCode)}?name=${encodeURIComponent(item.sectorName)}`)}>
                    {item.sectorName}
                  </span>
                </td>
                <td className="px-3 py-2 text-right font-medium">{fmtAmount(item.amount)}</td>
                <td className={`px-3 py-2 text-right ${changeColor(item.amountChange)}`}>{fmtChangeAbs(item.amountChange)}</td>
                <td className={`px-3 py-2 text-right font-medium ${changeColor(item.changePct)}`}>{fmtChange(item.changePct)}</td>
                <td className={`px-3 py-2 text-right ${changeColor(item.changePctChange)}`}>{fmtChange(item.changePctChange)}</td>
                <td className="px-3 py-2 text-right font-medium">{fmtVolumePct(item.volumePct)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {!isLoading && items.length > 0 && (
        <div className="text-sm text-gray-500">
          共 {items.length} 个{sectorType === "INDUSTRY" ? "行业" : "概念"}
        </div>
      )}
    </div>
  );
}