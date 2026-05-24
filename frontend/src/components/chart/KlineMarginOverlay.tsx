"use client";

import { useEffect, useRef } from "react";
import { CandlestickData, CandlestickSeries, ColorType, createChart, CrosshairMode, HistogramData, HistogramSeries, LineData, LineSeries, Time } from "lightweight-charts";

export interface KlinePoint {
  tradeDate: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
}

export interface MarginPoint {
  tradeDate: string;
  marginBalance?: number;
  shortBalance?: number;
}

interface KlineMarginOverlayProps {
  klines: KlinePoint[];
  margins?: MarginPoint[];
  period?: "daily" | "weekly" | "monthly";
  height?: number;
}

function getWeekKey(d: string): string {
  const date = new Date(d);
  const day = date.getDay();
  const monday = new Date(date);
  monday.setDate(date.getDate() - (day === 0 ? 6 : day - 1));
  return monday.toISOString().slice(0, 10);
}

function getMonthKey(d: string): string { return d.slice(0, 7); }

function aggregateKlines(klines: KlinePoint[], period: "daily" | "weekly" | "monthly"): KlinePoint[] {
  if (period === "daily") return klines;
  const getKey = period === "weekly" ? getWeekKey : getMonthKey;
  const groups = new Map<string, KlinePoint[]>();
  for (const k of klines) {
    const key = getKey(k.tradeDate);
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key)!.push(k);
  }
  return Array.from(groups.entries()).map(([, bars]) => ({
    tradeDate: bars[bars.length - 1].tradeDate,
    open: bars[0].open,
    high: Math.max(...bars.map(b => b.high)),
    low: Math.min(...bars.map(b => b.low)),
    close: bars[bars.length - 1].close,
    volume: bars.reduce((s, b) => s + b.volume, 0),
  }));
}

export default function KlineMarginOverlay({ klines, margins, period = "daily", height = 380 }: KlineMarginOverlayProps) {
  const chartRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!klines?.length || !chartRef.current) return;

    const container = chartRef.current;
    const chart = createChart(container, {
      layout: { background: { type: ColorType.Solid, color: "#fff" }, textColor: "#6b7280" },
      rightPriceScale: { visible: true, borderColor: "#e5e7eb" },
      crosshair: { mode: CrosshairMode.Normal },
      timeScale: { borderColor: "#e5e7eb" },
    });

    const aggregated = aggregateKlines(klines, period);

    const candleData: CandlestickData<Time>[] = aggregated.map((k) => ({ time: k.tradeDate as Time, open: k.open, high: k.high, low: k.low, close: k.close }));
    const volumeData: HistogramData<Time>[] = aggregated.map((k) => ({ time: k.tradeDate as Time, value: k.volume, color: k.close >= k.open ? "rgba(34,197,94,0.3)" : "rgba(239,68,68,0.3)" }));

    const candleSeries = chart.addSeries(CandlestickSeries, { upColor: "#dc2626", downColor: "#16a34a", borderUpColor: "#dc2626", borderDownColor: "#16a34a", wickUpColor: "#dc2626", wickDownColor: "#16a34a" });
    candleSeries.setData(candleData);

    const volSeries = chart.addSeries(HistogramSeries, { priceFormat: { type: "volume" }, priceScaleId: "" });
    volSeries.setData(volumeData);
    volSeries.priceScale().applyOptions({ scaleMargins: { top: 0.85, bottom: 0 } });

    if (margins?.length) {
      const marginMap = new Map(margins.map((m) => [m.tradeDate, m]));
      const marginBalanceData: LineData<Time>[] = aggregated.map((k) => ({ time: k.tradeDate as Time, value: marginMap.get(k.tradeDate)?.marginBalance ?? undefined })).filter((d): d is LineData<Time> => d.value != null);
      const shortBalanceData: LineData<Time>[] = aggregated.map((k) => ({ time: k.tradeDate as Time, value: marginMap.get(k.tradeDate)?.shortBalance ?? undefined })).filter((d): d is LineData<Time> => d.value != null);

      const marginSeries = chart.addSeries(LineSeries, { priceScaleId: "margin", color: "#2563eb", lineWidth: 1 });
      marginSeries.setData(marginBalanceData);

      const shortSeries = chart.addSeries(LineSeries, { priceScaleId: "margin", color: "#7c3aed", lineWidth: 1, lineStyle: 2 });
      shortSeries.setData(shortBalanceData);
    }

    chart.timeScale().fitContent();
    return () => chart.remove();
  }, [klines, margins, period]);

  return (
    <div>
      <div ref={chartRef} style={{ height }} />
      <div className="flex gap-4 mt-2 text-xs text-gray-500">
        <span>🕯️ K线</span><span>📊 成交量</span><span className="text-blue-600">━━ 融资余额(右轴)</span><span className="text-purple-600">╌╌ 融券余额(右轴)</span>
      </div>
    </div>
  );
}
