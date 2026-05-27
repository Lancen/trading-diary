"use client";

import { useState, useMemo } from "react";
import { useRouter } from "next/navigation";
import { useApiQuery } from "@/lib/hooks";
import type { SectorItem, SectorSortField } from "@/lib/types";
import { fmt, fmtDiff, diffColor, isStale, fmtDate, fmtVolumePct } from "@/lib/format";

const SORT_FIELDS: { key: SectorSortField; label: string }[] = [
  { key: "stockCount", label: "成分股数" },
  { key: "marginBalance", label: "融资余额" },
  { key: "marginChange", label: "融资变化" },
  { key: "shortBalance", label: "融券余额" },
  { key: "shortChange", label: "融券变化" },
  { key: "volumePct", label: "成交占比" },
];

export default function IndustriesPage() {
  const router = useRouter();
  const [sortBy, setSortBy] = useState<SectorSortField>("marginBalance");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("desc");
  const [keyword, setKeyword] = useState("");

  const { data, isLoading } = useApiQuery<SectorItem[]>(
    ["admin", "market", "industries"],
    "api/v1/admin/market/industries"
  );

  const items = useMemo(() => {
    let list = data?.data || [];
    if (keyword) list = list.filter((s) => s.name.includes(keyword) || s.code.includes(keyword));
    list.sort((a, b) => {
      const av = a[sortBy] ?? 0;
      const bv = b[sortBy] ?? 0;
      return sortDir === "desc" ? (bv as number) - (av as number) : (av as number) - (bv as number);
    });
    return list.sort((a, b) => (b.pinned ? 1 : 0) - (a.pinned ? 1 : 0));
  }, [data?.data, sortBy, sortDir, keyword]);

  function handleSort(field: SectorSortField) {
    if (sortBy === field) setSortDir(sortDir === "desc" ? "asc" : "desc");
    else { setSortBy(field); setSortDir("desc"); }
  }

  function sortArrow(field: SectorSortField) {
    if (sortBy !== field) return "↕";
    return sortDir === "desc" ? "↓" : "↑";
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">行业板块</h1>
          <p className="text-sm text-gray-500">浏览已采集的行业板块数据</p>
        </div>
        <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="搜索板块名称/代码" className="rounded-lg border px-3 py-2 text-sm w-52" />
      </div>

      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm min-w-[1000px]">
          <thead>
            <tr className="border-b bg-gray-50 text-left text-xs text-gray-500">
              <th className="px-3 py-2 w-8"></th>
              <th className="px-3 py-2">代码</th>
              <th className="px-3 py-2">名称</th>
              {SORT_FIELDS.map((f) => (
                <th key={f.key} className="px-3 py-2 text-right cursor-pointer select-none hover:bg-gray-100" onClick={() => handleSort(f.key)}>
                  {f.label} <span className="text-blue-600">{sortArrow(f.key)}</span>
                </th>
              ))}
              <th className="px-3 py-2">快照日期</th>
              <th className="px-3 py-2 w-20">操作</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={11} className="px-4 py-8 text-center text-gray-400">加载中...</td></tr>
            ) : items.length === 0 ? (
              <tr><td colSpan={11} className="px-4 py-8 text-center text-gray-400">无数据</td></tr>
            ) : items.map((item) => (
              <tr key={item.code} className={`border-b hover:bg-gray-50 cursor-pointer ${item.pinned ? "bg-yellow-50/50" : ""}`} onClick={() => router.push(`/admin/industries/${item.code}`)}>
                <td className="px-3 py-2">{item.pinned && "📌"}</td>
                <td className="px-3 py-2 font-mono">{item.code}</td>
                <td className="px-3 py-2 font-medium text-blue-600">{item.name}</td>
                <td className="px-3 py-2 text-right">{item.stockCount}</td>
                <td className="px-3 py-2 text-right">{fmt(item.marginBalance)}</td>
                <td className={`px-3 py-2 text-right ${diffColor(item.marginChange)}`}>{fmtDiff(item.marginChange)}</td>
                <td className="px-3 py-2 text-right">{fmt(item.shortBalance)}</td>
                <td className={`px-3 py-2 text-right ${diffColor(item.shortChange)}`}>{fmtDiff(item.shortChange)}</td>
                <td className="px-3 py-2 text-right">{fmtVolumePct(item.volumePct)}</td>
                <td className={`px-3 py-2 text-xs ${isStale(item.snapDate) ? "text-red-400" : ""}`}>{fmtDate(item.snapDate)}</td>
                <td className="px-3 py-2">
                  <button className="text-xs text-blue-600 hover:underline" onClick={(e) => { e.stopPropagation(); router.push(`/admin/industries/${item.code}`); }}>详情</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="text-sm text-gray-500">共 {items.length} 个行业板块</div>
    </div>
  );
}
