import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 2, // 2 phút
      gcTime: 1000 * 60 * 10, // 10 phút
      refetchOnWindowFocus: false,
      retry: (failureCount, error: any) => {
        // Không tự động retry với các lỗi client 4xx (400, 401, 403, 404...)
        const status = error?.response?.status || error?.status;
        if (status && status >= 400 && status < 500) {
          return false;
        }
        return failureCount < 1;
      },
    },
  },
});
