<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { userApi } from '../../api'
import ImageUploadButton from '../../components/ImageUploadButton.vue'
import { useAuthStore } from '../../stores/auth'
import { displayName, errorText } from '../../utils/format'

const auth = useAuthStore()
const formRef = ref()
const loading = ref(true)
const saving = ref(false)
const profile = ref({})
const form = reactive({ nickname: '', avatar: '', avatarFileId: null, removeAvatar: false, signature: '', birthday: null, gender: 0 })
const rules = {
  nickname: [{ required: true, whitespace: true, message: '请输入昵称' }, { max: 50, message: '昵称不能超过50个字符' }],
  signature: [{ max: 255, message: '签名不能超过255个字符' }],
  birthday: [{ validator: (_, value) => !value || value.isBefore(dayjs(), 'day') ? Promise.resolve() : Promise.reject(new Error('生日不能是今天或未来日期')) }],
}
const auditStatus = computed(() => profile.value?.profileAuditStatus || '')
const auditAlert = computed(() => ({
  PENDING: { type: 'info', text: '昵称、头像或签名正在等待审核，当前公开资料保持不变。' },
  PROCESSING: { type: 'info', text: '资料正在审核中，当前公开资料保持不变。' },
  MANUAL_REVIEW: { type: 'warning', text: '资料已进入人工审核，当前公开资料保持不变。' },
  REJECTED: { type: 'error', text: `资料审核未通过${profile.value?.profileAuditReason ? `：${profile.value.profileAuditReason}` : '，请修改后重新提交'}` },
  FAILED: { type: 'error', text: `资料审核处理失败${profile.value?.profileAuditReason ? `：${profile.value.profileAuditReason}` : '，请重新提交'}` },
}[auditStatus.value]))

function applyProfile(data) {
  profile.value = data || {}
  const hasPendingRevision = data?.pendingNickname != null
  Object.assign(form, {
    nickname: (hasPendingRevision ? data.pendingNickname : data?.nickname) || '',
    avatar: hasPendingRevision ? (data.pendingRemoveAvatar ? '' : data.pendingAvatar || '') : data?.avatar || '',
    avatarFileId: hasPendingRevision ? data.pendingAvatarFileId || null : data?.avatarFileId || null,
    removeAvatar: hasPendingRevision && Boolean(data.pendingRemoveAvatar),
    signature: (hasPendingRevision ? data.pendingSignature : data?.signature) || '',
    birthday: data?.birthday ? dayjs(data.birthday) : null,
    gender: data?.gender ?? 0,
  })
  auth.user = { ...auth.user, ...data }
}

onMounted(async () => {
  try {
    applyProfile(await userApi.profile())
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
})

function onAvatarUploaded(file) {
  form.avatar = file.accessUrl
  form.avatarFileId = file.id
  form.removeAvatar = false
}

function removeAvatar() {
  form.avatar = ''
  form.avatarFileId = null
  form.removeAvatar = true
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      nickname: form.nickname.trim(),
      avatarFileId: form.avatarFileId,
      removeAvatar: form.removeAvatar,
      signature: form.signature.trim() || null,
      birthday: form.birthday ? form.birthday.format('YYYY-MM-DD') : null,
      gender: form.gender,
    }
    const data = await userApi.updateProfile(payload)
    applyProfile(data)
    message.success('资料已保存，需审核内容将在审核通过后公开')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="content-page narrow-page">
    <div class="page-heading"><div><span class="eyebrow">PROFILE</span><h1>个人资料</h1><p>完善你的公开资料，让其他社区成员更了解你。</p></div><a-button type="primary" @click="$router.push('/my/blogs')">进入我的博客</a-button></div>
    <a-card :bordered="false" class="content-card">
      <a-spin :spinning="loading">
        <div class="profile-summary">
          <a-avatar :src="profile.avatar" :size="72">{{ displayName(profile).slice(0, 1) }}</a-avatar>
          <div><h2>{{ profile.nickname || auth.user?.username || '社区用户' }}</h2><p>{{ profile.signature || '这个人很低调，还没有填写签名。' }}</p></div>
        </div>
        <a-divider />
        <a-alert v-if="auditAlert" :type="auditAlert.type" :message="auditAlert.text" show-icon class="audit-alert" />
        <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" class="profile-form" @finish="submit">
          <a-form-item label="昵称" name="nickname" required><a-input v-model:value="form.nickname" :maxlength="50" show-count placeholder="请输入昵称" /></a-form-item>
          <a-form-item label="头像" extra="上传私有图片，审核通过后才会替换公开头像">
            <a-space size="middle" wrap>
              <a-avatar :src="form.avatar" :size="64">{{ displayName(form).slice(0, 1) }}</a-avatar>
              <ImageUploadButton label="上传新头像" @uploaded="onAvatarUploaded" />
              <a-button v-if="!form.removeAvatar && (form.avatar || form.avatarFileId)" danger @click="removeAvatar">移除头像</a-button>
              <a-tag v-if="form.removeAvatar" color="orange">保存并审核通过后移除</a-tag>
            </a-space>
          </a-form-item>
          <a-form-item label="个人签名" name="signature"><a-textarea v-model:value="form.signature" :rows="4" :maxlength="255" show-count placeholder="介绍一下自己" /></a-form-item>
          <div class="form-grid-two">
            <a-form-item label="生日" name="birthday"><a-date-picker v-model:value="form.birthday" class="full-width" placeholder="请选择生日" /></a-form-item>
            <a-form-item label="性别" name="gender">
              <a-select v-model:value="form.gender" :options="[{ value: 0, label: '保密' }, { value: 1, label: '男' }, { value: 2, label: '女' }]" />
            </a-form-item>
          </div>
          <a-form-item><a-button type="primary" html-type="submit" :loading="saving">保存资料</a-button></a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<style scoped>
.audit-alert { margin-bottom: 24px; }
</style>
