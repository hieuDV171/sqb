import { useQuery } from '@tanstack/react-query';
import { getErrorMessage } from '@/lib/errorMessage';
import { deviceService } from '@/services/deviceService';
import { useAuthStore } from '@/stores/useAuthStore';

export function useDevices() {
  const userId = useAuthStore((state) => state.user?.id);
  const currentDeviceId = typeof window !== 'undefined' ? localStorage.getItem('sqb_device_id') : null;

  const devicesQuery = useQuery({
    queryKey: ['my-devices', userId, currentDeviceId],
    staleTime: 0,
    gcTime: 0,
    networkMode: 'always',
    retry: false,
    refetchOnReconnect: false,
    queryFn: async () => {
      const response = await deviceService.getMyDevices();
      const deviceList = Array.isArray(response.data) ? response.data : response.data?.devices ?? [];

      return deviceList.map((device) => ({
        ...device,
        isCurrentDevice: device.isCurrentDevice ?? (currentDeviceId === device.deviceId),
      }));
    },
  });

  const fetchDevices = devicesQuery.refetch;

  const revokeDevice = async (deviceId: string) => {
    try {
      await deviceService.revokeDevice(deviceId);
      await fetchDevices();
    } catch (err) {
      throw new Error(getErrorMessage(err) ?? 'Đăng xuất thiết bị thất bại', { cause: err });
    }
  };

  const deleteDevice = async (deviceId: string) => {
    try {
      await deviceService.deleteDevice(deviceId);
      await fetchDevices();
    } catch (err) {
      throw new Error(getErrorMessage(err) ?? 'Xóa thiết bị thất bại', { cause: err });
    }
  };

  return {
    devices: devicesQuery.data ?? [],
    isLoading: devicesQuery.isFetching,
    error: devicesQuery.isError
      ? getErrorMessage(devicesQuery.error) ?? 'Không thể tải danh sách thiết bị'
      : null,
    fetchDevices,
    revokeDevice,
    deleteDevice,
    currentDeviceId,
  };
}
