"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import api from "@/lib/api";
import { useToast } from "@/hooks/use-toast";

interface ConstituentFile {
  filename: string;
  fetchedDate: string | null;
  industryCount: number;
  conceptCount: number;
  totalRelations: number;
  imported: boolean;
}

interface ImportResult {
  industryRelations: number;
  conceptRelations: number;
}

export default function ConstituentsPage() {
  const [files, setFiles] = useState<ConstituentFile[]>([]);
  const [loading, setLoading] = useState(true);
  const [importing, setImporting] = useState<string | null>(null);
  const toast = useToast((s) => s.toast);

  const fetchFiles = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get("api/v1/admin/collection/constituents/files").json<{ code: number; data: ConstituentFile[] }>();
      setFiles(res.data || []);
    } catch (e) { console.error(e); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchFiles(); }, [fetchFiles]);

  async function handleImport(filename: string) {
    setImporting(filename);
    try {
      const res = await api.post("api/v1/admin/collection/constituents/import", {
        json: { filename },
      }).json<{ code: number; data?: ImportResult; msg?: string }>();
      if (res.code === 200) {
        toast(`导入成功: ${res.data?.industryRelations || 0} 行业 + ${res.data?.conceptRelations || 0} 概念`, "success");
        fetchFiles();
      } else {
        toast(`导入失败: ${res.msg || "未知错误"}`, "error");
      }
    } catch (e) { toast("导入请求失败", "error"); }
    finally { setImporting(null); }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-2 text-sm text-gray-500">
        <Link href="/admin/collection" className="hover:underline">← 返回数据采集</Link>
        <span className="text-gray-300">|</span>
        <h1 className="text-2xl font-bold text-gray-900">成分股管理</h1>
      </div>

      <p className="text-sm text-gray-500">数据来源: Playwright 抓取同花顺，存放在 data/constituents/ 目录</p>

      <div className="flex justify-end">
        <button onClick={fetchFiles} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:opacity-90">刷新</button>
      </div>

      {loading ? (
        <div className="py-8 text-center text-gray-500">加载中...</div>
      ) : files.length === 0 ? (
        <div className="py-8 text-center text-gray-400">暂无成分股数据文件</div>
      ) : (
        <div className="overflow-hidden rounded-lg border">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-gray-50 text-left text-xs text-gray-500">
                <th className="px-4 py-3">文件名</th>
                <th className="px-4 py-3">采集日期</th>
                <th className="px-4 py-3 text-center">行业数</th>
                <th className="px-4 py-3 text-center">概念数</th>
                <th className="px-4 py-3 text-center">关系总数</th>
                <th className="px-4 py-3 text-center">状态</th>
                <th className="px-4 py-3 text-center">操作</th>
              </tr>
            </thead>
            <tbody>
              {files.map((f) => (
                <tr key={f.filename} className="border-b last:border-0">
                  <td className="px-4 py-3 font-mono text-xs">{f.filename}</td>
                  <td className="px-4 py-3">{f.fetchedDate || "-"}</td>
                  <td className="px-4 py-3 text-center">{f.industryCount ?? "-"}</td>
                  <td className="px-4 py-3 text-center">{f.conceptCount ?? "-"}</td>
                  <td className="px-4 py-3 text-center">{f.totalRelations ?? "-"}</td>
                  <td className="px-4 py-3 text-center">
                    {f.imported
                      ? <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-700">已导入</span>
                      : <span className="rounded-full bg-yellow-100 px-2 py-0.5 text-xs text-yellow-800">待导入</span>}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button
                      onClick={() => handleImport(f.filename)}
                      disabled={f.imported || importing === f.filename}
                      className="rounded bg-blue-600 px-3 py-1 text-xs font-medium text-white hover:opacity-90 disabled:opacity-40"
                    >
                      {f.imported ? "已导入" : importing === f.filename ? "导入中..." : "导入"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
