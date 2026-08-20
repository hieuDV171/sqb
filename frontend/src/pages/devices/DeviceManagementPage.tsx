import { useState } from 'react';
import { useDevices } from '@/hooks/useDevices';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@base-ui/react/button';
import {
  Monitor,
  Smartphone,
  LogOut,
  Trash2,
  RefreshCw,
  ShieldCheck,
  Clock,
  AlertTriangle,
  CheckCircle2,
  Sparkles
} from 'lucide-react';

export function DeviceManagementPage() {
  const { devices, isLoading, error, fetchDevices, revokeDevice, deleteDevice, currentDeviceId } = useDevices();

  const [actionDeviceId, setActionDeviceId] = useState<string | null>(null);
  const [actionType, setActionType] = useState<'revoke' | 'delete' | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [toastMessage, setToastMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const showToast = (type: 'success' | 'error', text: string) => {
    setToastMessage({ type, text });
    setTimeout(() => setToastMessage(null), 3000);
  };

  const handleConfirmAction = async () => {
    if (!actionDeviceId || !actionType) return;
    setIsSubmitting(true);
    try {
      if (actionType === 'revoke') {
        await revokeDevice(actionDeviceId);
        showToast('success', 'Đã đăng xuất phiên làm việc của thiết bị thành công');
      } else if (actionType === 'delete') {
        await deleteDevice(actionDeviceId);
        showToast('success', 'Đã xóa thiết bị khỏi danh sách của bạn thành công');
      }
    } catch (err: any) {
      showToast('error', err?.message || 'Thao tác thất bại');
    } finally {
      setIsSubmitting(false);
      setActionDeviceId(null);
      setActionType(null);
    }
  };

  const getPlatformIcon = (platform?: string) => {
    switch (platform?.toUpperCase()) {
      case 'IOS':
      case 'ANDROID':
        return <Smartphone className="w-6 h-6 text-indigo-600" />;
      case 'DESKTOP':
        return <Monitor className="w-6 h-6 text-blue-600" />;
      case 'WEB':
      default:
        return <Monitor className="w-6 h-6 text-purple-600" />;
    }
  };

  const targetDevice = devices.find((d) => d.deviceId === actionDeviceId);

  return (
    <div className="space-y-6">
      {/* Toast Notification */}
      {toastMessage && (
        <div
          className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-4 py-3 rounded-xl shadow-lg text-sm font-medium animate-in fade-in slide-in-from-bottom-4 duration-200 ${
            toastMessage.type === 'success'
              ? 'bg-slate-900 text-white border border-slate-700'
              : 'bg-red-600 text-white'
          }`}
        >
          {toastMessage.type === 'success' ? (
            <CheckCircle2 className="w-5 h-5 text-green-400" />
          ) : (
            <AlertTriangle className="w-5 h-5 text-red-200" />
          )}
          <span>{toastMessage.text}</span>
        </div>
      )}

      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl font-bold text-slate-900">Quản lý Thiết bị</h1>
            <span className="bg-indigo-50 text-indigo-700 text-xs font-semibold px-2.5 py-1 rounded-full border border-indigo-100 flex items-center gap-1">
              <ShieldCheck className="w-3.5 h-3.5" /> Security Center
            </span>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Danh sách các trình duyệt và ứng dụng di động đang duy trì đăng nhập vào tài khoản của bạn.
          </p>
        </div>

        <Button
          onClick={() => fetchDevices()}
          disabled={isLoading}
          className="flex items-center justify-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium text-sm rounded-xl transition-colors cursor-pointer disabled:opacity-50"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          Làm mới
        </Button>
      </div>

      {/* Error Message */}
      {error && (
        <div className="flex items-center gap-3 p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm">
          <AlertTriangle className="w-5 h-5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Loading Skeleton */}
      {isLoading && devices.length === 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-44 bg-slate-200/60 rounded-2xl animate-pulse" />
          ))}
        </div>
      )}

      {/* Empty State */}
      {!isLoading && devices.length === 0 && !error && (
        <Card className="p-12 text-center bg-white border-dashed border-2 border-slate-200">
          <CardContent className="space-y-3">
            <div className="w-12 h-12 rounded-full bg-slate-100 flex items-center justify-center mx-auto text-slate-400">
              <Monitor className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-semibold text-slate-800">Chưa ghi nhận thiết bị nào</h3>
            <p className="text-sm text-slate-500 max-w-md mx-auto">
              Không tìm thấy thông tin thiết bị đang lưu vết trong tài khoản của bạn.
            </p>
          </CardContent>
        </Card>
      )}

      {/* Device List Grid */}
      {devices.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {devices.map((device) => {
            const isCurrent = device.isCurrentDevice || device.deviceId === currentDeviceId;

            return (
              <Card
                key={device.id || device.deviceId}
                className={`relative bg-white border transition-all duration-200 rounded-2xl overflow-hidden hover:shadow-md ${
                  isCurrent ? 'border-indigo-500/80 ring-2 ring-indigo-500/20' : 'border-slate-200/80'
                }`}
              >
                <CardContent className="p-5 flex flex-col justify-between h-full space-y-4">
                  {/* Top Badge & Header */}
                  <div>
                    <div className="flex items-start justify-between gap-2 mb-3">
                      <div className="p-2.5 rounded-xl bg-slate-100 flex items-center justify-center">
                        {getPlatformIcon(device.platform)}
                      </div>
                      {isCurrent ? (
                        <span className="inline-flex items-center gap-1 bg-indigo-600 text-white text-xs font-semibold px-2.5 py-1 rounded-full shadow-xs">
                          <Sparkles className="w-3 h-3" /> Thiết bị này
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 bg-slate-100 text-slate-600 text-xs font-medium px-2 py-0.5 rounded-full">
                          <Clock className="w-3 h-3" />
                          {device.isActive ? 'Đang hoạt động' : 'Phiên lưu'}
                        </span>
                      )}
                    </div>

                    <h3 className="font-bold text-slate-900 text-base line-clamp-1">
                      {device.deviceName || 'Trình duyệt Web'}
                    </h3>
                    <p className="text-xs font-mono text-slate-500 mt-0.5 truncate">
                      ID: {device.deviceId}
                    </p>
                  </div>

                  {/* Device Specs */}
                  <div className="space-y-1.5 text-xs text-slate-600 bg-slate-50 p-3 rounded-xl border border-slate-100">
                    <div className="flex justify-between">
                      <span className="text-slate-400">Nền tảng:</span>
                      <span className="font-medium text-slate-800">{device.platform}</span>
                    </div>
                    {device.osVersion && (
                      <div className="flex justify-between">
                        <span className="text-slate-400">Hệ điều hành:</span>
                        <span className="font-medium text-slate-800">{device.osVersion}</span>
                      </div>
                    )}
                    {device.lastActiveAt && (
                      <div className="flex justify-between">
                        <span className="text-slate-400">Hoạt động gần nhất:</span>
                        <span className="font-medium text-slate-800">
                          {new Date(device.lastActiveAt).toLocaleString('vi-VN')}
                        </span>
                      </div>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2 pt-1 border-t border-slate-100">
                    <Button
                      onClick={() => {
                        setActionDeviceId(device.deviceId);
                        setActionType('revoke');
                      }}
                      className="flex-1 flex items-center justify-center gap-1.5 px-3 py-2 bg-slate-100 hover:bg-red-50 text-slate-700 hover:text-red-600 text-xs font-medium rounded-xl transition-colors cursor-pointer"
                    >
                      <LogOut className="w-3.5 h-3.5" />
                      Đăng xuất
                    </Button>

                    <Button
                      onClick={() => {
                        setActionDeviceId(device.deviceId);
                        setActionType('delete');
                      }}
                      className="flex items-center justify-center p-2 bg-slate-100 hover:bg-red-100 text-slate-500 hover:text-red-700 text-xs font-medium rounded-xl transition-colors cursor-pointer"
                      title="Xóa khỏi danh sách thiết bị"
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Confirmation Modal for Revoke / Delete */}
      {actionType && targetDevice && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl space-y-4 animate-in fade-in zoom-in-95 duration-200">
            <div className="flex items-center gap-3">
              <div
                className={`p-3 rounded-full ${
                  actionType === 'delete' ? 'bg-red-100 text-red-600' : 'bg-amber-100 text-amber-600'
                }`}
              >
                {actionType === 'delete' ? <Trash2 className="w-6 h-6" /> : <LogOut className="w-6 h-6" />}
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-900">
                  {actionType === 'delete' ? 'Xóa thiết bị khỏi danh sách?' : 'Đăng xuất thiết bị từ xa?'}
                </h3>
                <p className="text-xs text-slate-500">{targetDevice.deviceName}</p>
              </div>
            </div>

            <p className="text-sm text-slate-600">
              {actionType === 'delete'
                ? `Bạn có chắc chắn muốn XÓA thiết bị (ID: ${targetDevice.deviceId}) khỏi danh sách thiết bị của bạn không? Hành động này sẽ gọi API DELETE /devices/${targetDevice.deviceId}.`
                : `Hành động này sẽ hủy phiên làm việc hiện tại trên thiết bị "${targetDevice.deviceName}". Người dùng trên thiết bị đó sẽ bị yêu cầu đăng nhập lại.`}
            </p>

            <div className="flex justify-end gap-3 pt-2">
              <Button
                onClick={() => {
                  setActionType(null);
                  setActionDeviceId(null);
                }}
                disabled={isSubmitting}
                className="px-4 py-2 border rounded-xl text-sm font-medium text-slate-700 hover:bg-slate-100 cursor-pointer"
              >
                Hủy
              </Button>
              <Button
                onClick={handleConfirmAction}
                disabled={isSubmitting}
                className={`px-4 py-2 text-white rounded-xl text-sm font-medium transition-colors cursor-pointer ${
                  actionType === 'delete' ? 'bg-red-600 hover:bg-red-700' : 'bg-amber-600 hover:bg-amber-700'
                }`}
              >
                {isSubmitting
                  ? 'Đang xử lý...'
                  : actionType === 'delete'
                  ? 'Xác nhận xóa'
                  : 'Xác nhận đăng xuất'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
