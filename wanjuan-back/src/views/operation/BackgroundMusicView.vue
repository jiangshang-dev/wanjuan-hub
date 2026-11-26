<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SaveOutlined } from '@ant-design/icons-vue'
import { adminSiteExperienceApi } from '../../api/s6'
import { errorText, formatDate } from '../../utils'

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const updateTime = ref('')
const form = reactive({ enabled: false, audioUrl: '', playMode: 'AUTO', playerVisible: true })
const formRules = {
  audioUrl: [
    { max: 1000, message: '音频地址不能超过1000个字符' },
    { validator: (_, value) => {
      const url = (value || '').trim()
      if (form.enabled && !url) return Promise.reject(new Error('开启背景音乐时必须配置音频地址'))
      if (url && !/^https?:\/\/\S+$/i.test(url)) return Promise.reject(new Error('音频地址必须为有效的 HTTP/HTTPS 地址'))
      return Promise.resolve()
    } },
  ],
  playMode: [{ required: true, message: '请选择播放方式' }],
}

async function load() {
  loading.value = true
  try {
    const data = await adminSiteExperienceApi.backgroundMusic()
    Object.assign(form, {
      enabled: Boolean(data?.enabled),
      audioUrl: data?.audioUrl || '',
      playMode: data?.playMode || 'AUTO',
      playerVisible: Boolean(data?.playerVisible),
    })
    updateTime.value = data?.updateTime || ''
    formRef.value?.clearValidate()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await adminSiteExperienceApi.saveBackgroundMusic({ ...form, audioUrl: form.audioUrl.trim() })
    message.success('背景音乐配置已保存')
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="management-page background-music-page">
    <a-alert message="自动播放可能受浏览器策略限制；手动播放需保持播放器可见，以便用户主动播放。" type="info" show-icon class="page-alert" />
    <a-card :bordered="false" class="panel-card" title="背景音乐配置">
      <a-spin :spinning="loading">
        <a-form ref="formRef" :model="form" :rules="formRules" layout="vertical" class="music-form" @finish="save">
          <a-form-item label="背景音乐开关" name="enabled" extra="关闭后前台不播放背景音乐，音频地址和播放配置仍会保留。">
            <a-switch v-model:checked="form.enabled" checked-children="开启" un-checked-children="关闭" @change="formRef?.clearValidate('audioUrl')" />
          </a-form-item>
          <a-form-item label="音频地址" name="audioUrl" extra="请输入可公开访问的 HTTP/HTTPS 音频地址，开启背景音乐时必填。">
            <a-input v-model:value="form.audioUrl" :maxlength="1000" allow-clear placeholder="https://example.com/background-music.mp3" />
          </a-form-item>
          <a-form-item label="播放方式" name="playMode">
            <a-radio-group v-model:value="form.playMode" :options="[{ value: 'AUTO', label: 'AUTO 自动播放' }, { value: 'MANUAL', label: 'MANUAL 手动播放' }]" />
          </a-form-item>
          <a-form-item label="播放器显隐" name="playerVisible" extra="控制前台背景音乐播放器是否展示。">
            <a-switch v-model:checked="form.playerVisible" checked-children="显示" un-checked-children="隐藏" />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button v-permission="'operation:music:update'" type="primary" html-type="submit" :loading="saving"><SaveOutlined />保存配置</a-button>
              <a-button :loading="loading" @click="load"><ReloadOutlined />重新加载</a-button>
              <span v-if="updateTime" class="update-time">最近更新：{{ formatDate(updateTime) }}</span>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<style scoped>
.background-music-page { max-width: 900px; }
.music-form { max-width: 760px; }
.update-time { color: var(--muted); font-size: 13px; }
</style>
