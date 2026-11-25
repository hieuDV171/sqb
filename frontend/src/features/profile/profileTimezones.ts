export const DEFAULT_PROFILE_TIMEZONE = 'Asia/Ho_Chi_Minh';
export const LEGACY_VIETNAM_TIMEZONE = 'Asia/Saigon';

export function normalizeProfileTimezone(zone: string | null | undefined) {
  const value = zone?.trim();
  return !value || value === LEGACY_VIETNAM_TIMEZONE ? DEFAULT_PROFILE_TIMEZONE : value;
}
