import type { DeviceResponse, DeviceListResponse } from '../types/device.types';
import type { GlobalResponse } from '../types/response.types';

const MOCK_STORAGE_KEY = 'sqb_mock_devices';

function getOrCreateDeviceId() {
  if (typeof window === 'undefined') return 'browser-current-device';
  let deviceId = localStorage.getItem('sqb_device_id');
  if (!deviceId) {
    deviceId = 'web-browser-' + crypto.randomUUID().slice(0, 8);
    localStorage.setItem('sqb_device_id', deviceId);
  }
  return deviceId;
}

function getInitialMockDevices(): DeviceResponse[] {
  const currentId = getOrCreateDeviceId();
  return [
    {
      userDeviceId: 1,
      deviceId: currentId,
      deviceName: 'Chrome trên Windows 11',
      platform: 'WEB',
      osVersion: 'Windows 11 Home 23H2',
      appVersion: 'Chrome 128.0.6613.120',
      lastActiveAt: new Date().toISOString(),
      isActive: true,
      isCurrentDevice: true,
    },
    {
      userDeviceId: 2,
      deviceId: 'iphone-15-pro-mobile-sqb',
      deviceName: 'iPhone 15 Pro (SQB Mobile App)',
      platform: 'IOS',
      osVersion: 'iOS 17.6.1',
      appVersion: 'SQB App v2.4.0',
      lastActiveAt: new Date(Date.now() - 1000 * 60 * 25).toISOString(), // 25 phút trước
      isActive: true,
      isCurrentDevice: false,
    },
    {
      userDeviceId: 3,
      deviceId: 'macbook-m3-safari-hust',
      deviceName: 'MacBook Pro M3 (Thư viện Tạ Quang Bửu)',
      platform: 'WEB',
      osVersion: 'macOS Sonoma 14.6',
      appVersion: 'Safari 17.5',
      lastActiveAt: new Date(Date.now() - 1000 * 60 * 60 * 4).toISOString(), // 4 giờ trước
      isActive: true,
      isCurrentDevice: false,
    },
    {
      userDeviceId: 4,
      deviceId: 'samsung-s24-ultra-app',
      deviceName: 'Samsung Galaxy S24 Ultra',
      platform: 'ANDROID',
      osVersion: 'Android 14 (One UI 6.1)',
      appVersion: 'SQB App v2.3.2',
      lastActiveAt: new Date(Date.now() - 1000 * 60 * 60 * 36).toISOString(), // 1.5 ngày trước
      isActive: false,
      isCurrentDevice: false,
    },
    {
      userDeviceId: 5,
      deviceId: 'pc-lab-d9-hust-firefox',
      deviceName: 'Máy thực hành Lab D9-401 HUST',
      platform: 'DESKTOP',
      osVersion: 'Ubuntu 22.04 LTS',
      appVersion: 'Firefox 129.0',
      lastActiveAt: new Date(Date.now() - 1000 * 60 * 60 * 84).toISOString(), // 3.5 ngày trước
      isActive: false,
      isCurrentDevice: false,
    },
  ];
}

export const deviceService = {
  getMyDevices: async (): Promise<GlobalResponse<DeviceListResponse>> => {
    // Giả lập độ trễ mạng
    await new Promise((resolve) => setTimeout(resolve, 350));
    
    let stored = typeof window !== 'undefined' ? localStorage.getItem(MOCK_STORAGE_KEY) : null;
    let devices: DeviceResponse[];

    if (stored) {
      try {
        devices = JSON.parse(stored);
      } catch {
        devices = getInitialMockDevices();
        localStorage.setItem(MOCK_STORAGE_KEY, JSON.stringify(devices));
      }
    } else {
      devices = getInitialMockDevices();
      if (typeof window !== 'undefined') {
        localStorage.setItem(MOCK_STORAGE_KEY, JSON.stringify(devices));
      }
    }

    return {
      code: '1000',
      message: 'Lấy danh sách thiết bị thành công (Mock Data)',
      data: { devices },
    };
  },

  revokeDevice: async (deviceId: string): Promise<GlobalResponse<void>> => {
    await new Promise((resolve) => setTimeout(resolve, 400));
    if (typeof window !== 'undefined') {
      let stored = localStorage.getItem(MOCK_STORAGE_KEY);
      let devices: DeviceResponse[] = stored ? JSON.parse(stored) : getInitialMockDevices();
      devices = devices.map((d) => (d.deviceId === deviceId ? { ...d, isActive: false } : d));
      localStorage.setItem(MOCK_STORAGE_KEY, JSON.stringify(devices));
    }
    return {
      code: '1000',
      message: 'Đăng xuất thiết bị thành công (Mock Data)',
      data: undefined as any,
    };
  },

  deleteDevice: async (deviceId: string): Promise<GlobalResponse<void>> => {
    await new Promise((resolve) => setTimeout(resolve, 400));
    if (typeof window !== 'undefined') {
      let stored = localStorage.getItem(MOCK_STORAGE_KEY);
      let devices: DeviceResponse[] = stored ? JSON.parse(stored) : getInitialMockDevices();
      devices = devices.filter((d) => d.deviceId !== deviceId);
      localStorage.setItem(MOCK_STORAGE_KEY, JSON.stringify(devices));
    }
    return {
      code: '1000',
      message: 'Xóa thiết bị thành công (Mock Data)',
      data: undefined as any,
    };
  },
};
