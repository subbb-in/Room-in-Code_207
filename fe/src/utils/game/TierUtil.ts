import type { NextTierInfo } from "@/types/game/tier/NextTierInfo";
import type { TierInfo, TierType } from "@/types/game/tier/TierInfo";
import { TierTable } from "@/types/game/tier/TierTable";
import { QuestionCircle } from "@vicons/fa";
import { Badge, Crown, Diamond, Medal } from "@vicons/tabler";
import type { Component } from "vue";

/**
 * 누적 XP를 기반으로 현재 티어를 취득하는 함수
 * @param totalXP 누적 XP
 * @returns {TierType}
 */
export const getTierByTotalXP = (totalXP: number): TierType => {
  const found = TierTable.find(t => totalXP >= t.minXP && (t.maxXP === null || totalXP <= t.maxXP));
  return found ? found.tier : '브론즈';
};

/**
 * 누적 XP를 기반으로 티어 테이블 정보를 취득하는 함수
 * @param totalXP 누적 XP
 * @returns {TierInfo}
 */
export const getTierInfoByTotalXP = (totalXP: number): TierInfo => {
  const currentIdx = TierTable.findIndex(t => totalXP >= t.minXP && (t.maxXP === null || totalXP <= t.maxXP));
  if (currentIdx === -1) {
    return {
      tier: '알 수 없음',
      minXP: 0,
      maxXP: null
    };
  }

  return TierTable[currentIdx]!;
};

/**
 * 누적 XP를 기반으로 다음 티어 승급을 위한 정보를 취득하는 함수
 * @param totalXP 누적 XP
 * @returns {NextTierInfo}
 */
export const getNextTierFromTotalXP = (totalXP: number): NextTierInfo => {
  const currentIdx = TierTable.findIndex(t => totalXP >= t.minXP && (t.maxXP === null || totalXP <= t.maxXP));
  if (currentIdx === -1) {
    return {
      currentTier:      '알 수 없음',
      currentXP:        totalXP,
      nextTier:         '알 수 없음',
      XP2NextTier:      null,
      progressPercent:  0,
    };
  }

  const currentTierInfo = TierTable[currentIdx];
  const nextTierInfo    = TierTable[currentIdx + 1] ?? null;

  // 마스터 티어
  if (!nextTierInfo || !currentTierInfo || currentTierInfo?.maxXP === null) {
    return {
      currentTier:      currentTierInfo?.tier!,
      currentXP:        totalXP,
      nextTier:         null,
      XP2NextTier:      null,
      progressPercent:  100
    };
  }

  const xpIntoCurrentTier = totalXP - currentTierInfo.minXP;
  const currentTierRange  = currentTierInfo.maxXP - currentTierInfo.minXP + 1;

  return {
    currentTier:      currentTierInfo.tier,
    currentXP:        totalXP,
    nextTier:         nextTierInfo.tier,
    XP2NextTier:      nextTierInfo.minXP - totalXP,
    progressPercent:  Math.floor((xpIntoCurrentTier / currentTierRange) * 100)
  };
};

/**
 * 티어 유형에 따른 티어 등급 컴포넌트를 취득하는 함수 (아이콘 출력용)
 * @param tierType 티어 유형
 * @returns {Component}
 */
export const getTierIconComponentFromTierType = (tierType: TierType): Component => {
  switch (tierType) {
    case "브론즈": case "실버": case "골드": return Medal;
    case "플래티넘": return Badge;
    case "다이아몬드": return Diamond;
    case "마스터": return Crown;
    case "알 수 없음": return QuestionCircle;
  }
};

/**
 * 티어 유형에 따른 티어 등급 색상 값을 취득하는 함수
 * @param tierType 티어 유형
 * @returns {string}
 */
export const getTierColorFromTierType = (tierType: TierType): string => {
  switch (tierType) {
    case "브론즈": return '#CD7F32';
    case "실버": return '#C0C0C0';
    case "골드": return '#FFD700';
    case "플래티넘": return '#4FD1C5';
    case "다이아몬드": return '#3B82F6';
    case "마스터": return '#8B5CF6';
    case "알 수 없음": return "#9CA3AF";
  }
};