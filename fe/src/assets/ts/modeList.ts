import { Robot, Trophy, UserFriends } from "@vicons/fa";

export const modeList = [
  {
    name:       'AI',
    title:      '연습',
    desc:       'AI와 대결하여 실력을 겨뤄보세요!',
    routeName:  'ai',
    btnName:    '게임 시작',
    component:  Robot,
  },
  {
    name:       'FRIENDS',
    title:      '실전',
    desc:       '친구와 대결하여 실력을 겨뤄보세요!',
    routeName:  'friends',
    btnName:    '게임 시작',
    component:  UserFriends,
  },
  {
    name:       'CONTEST',
    title:      '랭킹전',
    desc:       '나와 비슷한 실력자와 겨뤄보세요!',
    routeName:  'contest',
    btnName:    '게임 시작',
    component:  Trophy,
  }
];