import { queryOptions } from '@tanstack/react-query';
import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';

export interface Topic {
  id: number;
  name: string;
  active: boolean;
}

export function topicCatalog(subjectId: number, includeInactive = false) {
  return queryOptions({
    queryKey: ['subject-topics', subjectId, includeInactive] as const,
    enabled: subjectId > 0,
    queryFn: async ({ signal }) => (await axiosClient.get<GlobalResponse<Topic[]>, GlobalResponse<Topic[]>>(
      `/subjects/${subjectId}/topics`, { params: { includeInactive }, signal },
    )).data ?? [],
    staleTime: 60_000,
  });
}

export async function saveTopic(subjectId: number, id: number | undefined, data: { name: string; active?: boolean }) {
  const url = `/subjects/${subjectId}/topics`;
  return id === undefined
    ? axiosClient.post<GlobalResponse<Topic>, GlobalResponse<Topic>>(url, data)
    : axiosClient.put<GlobalResponse<Topic>, GlobalResponse<Topic>>(`${url}/${id}`, data);
}
