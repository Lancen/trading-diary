"use client";

import SectorDetailPage, { type SectorDetailConfig } from "@/components/SectorDetailPage";

const CONFIG: SectorDetailConfig = {
  sectorType: "INDUSTRY",
  scrapeType: "industry",
  stocksApiPath: (code: string) => `api/v1/admin/market/industries/${code}/stocks`,
  klineTitle: "行业指数K线",
  backLabel: "返回行业列表",
};

export default function IndustryDetailPage() {
  return <SectorDetailPage config={CONFIG} />;
}
