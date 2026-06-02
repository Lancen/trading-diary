"use client";

import SectorListPage, { type SectorTypeConfig } from "@/components/SectorListPage";

const CONFIG: SectorTypeConfig = {
  sectorType: "industry",
  apiPath: "api/v1/admin/market/industries",
  title: "行业列表",
  searchLabel: "行业名称",
  countLabel: "个行业",
  basePath: "/admin/industries",
  stockFilterKey: "industry",
  marginStatsPath: "/admin/margin-stats/industry",
  externalLink: (code: string) => `https://q.10jqka.com.cn/thshy/detail/code/${code}/`,
};

export default function IndustriesPage() {
  return <SectorListPage config={CONFIG} />;
}
