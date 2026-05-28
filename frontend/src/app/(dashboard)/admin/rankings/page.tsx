"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import api from "@/lib/api";

interface RankingItem {
  sectorType: string;
  sectorCode: string;
  sectorName: string;
  tradeDate: string;
  amount: number | null;
  amountChange: number | null;
  changePct: number | null;
  changePctChange: number | null;
  volumePct: number | null;
}

type SectorType = "INDUSTRY" | "CONCEPT";
type SortField = "amount" | "amountChange" | "changePct" | "changePctChange";

const SORT_OPTIONS: { key: SortField; label: string }[] = [
  { key: "amount", label: "成交额" },
  { key: "amountChange", label: "成交额环比" },
  { key: "changePct", label: "涨幅" },
  { key: "changePctChange", label: "涨幅环比" },
];

function fmtAmount(val: number | null): string {
  if (val == null) return "-";
  if (Math.abs(val) >= 1e8) return (val / 1e8).toFixed(2) + "亿";
  if (Math.abs(val) >= 1e4) return (val / 1e4).toFixed(2) + "万";
  return val.toFixed(2);
}

function fmtChange(val: number | null): string {
  if (val == null) return "-";
  const sign = val > 0 ? "+" : "";
  return sign + val.toFixed(2) + "%";
}

function fmtChangeAbs(val: number | null): string {
  if (val == null) return "-";
  const sign = val > 0 ? "+" : "";
  if (Math.abs(val) >= 1e8) return sign + (val / 1e8).toFixed(2) + "亿";
  if (Math.abs(val) >= 1e4) return sign + (val / 1e4).toFixed(2) + "万";
  return sign + val.toFixed(2);
}

function changeColor(val: number | null): string {
  if (val == null) return "text-gray-400";
  return val > 0 ? "text-red-600" : val < 0 ? "text-green-600" : "text-gray-500";
}

export default function RankingsPage() {
  const router = useRouter();
  const [sectorType, setSectorType] = useState<SectorType>("INDUSTRY");
  const [tradeDate, setTradeDate] = useState<string>("");
  const [sortBy, setSortBy] = useState<SortField>("amount");
  const [sortDir, setSortDir] = useState<"desc" | "asc">("desc");
  const [data, setData] = useState<RankingItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [displayDate, setDisplayDate] = useState<string>("");

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const params: Record<string, string> = {
        sectorType,
        sortBy,
        sortDir,
      };
      if (tradeDate) params.tradeDate = tradeDate;
      const res = await api
        .get("api/v1/admin/sector-ranking", { searchParams: params })
        .json<{ code: number; data: RankingItem[] }>();
      setData(res.data || []);
      if (res.data && res.data.length > 0 && res.data[0].tradeDate) {
        setDisplayDate(res.data[0].tradeDate.slice(0, 10));
      } else {
        setDisplayDate("");
      }
    } catch (e) {
      console.error(e);
      setData([]);
    } finally {
      setLoading(false);
    }
  }, [sectorType, tradeDate, sortBy, sortDir]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  function handleSort(f: SortField) {
    if (sortBy === f) {
      setSortDir((d) => (d === "desc" ? "asc" : "desc"));
    } else {
      setSortBy(f);
      setSortDir("desc");
    }
  }

  function arrow(f: SortField) {
    return sortBy !== f ? "↕" : sortDir === "desc" ? "↓" : "↑";
  }

  function handleTabChange(t: SectorType) {
    setSectorType(t);
    setSortBy("amount");
    setSortDir("desc");
  }

  const detailPath = (code: string) =>
    sectorType === "INDUSTRY"
      ? `/admin/industries/${code}`
      : `/admin/concepts/${code}`;

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">板块排名</h1>

      <div className="flex items-center gap-4 flex-wrap">
        <div className="flex rounded-lg border overflow-hidden">
          <button
            onClick={() => handleTabChange("INDUSTRY")}
            className={`px-5 py-2 text-sm font-medium transition-colors ${
              sectorType === "INDUSTRY"
                ? "bg-blue-600 text-white"
                : "bg-white text-gray-600 hover:bg-gray-100"
            }`}
          >
            行业排名
          </button>
          <button
            onClick={() => handleTabChange("CONCEPT")}
            className={`px-5 py-2 text-sm font-medium transition-colors ${
              sectorType === "CONCEPT"
                ? "bg-blue-600 text-white"
                : "bg-white text-gray-600 hover:bg-gray-100"
            }`}
          >
            概念排名
          </button>
        </div>

        <div className="flex items-center gap-2">
          <label className="text-xs text-gray-500">交易日期</label>
          <input
            type="date"
            value={tradeDate}
            onChange={(e) => setTradeDate(e.target.value)}
            className="rounded-lg border px-3 py-2 text-sm"
          />
          {tradeDate && (
            <button
              onClick={() => setTradeDate("")}
              className="text-xs text-gray-400 hover:text-gray-600"
            >
              清除
            </button>
          )}
        </div>

        <div className="flex items-center gap-2">
          <label className="text-xs text-gray-500">排序</label>
          <select
            value={sortBy}
            onChange={(e) => {
              setSortBy(e.target.value as SortField);
              setSortDir("desc");
            }}
            className="rounded-lg border px-3 py-2 text-sm"
          >
            {SORT_OPTIONS.map((o) => (
              <option key={o.key} value={o.key}>
                {o.label}
              </option>
            ))}
          </select>
          <button
            onClick={() => setSortDir((d) => (d === "desc" ? "asc" : "desc"))}
            className="rounded-lg border px-3 py-2 text-sm hover:bg-gray-100"
            title={sortDir === "desc" ? "降序" : "升序"}
          >
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
              <th
                onClick={() => handleSort("amount")}
                className="px-3 py-2 text-right cursor-pointer select-none hover:bg-gray-100"
              >
                成交额 <span className="text-blue-600">{arrow("amount")}</span>
              </th>
              <th
                onClick={() => handleSort("amountChange")}
                className="px-3 py-2 text-right cursor-pointer select-none hover:bg-gray-100"
              >
                成交额环比 <span className="text-blue-600">{arrow("amountChange")}</span>
              </th>
              <th
                onClick={() => handleSort("changePct")}
                className="px-3 py-2 text-right cursor-pointer select-none hover:bg-gray-100"
              >
                涨幅 <span className="text-blue-600">{arrow("changePct")}</span>
              </th>
              <th
                onClick={() => handleSort("changePctChange")}
                className="px-3 py-2 text-right cursor-pointer select-none hover:bg-gray-100"
              >
                涨幅环比 <span className="text-blue-600">{arrow("changePctChange")}</span>
              </th>
              <th className="px-3 py-2 text-right">成交占比</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-gray-400">
                  加载中...
                </td>
              </tr>
            ) : data.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-gray-400">
                  无数据
                </td>
              </tr>
            ) : (
              data.map((item, idx) => (
                <tr
                  key={item.sectorCode}
                  className={`border-b hover:bg-gray-50${
                    idx < 3 ? " bg-amber-50/50" : ""
                  }`}
                >
                  <td className="px-3 py-2 text-center font-medium">
                    {idx < 3 ? (
                      <span
                        className={`inline-flex h-6 w-6 items-center justify-center rounded-full text-xs font-bold text-white ${
                          idx === 0
                            ? "bg-yellow-500"
                            : idx === 1
                            ? "bg-gray-400"
                            : "bg-amber-700"
                        }`}
                      >
                        {idx + 1}
                      </span>
                    ) : (
                      <span className="text-gray-500">{idx + 1}</span>
                    )}
                  </td>
                  <td className="px-3 py-2">
                    <span
                      className="text-blue-600 cursor-pointer hover:underline"
                      onClick={() =>
                        router.push(
                          `${detailPath(item.sectorCode)}?name=${encodeURIComponent(
                            item.sectorName
                          )}`
                        )
                      }
                    >
                      {item.sectorName}
                    </span>
                  </td>
                  <td className="px-3 py-2 text-right font-medium">
                    {fmtAmount(item.amount)}
                  </td>
                  <td className={`px-3 py-2 text-right ${changeColor(item.amountChange)}`}>
                    {fmtChangeAbs(item.amountChange)}
                  </td>
                  <td className={`px-3 py-2 text-right font-medium ${changeColor(item.changePct)}`}>
                    {fmtChange(item.changePct)}
                  </td>
                  <td className={`px-3 py-2 text-right ${changeColor(item.changePctChange)}`}>
                    {fmtChange(item.changePctChange)}
                  </td>
                  <td className="px-3 py-2 text-right font-medium">
                    {item.volumePct != null ? item.volumePct.toFixed(2) + "%" : "-"}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {!loading && data.length > 0 && (
        <div className="text-sm text-gray-500">
          共 {data.length} 个{sectorType === "INDUSTRY" ? "行业" : "概念"}
        </div>
      )}
    </div>
  );
}
