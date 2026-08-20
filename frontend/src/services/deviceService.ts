import { axiosClient } from '../api/axiosClient';
import type { DeviceListResponse } from '../types/device.types';
import type { GlobalResponse } from '../types/response.types';

export const deviceService = {
  getMyDevices: async (): Promise<GlobalResponse<DeviceListResponse>> => {
    return axiosClient.get(`/devices`);
  },

  revokeDevice: async (deviceId: string): Promise<GlobalResponse<void>> => {
    return axiosClient.post(`/devices/${deviceId}/logout`);
  },

  deleteDevice: async (deviceId: string): Promise<GlobalResponse<void>> => {
    return axiosClient.delete(`/devices/${deviceId}`);
  },
};
