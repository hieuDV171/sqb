import { axiosClient } from '@/api/axiosClient';
import type { DeviceListResponse } from '../types/device.types';
import type { GlobalResponse } from '../types/response.types';

export const deviceService = {
  getMyDevices: async (): Promise<GlobalResponse<DeviceListResponse>> => {
    return (await axiosClient.get<GlobalResponse<DeviceListResponse>>('/devices')) as any;
  },

  revokeDevice: async (deviceId: string): Promise<GlobalResponse<void>> => {
    return (await axiosClient.post<GlobalResponse<void>>(`/devices/${deviceId}/logout`)) as any;
  },

  deleteDevice: async (deviceId: string): Promise<GlobalResponse<void>> => {
    return (await axiosClient.delete<GlobalResponse<void>>(`/devices/${deviceId}`)) as any;
  },

  unregisterFid: async (fid: string): Promise<GlobalResponse<void>> => {
    return (await axiosClient.post<GlobalResponse<void>>('/devices/fid/unregister', { fid })) as any;
  },

  syncFid: async (fid: string): Promise<GlobalResponse<void>> => {
    return (await axiosClient.post<GlobalResponse<void>>('/devices/fid', { fid })) as any;
  },
};
