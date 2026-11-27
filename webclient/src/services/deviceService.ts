import { axiosClient } from '@/api/axiosClient';
import type { DeviceListResponse, DeviceResponse } from '../types/device.types';
import type { GlobalResponse } from '../types/response.types';

export const deviceService = {
  getMyDevices: async (): Promise<GlobalResponse<DeviceListResponse | DeviceResponse[]>> => {
    return axiosClient.get<unknown, GlobalResponse<DeviceListResponse | DeviceResponse[]>>('/devices');
  },

  revokeDevice: async (deviceId: string): Promise<GlobalResponse<void>> => {
    return axiosClient.post<unknown, GlobalResponse<void>>(`/devices/${deviceId}/logout`);
  },

  deleteDevice: async (deviceId: string): Promise<GlobalResponse<void>> => {
    return axiosClient.delete<unknown, GlobalResponse<void>>(`/devices/${deviceId}`);
  },

  unregisterFid: async (fid: string): Promise<GlobalResponse<void>> => {
    return axiosClient.post<unknown, GlobalResponse<void>>('/devices/fid/unregister', { fid });
  },

  syncFid: async (fid: string): Promise<GlobalResponse<void>> => {
    return axiosClient.post<unknown, GlobalResponse<void>>('/devices/fid', { fid });
  },
};
