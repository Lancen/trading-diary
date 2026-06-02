"use client";

import { useEffect, useRef } from "react";
import { ColorType, createChart, CrosshairMode, LineSeries, Time } from "lightweight-charts";

export interface CrowdednessPoint {
  tradeDate: string;
  crowdedness: number;
}

interface CrowdednessChartProps {
  data: CrowdednessPoint[];
  height?: number;
}

const RISK_LINE_VALUE = 45;

export default function CrowdednessChart({ data, height = 400 }: CrowdednessChartProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!containerRef.current || data.length === 0) return;

    const chart = createChart(containerRef.current, {
      layout: {
        background: { type: ColorType.Solid, color: "#ffffff" },
        textColor: "#333",
      },
      grid: {
        vertLines: { color: "#f0f0f0" },
        horzLines: { color: "#f0f0f0" },
      },
      crosshair: { mode: CrosshairMode.Normal },
      timeScale: {
        borderColor: "#ddd",
        timeVisible: false,
        rightOffset: 5,
        minBarSpacing: 3,
      },
    });

    const lineSeries = chart.addSeries(LineSeries, {
      color: "#2563eb",
      lineWidth: 2,
      priceFormat: { type: "price", precision: 2, minMove: 0.01 },
      title: "拥挤度(%)",
    });

    const lineData = data.map(d => ({
      time: d.tradeDate as Time,
      value: d.crowdedness,
    }));
    lineSeries.setData(lineData);

    const riskSeries = chart.addSeries(LineSeries, {
      color: "#dc2626",
      lineWidth: 1,
      lineStyle: 2,
      priceFormat: { type: "price", precision: 2, minMove: 0.01 },
      title: "风险线(45%)",
      crosshairMarkerVisible: false,
      lastValueVisible: false,
      priceLineVisible: false,
    });

    if (lineData.length > 0) {
      const startTime = lineData[0].time;
      const endTime = lineData[lineData.length - 1].time;
      riskSeries.setData([
        { time: startTime, value: RISK_LINE_VALUE },
        { time: endTime, value: RISK_LINE_VALUE },
      ]);
    }

    chart.timeScale().fitContent();

    const handleResize = () => {
      if (containerRef.current) {
        chart.applyOptions({ width: containerRef.current.clientWidth });
      }
    };
    window.addEventListener("resize", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
      chart.remove();
    };
  }, [data, height]);

  return <div ref={containerRef} style={{ height }} />;
}
