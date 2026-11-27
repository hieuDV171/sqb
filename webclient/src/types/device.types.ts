import { DevicePlatform } from './auth.types';

export interface DeviceResponse {
  userDeviceId: number;
  deviceId: string;
  deviceName: string;
  platform: DevicePlatform;
  osVersion?: string;
  appVersion?: string;
  lastActiveAt: string;
  isActive: boolean;
  isCurrentDevice: boolean;
}

export interface DeviceListResponse {
  devices: DeviceResponse[];
}
