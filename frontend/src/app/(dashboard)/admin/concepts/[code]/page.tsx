"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { useApiQuery } from "@/lib/hooks";
import type { SectorIndexDaily, SectorMarginDaily, SectorStockItem, SectorMarginDiff } from "@/lib/types";
import { fmt, fmtDiff, diffColor, fmtChange, fmtVolumePct, getStartDate } from "@/lib/format";
import KlineMarginOverlay from "@/components/chart/KlineMarginOverlay";

type RangeKey = "1m" | "3m" | "6m" | "1y";

export default function ConceptDetailPage() {
  const params = useParams();
  const code = params.code as string;
  const [range, setRange] = useState<RangeKey>("3m");
  const startDate = getStartDate(range);

  // 板块信息（成分股列表）
  const { data: detailData } = useApiQuery<SectorStockItem[]>(
    ["admin", "market", "concepts", code],
    `api/v1/admin/market/concepts/${code}/stocks`
  );

  // K线数据
  const { data: klineData, isLoading: klineLoading } = useApiQuery<SectorIndexDaily[]>(
    ["admin", "sector-index-daily", "concepts", code, range],
    `api/v1/admin/sector-index-daily/concepts/${code}`,
    { startDate }
  );

  // 两融数据
  const { data: marginData, isLoading: marginLoading } = useApiQuery<SectorMarginDaily[]>(
    ["admin", "sector-margin", "concepts", code, range],
    `api/v1/admin/sector-margin/concepts/${code}`,
    { startDate }
  );

  // 两融差额
  const { data: marginDiffData } = useApiQuery<SectorMarginDiff>(
    ["admin", "market", "concepts", code, "margin-diff"],
    `api/v1/admin/market/concepts/${code}/margin-diff`,
    { startDate }
  );

  // 关联度
  const { data: correlationData } = useApiQuery<{ sectors: { code: string; name: string; score: number }[] }>(
    ["admin", "market", "concepts", code, "correlations"],
    `api/v1/admin/market/concepts/${code}/correlations`
  );

  const stocks = detailData?.data || [];
  const klines = klineData?.data || [];
  const margins = marginData?.data || [];
  const marginDiff = marginDiffData?.data;
  const correlations = correlationData?.data?.sectors || [];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <a href="/admin/concepts" className="text-sm text-gray-400 hover:text-blue-600">← 概念板块</a>
      </div>

      <h1 className="text-2xl font-bold">概念详情 {code}</h1>

      {/* 时间范围选择 */}
      <div className="flex gap-2">
        {(["1m", "3m", "6m", "1y"] as RangeKey[]).map((r) => (
          <button key={r} onClick={() => setRange(r)} className={`rounded-lg px-3 py-1 text-sm ${range === r ? "bg-blue-600 text-white" : "border hover:bg-gray-50"}`}>
            {r === "1m" ? "1月" : r === "3m" ? "3月" : r === "6m" ? "6月" : "1年"}
          </button>
        ))}
      </div>

      {/* 两融差额摘要 */}
      {marginDiff && (
        <div className="grid grid-cols-3 gap-4">
          <div className="rounded-lg border bg-white p-4">
            <div className="text-xs text-gray-500 mb-1">融资变化</div>
            <div className={`text-xl font-bold ${diffColor(marginDiff.marginBalance)}`}>{fmtDiff(marginDiff.marginBalance)}</div>
          </div>
          <div className="rounded-lg border bg-white p-4">
            <div className="text-xs text-gray-500 mb-1">融券变化</div>
            <div className={`text-xl font-bold ${diffColor(marginDiff.shortBalance)}`}>{fmtDiff(marginDiff.shortBalance)}</div>
          </div>
          <div className="rounded-lg border bg-white p-4">
            <div className="text-xs text-gray-500 mb-1">总额变化</div>
            <div className={`text-xl font-bold ${diffColor(marginDiff.totalBalance)}`}>{fmtDiff(marginDiff.totalBalance)}</div>
          </div>
        </div>
      )}

      {/* K线+两融叠加图 */}
      <div className="rounded-lg border bg-white p-4">
        <h2 className="text-sm font-semibold mb-2">K线 + 两融</h2>
        {klineLoading || marginLoading ? (
          <div className="h-[400px] flex items-center justify-center text-gray-400">加载中...</div>
        ) : klines.length > 0 ? (
          <KlineMarginOverlay klines={klines} margins={margins} />
        ) : (
          <div className="h-[400px] flex items-center justify-center text-gray-400">无数据</div>
        )}
      </div>

      {/* 关联度 */}
      {correlations.length > 0 && (
        <div className="rounded-lg border bg-white p-4">
          <h2 className="text-sm font-semibold mb-2">关联板块</h2>
          <div className="grid grid-cols-4 gap-2">
            {correlations.slice(0, 12).map((c) => (
              <div key={c.code} className="rounded border px-2 py-1 text-xs flex justify-between">
                <span className="text-blue-600">{c.name}</span>
                <span className="text-gray-500">{(c.score * 100).toFixed(1)}%</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 成分股列表 */}
      <div className="rounded-lg border bg-white p-4">
        <h2 className="text-sm font-semibold mb-2">成分股 ({stocks.length})</h2>
        {stocks.length > 0 ? (
          <div className="grid grid-cols-6 gap-1">
            {stocks.map((s) => (
              <span key={s.stockCode} className="text-xs text-gray-600 truncate">{s.stockName}</span>
            ))}
          </div>
        ) : (
          <div className="text-gray-400 text-sm">无成分股数据</div>
        )}
      </div>
    </div>
  );
}
