<template>
  <n-form v-bind:model="profileEditFormValue" v-bind:rules="profileEditFormRules" ref="profileEditForm">
    <n-form-item label="프로필 이미지" path="profile.image">
      <div class="flex flex-col gap-4">
        <img v-bind:src="previewProfileImage || '/defaultProfile.png'" alt="PROFILE" class="rounded-full size-40">
        
        <n-upload list-type="image" accept="image/jpeg" @update:file-list="handleProfileImageUpload">
          <n-button type="primary">이미지 파일 업로드</n-button>
        </n-upload>
      </div>
    </n-form-item>

    <n-form-item label="닉네임" path="profile.nickname">
      <n-input v-model:value="profileEditFormValue.profile.nickname" placeholder="닉네임을 입력해주세요" />
    </n-form-item>

    <n-form-item label="자기소개" path="profile.introText" v-bind:show-feedback="false">
      <n-input v-model:value="profileEditFormValue.profile.introText" placeholder="자기소개를 입력해주세요" type="textarea" />
    </n-form-item>

    <div class="flex gap-2 justify-end mt-6">
      <n-button @click="handleSaveProfile" type="primary">저장</n-button>
    </div>
  </n-form>
</template>

<script setup lang="ts">
import { NForm, NFormItem, NUpload, NInput, NButton, type FormInst, type UploadFileInfo, useMessage } from 'naive-ui';
import { ref, onMounted } from 'vue';
import { useUserStore } from '@/stores/userStore';
import { editProfile } from '@/api/userService';
import { getProfileImageUrl } from '@/utils/imageUtil';
import { HttpStatusCode } from 'axios';

const userStore = useUserStore();
const message = useMessage();

const profileEditForm = ref<FormInst | null>(null);
const profileEditFormValue = ref({
  profile: {
    nickname: '',
    introText: '',
    image: null as File | null,
  }
});
const profileEditFormRules = {
  profile: {
    nickname: {
      required: true,
      message: '닉네임을 입력해주세요'
    }
  }
};

const previewProfileImage = ref('');

onMounted(() => {
  if (userStore.user) {
    profileEditFormValue.value.profile.nickname = userStore.user.nickname || '';
    profileEditFormValue.value.profile.introText = userStore.user.introText || '';
    previewProfileImage.value = getProfileImageUrl(userStore.user.profileImageUrl);
  }
});

const handleProfileImageUpload = (fileList: UploadFileInfo[]) => {
  if (fileList.length > 0) {
    const file = fileList[0]!.file;
    if (file) {
      // JPG 형식 검증
      if (!file.type.includes('image/jpeg')) {
        message.error('JPG 형식의 이미지만 업로드 가능합니다.');
        return;
      }
      const reader = new FileReader();
      reader.onload = (e) => {
        previewProfileImage.value = e.target?.result as string;
        profileEditFormValue.value.profile.image = file;
      };
      reader.readAsDataURL(file);
    }
  } else {
    previewProfileImage.value = getProfileImageUrl(userStore.user?.profileImageUrl);
    profileEditFormValue.value.profile.image = null;
  }
};

const handleSaveProfile = async () => {
  await profileEditForm.value?.validate().then(async () => {
    try {
      const nickname = profileEditFormValue.value.profile.nickname;
      const introText = profileEditFormValue.value.profile.introText;
      const image = profileEditFormValue.value.profile.image || undefined;

      const resp = await editProfile({ nickname, introText }, image);
      if (resp.statusCode === HttpStatusCode.Ok) {
        userStore.user = resp.data;
        message.success('프로필을 성공적으로 변경하였습니다.');
      }
    } catch (err: any) {
      message.warning(err.response.data.message);
    }
  });
};
</script>