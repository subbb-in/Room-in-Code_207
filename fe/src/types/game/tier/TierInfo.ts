export type TierType = '브론즈' | '실버' | '골드' | '플래티넘' | '다이아몬드' | '마스터' | '알 수 없음';

export interface TierInfo {
  tier:   TierType,
  minXP:  number,
  maxXP:  number | null
};