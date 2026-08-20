import { useState, useCallback, useEffect } from 'react';
import { deviceService } from '@/services/deviceService';
import type { DeviceResponse } from '@/types/device.types';

export function useDevices() {
  const [devices, setDevices] = useState<DeviceResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const currentDeviceId = typeof window !== 'undefined' ? localStorage.getItem('sqb_device_id') : null;

  const fetchDevices = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await deviceService.getMyDevices();
      const deviceList = response.data?.devices || (Array.isArray(response.data) ? response.data : []);
      
      // Gán thêm cờ isCurrentDevice nếu trùng với sqb_device_id trong localStorage
      const enrichedDevices = deviceList.map((device) => ({
        ...device,
        isCurrentDevice: device.isCurrentDevice ?? (currentDeviceId === device.deviceId),
      }));

      setDevices(enrichedDevices);
    } catch (err: any) {
      setError(err?.message || 'Không thể tải danh sách thiết bị');
    } finally {
      setIsLoading(false);
    }
  }, [currentDeviceId]);

  const revokeDevice = async (deviceId: string) => {
    try {
      await deviceService.revokeDevice(deviceId);
      await fetchDevices();
    } catch (err: any) {
      throw new Error(err?.message || 'Đăng xuất thiết bị thất bại');
    }
  };

  const deleteDevice = async (deviceId: string) => {
    try {
      await deviceService.deleteDevice(deviceId);
      await fetchDevices();
    } catch (err: any) {
      throw new Error(err?.message || 'Xóa thiết bị thất bại');
    }
  };

  useEffect(() => {
    fetchDevices();
  }, [fetchDevices]);

  return {
    devices,
    isLoading,
    error,
    fetchDevices,
    revokeDevice,
    deleteDevice,
    currentDeviceId,
  };
}
