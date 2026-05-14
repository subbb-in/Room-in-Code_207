<template>
  <div class="flex justify-center items-center">
    <div class="w-full max-w-6xl border border-slate-700 rounded-xl my-8 overflow-hidden">
      <n-data-table
        :columns="columns"
        :data="ranks"
        :bordered="false"
        :row-class-name="rowClassName"
        :pagination="false"
        size="large"
        class="bg-slate-800"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { getTierColorFromTierType, getTierIconComponentFromTierType, getTierByTotalXP } from '@/utils/game/TierUtil';
import { useContestStore } from '@/stores/contestStore';
import { storeToRefs } from 'pinia';
import { NDataTable, NIcon } from 'naive-ui';
import { useUserStore } from '@/stores/userStore';
import { h } from 'vue';
import type { DataTableColumns } from 'naive-ui';
import type { UserRankingResponse } from '@/types/contest/UserRankingResponse';

const userStore = useUserStore()
const contestStore = useContestStore();
const { rankingData: ranks } = storeToRefs(contestStore);

const columns: DataTableColumns<UserRankingResponse> = [
  {
    title: '순위',
    key: 'rank',
    width: 100,
    render: (row) => h('span', { class: 'text-xl font-bold text-slate-300' }, row.rank)
  },
  {
    title: '프로필',
    key: 'profile',
    render: (row) => h('div', { class: 'flex items-center gap-4 py-2' }, [
      h('img', {
        src: row.profileImageUrl || '/defaultProfile.png',
        class: 'rounded-full size-14 border-2 border-primary'
      }),
      h('div', { class: 'flex flex-col gap-1' }, [
        h('span', { class: 'text-lg text-white font-bold' }, row.nickname),
        h('span', { class: 'text-sm text-cyan-400' }, row.introText || '')
      ])
    ])
  },
  {
    title: '티어',
    key: 'tier',
    width: 100,
    render: (row) => h(NIcon, {
      size: 28,
      color: getTierColorFromTierType(getTierByTotalXP(row.xp))
    }, {
      default: () => h(getTierIconComponentFromTierType(getTierByTotalXP(row.xp)))
    })
  },
  {
    title: 'XP',
    key: 'xp',
    width: 140,
    render: (row) => h('span', { class: 'text-lg font-bold text-slate-200' }, row.xp)
  }
];

const rowClassName = (row: UserRankingResponse) => {
  return row.userId === userStore.user?.userId
    ? 'my-user-row'
    : '';
};


</script>

<style scoped>
:deep(.n-data-table-th) {
  background-color: rgb(51 65 85);
  color: rgb(148 163 184);
  font-weight: 700;
  font-size: 1rem;
  padding: 12px 16px;
}

:deep(.n-data-table-td) {
  background-color: rgb(30 41 59);
  padding: 16px;
}

:deep(.n-data-table-tr:hover) .n-data-table-td {
  background-color: rgba(148, 163, 184, 0.07);
}

/* 자신의 순위 행 하이라이트 */
:deep(.my-user-row) .n-data-table-td {
  background-color: rgba(59, 130, 246, 0.15);
}

:deep(.my-user-row:hover) .n-data-table-td {
  background-color: rgba(59, 130, 246, 0.25);
}
</style>