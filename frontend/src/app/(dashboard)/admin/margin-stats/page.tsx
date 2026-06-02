"use client";

import { useState } from "react";
import { useApiQuery, keys } from "@/lib/hooks";
import type { MarginSummary } from "@/lib/types";
import { fmtYi } from "@/lib/format";

export default function MarginStatsPage() {
  const [tradeDate, setTradeDate] = useState("");

  const { data, isFetching, refetch } = useApiQuery<MarginSummary>(
    keys.marginStats(tradeDate ? { tradeDate } : {}),
    "api/v1/admin/margin-stats/summary",
    { tradeDate: tradeDate || undefined },
  );

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">融资统计</h1>

      <div className="flex gap-3 items-end">
        <div>
          <label className="block text-xs text-gray-500 mb-1">日期（可选）</label>
          <input
            type="date"
            value={tradeDate}
            onChange={(e) => setTradeDate(e.target.value)}
            className="rounded-lg border px-3 py-2 text-sm w-44"
          />
        </div>
        <button onClick={() => refetch()} className="rounded-lg bg-blue-600 px-5 py-2 text-sm font-medium text-white hover:opacity-90">
          查询
        </button>
      </div>

      {isFetching ? (
        <div className="text-center text-gray-400 py-12">加载中...</div>
      ) : !data ? (
        <div className="text-center text-gray-400 py-12">无数据</div>
      ) : (
        <>
          <div className="grid grid-cols-4 gap-4">
            <div className="rounded-lg border bg-white p-5">
              <div className="text-xs text-gray-500 mb-1">融资余额</div>
              <div className="text-2xl font-bold text-blue-600">{fmtYi(data.totalMarginBalance)}</div>
            </div>
            <div className="rounded-lg border bg-white p-5">
              <div className="text-xs text-gray-500 mb-1">融券余额</div>
              <div className="text-2xl font-bold text-orange-600">{fmtYi(data.totalShortBalance)}</div>
            </div>
            <div className="rounded-lg border bg-white p-5">
              <div className="text-xs text-gray-500 mb-1">两融总额</div>
              <div className="text-2xl font-bold text-green-600">{fmtYi(data.totalBalance)}</div>
            </div>
            <div className="rounded-lg border bg-white p-5">
              <div className="text-xs text-gray-500 mb-1">标的数量</div>
              <div className="text-2xl font-bold">{data.stockCount?.toLocaleString() ?? "-"}</div>
            </div>
          </div>
          {data.tradeDate && (
            <div className="text-sm text-gray-400">数据日期：{data.tradeDate}</div>
          )}
        </>
      )}
    </div>
  );
}
