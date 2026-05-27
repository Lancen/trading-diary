"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import api from "@/lib/api";

/** API 统一响应结构 */
interface ApiResponse<T> {
  code: number;
  data: T;
}

/**
 * 通用 GET 查询 hook，封装 React Query + ky
 *
 * @param queryKey - React Query 缓存键
 * @param path - API 路径（相对于 prefixUrl）
 * @param searchParams - 可选查询参数
 */
export function useApiQuery<T>(
  queryKey: unknown[],
  path: string,
  searchParams?: Record<string, string | number | boolean | undefined>
) {
  return useQuery<ApiResponse<T>>({
    queryKey,
    queryFn: () =>
      api.get(path, { searchParams: filterParams(searchParams) }).json(),
    // 5 分钟内复用缓存，避免频繁请求
    staleTime: 5 * 60 * 1000,
  });
}

/**
 * 通用 POST 变更 hook，封装 React Query + ky
 *
 * @param path - API 路径
 * @param onSuccess - 成功后回调（通常用于刷新相关查询）
 */
export function useApiMutation<TData, TVariables>(
  method: "post" | "put" | "delete",
  path: string,
  invalidateKeys?: unknown[][]
) {
  const queryClient = useQueryClient();

  return useMutation<ApiResponse<TData>, Error, TVariables>({
    mutationFn: (variables) => {
      if (method === "post") {
        return api.post(path, { json: variables }).json();
      }
      if (method === "put") {
        return api.put(path, { json: variables }).json();
      }
      return api.delete(path, { json: variables }).json();
    },
    onSuccess: () => {
      if (invalidateKeys) {
        invalidateKeys.forEach((key) =>
          queryClient.invalidateQueries({ queryKey: key })
        );
      }
    },
  });
}

/** 过滤掉 undefined 参数（ky 不接受 undefined 值） */
function filterParams(
  params?: Record<string, string | number | boolean | undefined>
): Record<string, string | number | boolean> | undefined {
  if (!params) return undefined;
  const filtered: Record<string, string | number | boolean> = {};
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined) filtered[k] = v;
  }
  return Object.keys(filtered).length > 0 ? filtered : undefined;
}

/** Query Key 工厂，集中管理缓存键避免冲突 */
export const keys = {
  sectors: (type: "concepts" | "industries", params?: object) =>
    ["admin", "market", type, params] as const,
  sectorDetail: (type: "concepts" | "industries", code: string) =>
    ["admin", "market", type, code] as const,
  sectorCorrelations: (type: "concepts" | "industries", code: string) =>
    ["admin", "market", type, code, "correlations"] as const,
  sectorKline: (type: string, code: string, range: string) =>
    ["admin", "sector-index-daily", type, code, range] as const,
  sectorMargin: (type: string, code: string, range: string) =>
    ["admin", "sector-margin", type, code, range] as const,
  stocks: (params?: object) => ["admin", "stocks", params] as const,
  stockDetail: (code: string) => ["admin", "stocks", code] as const,
  rankings: (params?: object) => ["admin", "sector-ranking", params] as const,
  marginStats: (date?: string) => ["admin", "margin-stats", date] as const,
  indexAnalysis: (range: string) => ["admin", "index-analysis", range] as const,
  collection: {
    status: () => ["admin", "collection", "status"] as const,
    calendar: (dataType: string) =>
      ["admin", "collection", "calendar", dataType] as const,
    logs: (dataType: string) =>
      ["admin", "collection", "logs", dataType] as const,
    gapReport: (dataType: string) =>
      ["admin", "collection", "gap", dataType] as const,
  },
};