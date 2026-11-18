export const ADMIN_HOSTNAME = 'admin.hieudv.id.vn';
export const ADMIN_ORIGIN = `https://${ADMIN_HOSTNAME}`;

const PUBLIC_HOSTNAMES = new Set(['hieudv.id.vn', 'www.hieudv.id.vn', 'snsqb.duckdns.org']);

export const ACCESS_REAUTH_MESSAGE = 'Phiên truy cập quản trị đã hết hạn hoặc bị từ chối. Hãy tải lại trang để xác thực với Cloudflare Access.';

export class AccessGatewayError extends Error {
  override name = 'AccessGatewayError';
}

export function isAdminHostname(hostname: string): boolean {
  return hostname === ADMIN_HOSTNAME;
}

export function resolveApiBaseUrl(location: Pick<Location, 'hostname' | 'origin'>, configuredUrl?: string): string {
  // One artifact serves both sites. Never send an admin session to the public API origin.
  if (isAdminHostname(location.hostname) || PUBLIC_HOSTNAMES.has(location.hostname)) {
    return `${location.origin}/api/v1`;
  }
  return new URL(configuredUrl || 'http://localhost:8080/api/v1', location.origin).href.replace(/\/$/, '');
}

export function getAdminPageRedirect(hostname: string, pathname: string, search = '', hash = ''): string | null {
  if (!PUBLIC_HOSTNAMES.has(hostname) || !/^\/admin(?:\/|$)/i.test(pathname)) return null;
  return `${ADMIN_ORIGIN}${pathname}${search}${hash}`;
}

export function isAccessGatewayResponse(status: number | undefined, data: unknown): boolean {
  if (status !== 401 && status !== 403) return false;
  // Spring errors have an envelope. Edge denials must not trigger SQB token refresh/logout.
  return !(typeof data === 'object' && data !== null && 'code' in data && 'message' in data);
}
