export function getAvatarInitial(name?: string | null): string {
  const lastWord = name?.trim().split(/\s+/).at(-1) || 'N';
  const [initial] = lastWord.normalize();
  return initial.toLocaleUpperCase('vi-VN');
}
