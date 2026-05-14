import type { TierInfo } from "./TierInfo";

export const TierTable: TierInfo[] = [
  { tier: '브론즈', minXP: 0, maxXP: 299 },
  { tier: '실버', minXP: 300, maxXP: 599 },
  { tier: '골드', minXP: 600, maxXP: 999 },
  { tier: '플래티넘', minXP: 1000, maxXP: 1499 },
  { tier: '다이아몬드', minXP: 1500, maxXP: 2499 },
  { tier: '마스터', minXP: 2500, maxXP: null }
];