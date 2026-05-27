"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { useApiQuery } from "@/lib/hooks";
import type { SectorIndexDaily, SectorMarginDaily, SectorStockItem, SectorMarginDiff } from "@/lib/types";
import { fmtDiff, diffColor, getStartDate } from "@/lib/format";
import KlineMarginOverlay from "@/components/chart/KlineMarginOverlay";
import type { SectorTypeConfig } from "@/components/SectorListPage";

type RangeKey = "1m" | "3m" | "6m" | "1y";

const RANGE_LABELS: Record<RangeKey, string> = { "1m": "1月", "3m": "3月", "6m": "6月", "1y": "1年" };

/** 参数化板块详情组件，消除 concepts/industries 近克隆 */
export default function SectorDetailPage({ config }: { config: SectorTypeConfig }) {
  const params = useParams();
  const code = params.code as string;
  const [range, setRange] = useState<RangeKey>("3m");
  const startDate = getStartDate(range);

  const { data: detailData } = useApiQuery<SectorStockItem[]>(
    [...config.queryKey, code],
    `${config.apiPath}/${code}/stocks`
  );

  const { data: klineData, isLoading: klineLoading } = useApiQuery<SectorIndexDaily[]>(
    ["admin", "sector-index-daily", config.type, code, range],
    `api/v1/admin/sector-index-daily/${config.type}/${code}`,
    { startDate }
  );

  const { data: marginData, isLoading: marginLoading } = useApiQuery<SectorMarginDaily[]>(
    ["admin", "sector-margin", config.type, code, range],
    `api/v1/admin/sector-margin/${config.type}/${code}`,
    { startDate }
  );

  const { data: marginDiffData } = useApiQuery<SectorMarginDiff>(
    [...config.queryKey, code, "margin-diff"],
    `api/v1/admin/market/${config.type}/${code}/margin-diff`,
    { startDate }
  );

  const { data: correlationData } = useApiQuery<{ sectors: { code: string; name: string; score: number }[] }>(
    [...config.queryKey, code, "correlations"],
    `api/v1/admin/market/${config.type}/${code}/correlations`
  );

  const stocks = detailData?.data || [];
  const klines = klineData?.data || [];
  const margins = marginData?.data || [];
  const marginDiff = marginDiffData?.data;
  const correlations = correlationData?.data?.sectors || [];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <a href={config.detailRoute} className="text-sm text-gray-400 hover:text-blue-600">← {config.title}</a>
      </div>

      <h1 className="text-2xl font-bold">{config.title.replace("板块", "")}详情 {code}</h1>

      <div className="flex gap-2">
        {(Object.keys(RANGE_LABELS) as RangeKey[]).map((r) => (
          <button key={r} onClick={() => setRange(r)} className={`rounded-lg px-3 py-1 text-sm ${range === r ? "bg-blue-600 text-white" : "border hover:bg-gray-50"}`}>
            {RANGE_LABELS[r]}
          </button>
        ))}
      </div>

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