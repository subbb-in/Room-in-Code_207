import type { TierType } from "./TierInfo";

export interface NextTierInfo {
  currentTier:      TierType,             // 현재 티어
  currentXP:        number,               // 현재 XP
  nextTier:         TierType | null,      // 다음 티어
  XP2NextTier:      number | null,        // 다음 티어 상한 XP
  progressPercent:  number,               // 현재 진행도
};