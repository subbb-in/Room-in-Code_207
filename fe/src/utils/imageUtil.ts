/**
 * 프로필 이미지 URL을 생성합니다
 * @param profileImageUrl - 서버에서 받은 이미지 파일명
 * @returns 완전한 이미지 URL
 */
export const getProfileImageUrl = (profileImageUrl?: string | null): string => {
  if (!profileImageUrl) return '/defaultProfile.png';

  const base = import.meta.env.VITE_IMG_BASE ?? `${window.location.origin}/images/`;
  const normalizedBase = base.endsWith('/') ? base : `${base}/`;
  const normalizedPath = profileImageUrl.startsWith('/') ? profileImageUrl.slice(1) : profileImageUrl;
  return `${normalizedBase}${normalizedPath}`;
};