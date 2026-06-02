"use client";

import SectorDetailPage, { type SectorDetailConfig } from "@/components/SectorDetailPage";

const CONFIG: SectorDetailConfig = {
  sectorType: "CONCEPT",
  scrapeType: "concept",
  stocksApiPath: (code: string) => `api/v1/admin/market/concepts/${code}/stocks`,
  klineTitle: "概念指数K线",
  backLabel: "返回概念列表",
};

export default function ConceptDetailPage() {
  return <SectorDetailPage config={CONFIG} />;
}
