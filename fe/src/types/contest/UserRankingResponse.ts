export interface UserRankingResponse {
  rank:             number,
  userId:           number,     // 사용자 식별번호
  nickname:         string,     // 닉네임
  introText:        string,     // 자기소개
  xp:               number,     // 누적 XP
  profileImageUrl:  string,     // 프로필 이미지 주소
};