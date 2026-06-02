import { useQuery, useMutation, useQueryClient, UseQueryOptions } from "@tanstack/react-query";
import api from "@/lib/api";
import type { ApiResponse } from "@/lib/types";

export const keys = {
  crowdedness: (startDate: string) => ["crowdedness", startDate] as const,
  rankings: (params: Record<string, string>) => ["rankings", params] as const,
  stocks: (params: Record<string, string | number>) => ["stocks", params] as const,
  stockDetail: (code: string, params: Record<string, string>) => ["stockDetail", code, params] as const,
  marginStats: (params: Record<string, string>) => ["marginStats", params] as const,
  marketIndexLatest: () => ["marketIndexLatest"] as const,
  marketIndexDaily: (params: Record<string, string>) => ["marketIndexDaily", params] as const,
  marginMacroSse: (params: Record<string, string>) => ["marginMacroSse", params] as const,
  sectors: (sectorType: string, params: Record<string, string | number>) => ["sectors", sectorType, params] as const,
  sectorIndexDaily: (params: Record<string, string>) => ["sectorIndexDaily", params] as const,
  sectorMargin: (params: Record<string, string>) => ["sectorMargin", params] as const,
  sectorStocks: (sectorType: string, code: string) => ["sectorStocks", sectorType, code] as const,
  collectionStatus: () => ["collectionStatus"] as const,
  cookieStatus: () => ["cookieStatus"] as const,
  collectionDataType: (dataType: string) => ["collectionDataType", dataType] as const,
  collectionCalendar: (dataType: string) => ["collectionCalendar", dataType] as const,
  constituentsFiles: () => ["constituentsFiles"] as const,
};

export function useApiQuery<T>(
  queryKey: ReturnType<typeof keys[keyof typeof keys]>,
  url: string,
  params?: Record<string, string | number | undefined>,
  options?: Omit<UseQueryOptions<T>, "queryKey" | "queryFn">,
) {
  const filteredParams: Record<string, string | number> = {};
  if (params) {
    for (const [k, v] of Object.entries(params)) {
      if (v !== undefined) filteredParams[k] = v;
    }
  }

  return useQuery<T>({
    queryKey,
    queryFn: async () => {
      const res = await api.get(url, { searchParams: filteredParams }).json<ApiResponse<T>>();
      return res.data;
    },
    ...options,
  });
}

export function useApiMutation<TData, TVariables>(
  method: "post" | "put" | "patch" | "delete",
  url: string | ((vars: TVariables) => string),
) {
  const queryClient = useQueryClient();

  return useMutation<TData, Error, TVariables>({
    mutationFn: async (variables) => {
      const targetUrl = typeof url === "function" ? url(variables) : url;
      if (method === "post") {
        const res = await api.post(targetUrl, { json: variables }).json<ApiResponse<TData>>();
        return res.data;
      }
      if (method === "put") {
        const res = await api.put(targetUrl, { json: variables }).json<ApiResponse<TData>>();
        return res.data;
      }
      if (method === "patch") {
        const res = await api.patch(targetUrl, { json: variables }).json<ApiResponse<TData>>();
        return res.data;
      }
      const res = await api.delete(targetUrl).json<ApiResponse<TData>>();
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries();
    },
  });
}
