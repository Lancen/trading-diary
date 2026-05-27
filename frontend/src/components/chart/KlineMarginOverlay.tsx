"use client";

import { useEffect, useRef } from "react";
import { CandlestickData, CandlestickSeries, ColorType, createChart, CrosshairMode, HistogramData, HistogramSeries, IChartApi, LineData, LineSeries, LogicalRangeChangeEventHandler, TickMarkType, Time } from "lightweight-charts";

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

function aggregateMargins(margins: MarginPoint[], period: "daily" | "weekly" | "monthly"): MarginPoint[] {
  if (period === "daily") return margins;
  const getKey = period === "weekly" ? getWeekKey : getMonthKey;
  const groups = new Map<string, MarginPoint[]>();
  for (const m of margins) {
    const key = getKey(m.tradeDate);
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key)!.push(m);
  }
  return Array.from(groups.entries()).map(([, points]) => {
    const last = points[points.length - 1];
    return {
      tradeDate: last.tradeDate,
      marginBalance: last.marginBalance,
      shortBalance: last.shortBalance,
    };
  });
}

function timeToDate(time: Time): Date {
  if (typeof time === "number") return new Date(time * 1000);
  if (typeof time === "string") return new Date(time);
  const bd = time as { year: number; month: number; day: number };
  return new Date(bd.year, bd.month - 1, bd.day);
}

function makeTickMarkFormatter(p: "daily" | "weekly" | "monthly") {
  return (time: Time, tickMarkType: TickMarkType) => {
    const d = timeToDate(time);
    switch (tickMarkType) {
      case TickMarkType.Year:
        return `${d.getFullYear()}`;
      case TickMarkType.Month:
        return p === "monthly"
          ? `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, "0")}`
          : `${d.getMonth() + 1}月`;
      case TickMarkType.DayOfMonth:
        return `${d.getMonth() + 1}/${d.getDate()}`;
      default:
        return `${d.getMonth() + 1}/${d.getDate()}`;
    }
  };
}

function formatLargeNumber(v: number): string {
  const abs = Math.abs(v);
  if (abs >= 1e8) return (v / 1e8).toFixed(2) + "亿";
  if (abs >= 1e4) return (v / 1e4).toFixed(2) + "万";
  if (abs >= 1) return v.toFixed(2);
  return v.toFixed(4);
}

function formatPrice(v: number): string {
  return v.toFixed(2);
}

function syncTimeScales(charts: IChartApi[]) {
  let syncing = false;
  const handlers: LogicalRangeChangeEventHandler[] = charts.map((chart, i) => {
    return (logicalRange) => {
      if (syncing || !logicalRange) return;
      syncing = true;
      for (let j = 0; j < charts.length; j++) {
        if (j === i) continue;
        charts[j].timeScale().setVisibleLogicalRange(logicalRange);
      }
      syncing = false;
    };
  });
  charts.forEach((chart, i) => {
    chart.timeScale().subscribeVisibleLogicalRangeChange(handlers[i]);
  });
  return () => {
    charts.forEach((chart, i) => {
      chart.timeScale().unsubscribeVisibleLogicalRangeChange(handlers[i]);
    });
  };
}

const UP_SOLID = "#dc2626";
const UP_HOLLOW = "#ffffff";
const UP_BORDER_FAINT = "rgba(220,38,38,0.4)";
const UP_BORDER_SOLID = "#dc2626";
const UP_WICK_FAINT = "rgba(220,38,38,0.4)";
const UP_WICK_SOLID = "#dc2626";
const DN_SOLID = "#16a34a";
const DN_HOLLOW = "#ffffff";
const DN_BORDER_FAINT = "rgba(22,163,74,0.4)";
const DN_BORDER_SOLID = "#16a34a";
const DN_WICK_FAINT = "rgba(22,163,74,0.4)";
const DN_WICK_SOLID = "#16a34a";

function candleStyle(k: KlinePoint) {
  const pct = Math.abs((k.close - k.open) / k.open) * 100;
  const isUp = k.close >= k.open;
  if (isUp) {
    if (pct < 3) return { color: UP_HOLLOW, borderColor: UP_BORDER_FAINT, wickColor: UP_WICK_FAINT };
    if (pct <= 7) return { color: UP_SOLID, borderColor: UP_BORDER_SOLID, wickColor: UP_WICK_SOLID };
    return { color: UP_HOLLOW, borderColor: UP_BORDER_SOLID, wickColor: UP_WICK_SOLID };
  }
  if (pct < 3) return { color: DN_HOLLOW, borderColor: DN_BORDER_FAINT, wickColor: DN_WICK_FAINT };
  if (pct <= 7) return { color: DN_SOLID, borderColor: DN_BORDER_SOLID, wickColor: DN_WICK_SOLID };
  return { color: DN_HOLLOW, borderColor: DN_BORDER_SOLID, wickColor: DN_WICK_SOLID };
}

export default function KlineMarginOverlay({ klines, margins, period = "daily", height = 460 }: KlineMarginOverlayProps) {
  const klineRef = useRef<HTMLDivElement>(null);
  const volumeRef = useRef<HTMLDivElement>(null);
  const marginRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const hasKlines = (klines?.length ?? 0) > 0;
    const hasMargins = (margins?.length ?? 0) > 0;
    if (!hasKlines && !hasMargins) return;

    const charts: IChartApi[] = [];
    const cleanups: (() => void)[] = [];
    const tickFormatter = makeTickMarkFormatter(period);

    const commonOpts = {
      layout: { background: { type: ColorType.Solid, color: "#fff" }, textColor: "#6b7280" },
      rightPriceScale: { visible: true, borderColor: "#e5e7eb" },
      crosshair: { mode: CrosshairMode.Normal },
      localization: {
        locale: "zh-CN",
        dateFormat: "yyyy-MM-dd",
        priceFormatter: formatPrice,
      },
    };

    const hiddenTimeScale = {
      borderColor: "#e5e7eb",
      timeVisible: false,
      secondsVisible: false,
      visible: false,
      tickMarkFormatter: tickFormatter,
    };

    const bottomTimeScale = {
      borderColor: "#e5e7eb",
      timeVisible: false,
      secondsVisible: false,
      visible: true,
      tickMarkFormatter: tickFormatter,
    };

    if (hasKlines) {
      const aggregated = aggregateKlines(klines, period);

      if (klineRef.current) {
        const klineChart = createChart(klineRef.current, {
          ...commonOpts,
          timeScale: hiddenTimeScale,
        });
        const candleData: CandlestickData<Time>[] = aggregated.map((k) => ({
          time: k.tradeDate as Time, open: k.open, high: k.high, low: k.low, close: k.close, ...candleStyle(k),
        }));
        const candleSeries = klineChart.addSeries(CandlestickSeries, {
          upColor: UP_SOLID, downColor: DN_SOLID,
          borderUpColor: UP_BORDER_SOLID, borderDownColor: DN_BORDER_SOLID,
          wickUpColor: UP_WICK_SOLID, wickDownColor: DN_WICK_SOLID,
        });
        candleSeries.setData(candleData);
        candleSeries.applyOptions({
          priceFormat: { type: "custom", formatter: formatPrice, minMove: 0.01 },
        });
        charts.push(klineChart);
        cleanups.push(() => klineChart.remove());
      }

      if (volumeRef.current) {
        const volChart = createChart(volumeRef.current, {
          ...commonOpts,
          timeScale: hiddenTimeScale,
        });
        const volumeData: HistogramData<Time>[] = aggregated.map((k) => ({
          time: k.tradeDate as Time, value: k.volume,
          color: k.close >= k.open ? "rgba(239,68,68,0.5)" : "rgba(34,197,94,0.5)",
        }));
        const volSeries = volChart.addSeries(HistogramSeries, {
          priceFormat: { type: "custom", formatter: formatLargeNumber, minMove: 1 },
        });
        volSeries.setData(volumeData);
        charts.push(volChart);
        cleanups.push(() => volChart.remove());
      }
    }

    if (hasMargins && marginRef.current) {
      const aggregatedMargins = aggregateMargins(margins!, period);
      const marginChart = createChart(marginRef.current, {
        ...commonOpts,
        timeScale: hasKlines ? bottomTimeScale : { ...bottomTimeScale, visible: true },
      });

      const marginBalanceData: LineData<Time>[] = aggregatedMargins
        .filter((m) => m.marginBalance != null)
        .map((m) => ({ time: m.tradeDate as Time, value: m.marginBalance! }));

      const shortBalanceData: LineData<Time>[] = aggregatedMargins
        .filter((m) => m.shortBalance != null)
        .map((m) => ({ time: m.tradeDate as Time, value: m.shortBalance! }));

      if (marginBalanceData.length > 0) {
        const marginSeries = marginChart.addSeries(LineSeries, {
          color: "#2563eb", lineWidth: 1,
          priceFormat: { type: "custom", formatter: formatLargeNumber, minMove: 0.01 },
        });
        marginSeries.setData(marginBalanceData);
      }

      if (shortBalanceData.length > 0) {
        const shortSeries = marginChart.addSeries(LineSeries, {
          color: "#7c3aed", lineWidth: 1, lineStyle: 2,
          priceFormat: { type: "custom", formatter: formatLargeNumber, minMove: 0.01 },
        });
        shortSeries.setData(shortBalanceData);
      }

      charts.push(marginChart);
      cleanups.push(() => marginChart.remove());
    }

    if (charts.length > 1) {
      cleanups.push(syncTimeScales(charts));
    }

    charts.forEach((c) => c.timeScale().fitContent());

    return () => {
      cleanups.forEach((fn) => fn());
    };
  }, [klines, margins, period]);

  const klineH = Math.round(height * 0.5);
  const volH = Math.round(height * 0.2);
  const marginH = Math.round(height * 0.3);

  return (
    <div>
      <div className="relative">
        <span className="absolute top-1 left-2 text-xs text-gray-400">价格</span>
        <div ref={klineRef} style={{ height: klineH }} />
      </div>
      <div className="relative">
        <span className="absolute top-1 left-2 text-xs text-gray-400">成交量</span>
        <div ref={volumeRef} style={{ height: volH }} />
      </div>
      <div className="relative">
        <span className="absolute top-1 left-2 text-xs text-gray-400">两融余额</span>
        <div ref={marginRef} style={{ height: marginH }} />
      </div>
      <div className="flex gap-4 mt-2 text-xs text-gray-500">
        <span className="text-red-600">━━ K线</span>
        <span className="text-gray-500">━━ 成交量</span>
        <span className="text-blue-600">━━ 融资余额</span>
        <span className="text-purple-600">╌╌ 融券余额</span>
      </div>
    </div>
  );
}
