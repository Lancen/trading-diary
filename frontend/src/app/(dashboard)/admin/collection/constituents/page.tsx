"use client";

import { useApiQuery, useApiMutation } from "@/lib/hooks";
import type { ConstituentFile, ImportResult } from "@/lib/types";
function Badge({ variant, children }: { variant: string; children: React.ReactNode }) {
  const colors: Record<string, string> = {
    default: "bg-gray-100 text-gray-800",
    outline: "bg-white text-gray-600 border",
  };
  return <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${colors[variant] || colors.outline}`}>{children}</span>;
}

export default function ConstituentsPage() {
  const { data: filesData, isLoading } = useApiQuery<ConstituentFile[]>(
    ["admin", "collection", "constituents"],
    "api/v1/admin/collection/constituents/files"
  );

  const importMutation = useApiMutation<ImportResult, { filename: string }>(
    "post",
    "api/v1/admin/collection/constituents/import",
    [["admin", "collection", "constituents"]]
  );

  const fetchMutation = useApiMutation<void, void>(
    "post",
    "api/v1/admin/collection/constituents/fetch",
    [["admin", "collection", "constituents"]]
  );

  const files = filesData?.data || [];

  return (
    <div className="space-y-6">
      <a href="/admin/collection" className="text-sm text-gray-400 hover:text-blue-600">← 数据采集</a>
      <h1 className="text-2xl font-bold">成分股管理</h1>

      <div className="flex gap-3">
        <button
          onClick={() => fetchMutation.mutate()}
          disabled={fetchMutation.isPending}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50"
        >
          {fetchMutation.isPending ? "获取中..." : "获取最新文件"}
        </button>
      </div>

      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b bg-gray-50 text-xs text-gray-500">
              <th className="px-4 py-2 text-left">文件名</th>
              <th className="px-4 py-2 text-left">获取日期</th>
              <th className="px-4 py-2 text-right">行业关系</th>
              <th className="px-4 py-2 text-right">概念关系</th>
              <th className="px-4 py-2 text-right">总关系</th>
              <th className="px-4 py-2 text-left">状态</th>
              <th className="px-4 py-2 w-20">操作</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">加载中...</td></tr>
            ) : files.length === 0 ? (
              <tr><td colSpan={7} className="px-4 py-8 text-center text-gray-400">无数据</td></tr>
            ) : files.map((f) => (
              <tr key={f.filename} className="border-b hover:bg-gray-50">
                <td className="px-4 py-2 font-mono text-xs">{f.filename}</td>
                <td className="px-4 py-2 text-xs">{f.fetchedDate || "-"}</td>
                <td className="px-4 py-2 text-right">{f.industryCount}</td>
                <td className="px-4 py-2 text-right">{f.conceptCount}</td>
                <td className="px-4 py-2 text-right">{f.totalRelations}</td>
                <td className="px-4 py-2">
                  <Badge variant={f.imported ? "default" : "outline"}>{f.imported ? "已导入" : "未导入"}</Badge>
                </td>
                <td className="px-4 py-2">
                  {!f.imported && (
                    <button
                      onClick={() => importMutation.mutate({ filename: f.filename })}
                      disabled={importMutation.isPending}
                      className="text-xs text-blue-600 hover:underline disabled:opacity-50"
                    >
                      导入
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
