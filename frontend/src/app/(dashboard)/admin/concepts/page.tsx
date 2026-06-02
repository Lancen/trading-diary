"use client";

import SectorListPage, { type SectorTypeConfig } from "@/components/SectorListPage";

const CONFIG: SectorTypeConfig = {
  sectorType: "concept",
  apiPath: "api/v1/admin/market/concepts",
  title: "概念列表",
  searchLabel: "概念名称",
  countLabel: "个概念",
  basePath: "/admin/concepts",
  stockFilterKey: "concept",
  marginStatsPath: "/admin/margin-stats/concept",
  externalLink: (code: string) => `https://q.10jqka.com.cn/gn/detail/code/${code}/`,
};

export default function ConceptsPage() {
  return <SectorListPage config={CONFIG} />;
}
