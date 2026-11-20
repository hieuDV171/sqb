export function normalizePassword(password: string) {
  return password.normalize('NFC');
}

export function getPasswordRules(value: string) {
  const password = normalizePassword(value);
  return [
    { id: 'length', label: 'Có ít nhất 12 ký tự', met: [...password].length >= 12 },
    { id: 'lower', label: 'Có chữ thường (a–z, đ, ệ…)', met: /\p{Ll}/u.test(password) },
    { id: 'upper', label: 'Có chữ hoa (A–Z, Đ, Ệ…)', met: /\p{Lu}/u.test(password) },
    { id: 'digit', label: 'Có chữ số (0–9)', met: /[0-9]/.test(password) },
    { id: 'special', label: 'Có ký tự đặc biệt, ví dụ ! @ # $', met: /[\p{P}\p{S}]/u.test(password) },
    { id: 'bytes', label: 'Không vượt quá 72 byte UTF-8', met: new TextEncoder().encode(password).length <= 72 },
    { id: 'visible', label: 'Không chứa ký tự điều khiển hoặc ký tự ẩn', met: /^[\p{L}\p{M}\p{N}\p{P}\p{S}\p{Zs}]*$/u.test(password) },
  ];
}

export function getPasswordError(password: string, currentPassword?: string): string | null {
  const unmet = getPasswordRules(password).filter(rule => !rule.met);
  if (unmet.length) return `Mật khẩu chưa đạt yêu cầu: ${unmet.map(rule => rule.label.toLocaleLowerCase('vi')).join('; ')}.`;
  if (currentPassword !== undefined && normalizePassword(password) === normalizePassword(currentPassword))
    return 'Mật khẩu mới phải khác mật khẩu hiện tại.';
  return null;
}

export function passwordsMatch(password: string, confirmation: string) {
  return normalizePassword(password) === normalizePassword(confirmation);
}
