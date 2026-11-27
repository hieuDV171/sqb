import { queryOptions } from '@tanstack/react-query';
import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';

export interface Topic {
  id: number;
  name: string;
  active: boolean;
}

export interface ManagedTopic extends Topic {
  version: number;
}

export const subjectTopicsKey = (subjectId: number) => ['subject-topics', subjectId] as const;

export function topicCatalog(subjectId: number) {
  return queryOptions({
    queryKey: [...subjectTopicsKey(subjectId), 'active'] as const,
    enabled: subjectId > 0,
    queryFn: async ({ signal }) => (await axiosClient.get<GlobalResponse<Topic[]>, GlobalResponse<Topic[]>>(
      `/subjects/${subjectId}/topics`, { signal },
    )).data ?? [],
    staleTime: 60_000,
  });
}

export function managedTopicCatalog(subjectId: number) {
  return queryOptions({
    queryKey: [...subjectTopicsKey(subjectId), 'managed'] as const,
    enabled: subjectId > 0,
    queryFn: async ({ signal }) => (await axiosClient.get<GlobalResponse<ManagedTopic[]>, GlobalResponse<ManagedTopic[]>>(
      `/lecturer/subjects/${subjectId}/topics`, { signal },
    )).data ?? [],
  });
}

type TopicChange =
  | { id?: never; name: string }
  | { id: number; name: string; active?: boolean; version: number };

export async function saveTopic(subjectId: number, { id, ...data }: TopicChange) {
  const url = `/lecturer/subjects/${subjectId}/topics`;
  return id === undefined
    ? axiosClient.post<GlobalResponse<ManagedTopic>, GlobalResponse<ManagedTopic>>(url, data)
    : axiosClient.put<GlobalResponse<ManagedTopic>, GlobalResponse<ManagedTopic>>(`${url}/${id}`, data);
}
