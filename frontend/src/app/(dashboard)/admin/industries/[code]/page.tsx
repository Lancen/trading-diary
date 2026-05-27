"use client";

import SectorDetailPage from "@/components/SectorDetailPage";
import { INDUSTRY_CONFIG } from "@/components/SectorListPage";

export default function IndustryDetailPage() {
  return <SectorDetailPage config={INDUSTRY_CONFIG} />;
}
