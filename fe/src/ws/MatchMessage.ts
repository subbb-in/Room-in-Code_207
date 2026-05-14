export type MatchStatus = 'DESIGN' | 'PLAYING' | 'END';
export type MatchEventType = 'JOIN' | 'PHASE_CHANGED' | 'CODE_UPDATE' | 'EMOJI' | 'HINT_REQUEST' | 'HINT_RESPONSE' | 'AI_HINT' | 'AI_READY' | 'HP_UPDATE' | 'TIMER';

/** 턴 정보 */
export interface TurnInfo {
    currentUserId: number;
    remainingTime: number;
    seq: number;
}

//방입장(프론트 -> 백)
export interface JoinMatchMessage {
    type: 'JOIN';
    matchId: string;
    userId: number;
}

export interface PhaseChangedMessage {
    type: 'PHASE_CHANGED';
    phase: MatchStatus;
    duration?: number;
    currentTurn?: number | string; // 백엔드 통신용
}

/** 코드 업데이트 (Both: Client send / Server broadcast) */
export interface CodeUpdateMessage {
    type: 'CODE_UPDATE';
    matchId: string;
    userId: number;
    code: string;
}

/** 이모지 전송 (Both: Client send / Server broadcast) */
export interface EmojiMessage {
    type: 'EMOJI';
    matchId: string;
    userId: number;
    emoji: string;
}

/** 힌트 요청 (Front -> Back) */
export interface HintRequestMessage {
    type: 'HINT_REQUEST';
    matchId: string;
    userId: number;
    sequence: number;
}

/** 힌트 결과 (Back -> Front : Only to sender) */
export interface HintResponseMessage {
    type: 'HINT_RESPONSE';
    matchId: string;
    userId: number;
    sequence: number;
    content: string;
    remainingHp: number;
}

/** HP 업데이트 (Back -> Front : Broadcast) */
export interface HpUpdateMessage {
    type: 'HP_UPDATE';
    userId: number;
    remainingHp: number;
}

export interface TimerMessage {
    type: "TIMER";
    matchId: string;
    remainingTime: number;
    currentUserId: number; // store와 맞추기 위해 number로 처리
    seq: number;
}


export type MatchMessage =
    | JoinMatchMessage
    | HintRequestMessage
    | HintResponseMessage
    | EmojiMessage
    | CodeUpdateMessage
    | HpUpdateMessage
    | PhaseChangedMessage
    | TimerMessage;
