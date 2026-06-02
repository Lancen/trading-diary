"use client";

import { useCallback, useEffect, useState } from "react";
import api from "@/lib/api";
import CrowdednessChart, { CrowdednessPoint } from "@/components/chart/CrowdednessChart";

interface CrowdednessDaily {
  tradeDate: string;
  crowdedness: number;
  totalAmount: number;
  topAmount: number;
  totalStocks: number;
  topStocks: number;
}

function fmtAmount(val: number | null): string {
  if (val == null) return "-";
  return (val / 1e8).toFixed(2) + "亿";
}

export default function CrowdednessPage() {
  const [data, setData] = useState<CrowdednessDaily[]>([]);
  const [loading, setLoading] = useState(true);
  const [range, setRange] = useState("1y");
  const [latest, setLatest] = useState<CrowdednessDaily | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const now = new Date();
      let startDate = "";
      if (range === "6m") {
        const d = new Date(now); d.setMonth(d.getMonth() - 6); startDate = d.toISOString().slice(0, 10);
      } else if (range === "1y") {
        const d = new Date(now); d.setFullYear(d.getFullYear() - 1); startDate = d.toISOString().slice(0, 10);
      } else if (range === "3y") {
        const d = new Date(now); d.setFullYear(d.getFullYear() - 3); startDate = d.toISOString().slice(0, 10);
      } else if (range === "all") {
        startDate = "2006-01-01";
      }

      const res = await api.get("api/v1/admin/crowdedness", {
        searchParams: { startDate },
      }).json<{ code: number; data: CrowdednessDaily[] }>();

      const rows = res.data || [];
      setData(rows);
      setLatest(rows.length > 0 ? rows[rows.length - 1] : null);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }, [range]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const chartData: CrowdednessPoint[] = data.map(d => ({
    tradeDate: d.tradeDate,
    crowdedness: d.crowdedness,
  }));

  const isAboveRisk = latest != null && latest.crowdedness >= 45;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">市场拥挤度</h1>

      <div className="rounded-lg border bg-amber-50 p-4 text-sm text-amber-800">
        <p className="font-semibold mb-1">指标说明</p>
        <p>拥挤度 = 成交额排名前5%个股的总成交额 / 全市场总成交额。历史表明 <strong>45%</strong> 是风险边界，突破45%往往对应市场大变化（牛熊转换或风格切换）。</p>
      </div>

      {latest && (
        <div className="grid grid-cols-4 gap-4">
          <div className="rounded-lg border bg-white p-5">
            <div className="text-xs text-gray-500 mb-1">当前拥挤度</div>
            <div className={`text-2xl font-bold ${isAboveRisk ? "text-red-600" : "text-blue-600"}`}>
              {latest.crowdedness.toFixed(2)}%
            </div>
            {isAboveRisk && <div className="text-xs text-red-500 mt-1">⚠ 已突破45%风险线</div>}
          </div>
          <div className="rounded-lg border bg-white p-5">
            <div className="text-xs text-gray-500 mb-1">全市场成交额</div>
            <div className="text-2xl font-bold">{fmtAmount(latest.totalAmount)}</div>
          </div>
          <div className="rounded-lg border bg-white p-5">
            <div className="text-xs text-gray-500 mb-1">前5%个股成交额</div>
            <div className="text-2xl font-bold">{fmtAmount(latest.topAmount)}</div>
          </div>
          <div className="rounded-lg border bg-white p-5">
            <div className="text-xs text-gray-500 mb-1">个股数 / 前5%数</div>
            <div className="text-2xl font-bold">{latest.totalStocks} / {latest.topStocks}</div>
          </div>
        </div>
      )}

      <div className="rounded-lg border p-4">
        <div className="flex flex-wrap gap-2 items-center mb-3">
          <span className="font-semibold text-sm">拥挤度走势</span>
          <span className="text-gray-300">|</span>
          {[
            { key: "6m", label: "6月" },
            { key: "1y", label: "1年" },
            { key: "3y", label: "3年" },
            { key: "all", label: "全部" },
          ].map(r => (
            <button
              key={r.key}
              onClick={() => setRange(r.key)}
              className={`rounded px-2 py-1 text-xs ${range === r.key ? "bg-blue-600 text-white" : "border hover:bg-gray-50"}`}
            >
              {r.label}
            </button>
          ))}
        </div>
        {loading ? (
          <div className="flex items-center justify-center text-gray-500" style={{ height: 400 }}>加载中...</div>
        ) : chartData.length === 0 ? (
          <div className="flex items-center justify-center text-gray-500" style={{ height: 400 }}>暂无数据</div>
        ) : (
          <CrowdednessChart data={chartData} />
        )}
      </div>

      {latest && (
        <div className="text-sm text-gray-400">数据日期：{latest.tradeDate}</div>
      )}
    </div>
  );
}
