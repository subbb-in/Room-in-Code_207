export interface SkillStat {
  skillName: string;  // 기술 이름
  solvedCount: number; // 해결된 횟수
}

export interface UserProfileResponse {
  userId: number;          // 사용자 식별번호
  nickname: string;        // 닉네임
  profileImageUrl: string; // 프로필 이미지 주소
  introText: string;       // 자기소개
  totalXp: number;         // 누적 XP
  email: string;           // 이메일 주소
  joinedAt: string;        // 가입일자
  skillStats: SkillStat[]; // 기술 통계 배열
}