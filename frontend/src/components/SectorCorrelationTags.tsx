"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import api from "@/lib/api";

interface CorrelationItem {
  relatedType: string;
  relatedCode: string;
  relatedName: string;
  jaccard: number;
  intersectionCount: number;
  sourceCount: number;
  targetCount: number;
}

interface SectorCorrelationTagsProps {
  sectorType: "INDUSTRY" | "CONCEPT";
  sectorCode: string;
}

function tagStyle(jaccard: number): string {
  if (jaccard >= 0.5) return "bg-blue-100 text-blue-800 border-blue-300 hover:bg-blue-200";
  if (jaccard >= 0.2) return "bg-blue-50 text-blue-600 border-blue-200 hover:bg-blue-100";
  return "bg-gray-50 text-gray-600 border-gray-200 hover:bg-gray-100";
}

export default function SectorCorrelationTags({ sectorType, sectorCode }: SectorCorrelationTagsProps) {
  const router = useRouter();
  const [items, setItems] = useState<CorrelationItem[]>([]);
  const [expanded, setExpanded] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    api.get("api/v1/admin/sector-correlation", {
      searchParams: { sectorType, sectorCode },
    })
      .json<{ code: number; data: CorrelationItem[] }>()
      .then((res) => {
        setItems(res.data || []);
      })
      .catch(() => setItems([]))
      .finally(() => setLoading(false));
  }, [sectorType, sectorCode]);

  if (loading) return <span className="text-xs text-gray-400 ml-2">关联度加载中...</span>;
  if (items.length === 0) return null;

  const visible = expanded ? items : items.slice(0, 5);
  const targetPath = items[0]?.relatedType === "INDUSTRY" ? "/admin/industries" : "/admin/concepts";

  return (
    <span className="inline-flex flex-wrap gap-1.5 ml-3 items-center">
      {visible.map((item) => (
        <span key={item.relatedCode} className="relative group">
          <button
            onClick={() => router.push(`${targetPath}/${item.relatedCode}?name=${encodeURIComponent(item.relatedName)}`)}
            className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium transition-colors cursor-pointer ${tagStyle(item.jaccard)}`}
          >
            {item.relatedName} {(item.jaccard * 100).toFixed(0)}%
          </button>
          <span className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block z-10 w-48 rounded bg-gray-900 px-3 py-2 text-xs text-white shadow-lg whitespace-nowrap">
            重叠 {item.intersectionCount} 只 | {item.sourceCount} / {item.targetCount}
          </span>
        </span>
      ))}
      {items.length > 5 && !expanded && (
        <button onClick={() => setExpanded(true)} className="text-xs text-blue-600 hover:underline">
          更多({items.length - 5})
        </button>
      )}
      {expanded && items.length > 5 && (
        <button onClick={() => setExpanded(false)} className="text-xs text-blue-600 hover:underline">
          收起
        </button>
      )}
    </span>
  );
}
