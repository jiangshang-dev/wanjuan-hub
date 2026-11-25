<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { forumApi, userForumApi } from '../../api/s3'
import { collectionOf } from '../../utils/blog'
import { errorText, renderMarkdown } from '../../utils/format'
import ImageUploadButton from '../../components/ImageUploadButton.vue'

const route = useRoute()
const router = useRouter()
const editingId = ref(route.params.id ? String(route.params.id) : '')
const formRef = ref()
const loading = ref(true)
const saving = ref(false)
const activeTab = ref('edit')
const boards = ref([])
const form = reactive({ boardId: undefined, title: '', content: '', imageFileIds: [] })
const previewHtml = computed(() => renderMarkdown(form.content))
const boardOptions = computed(() => boards.value.map((board) => ({ value: String(board.id), label: `${board.name}（${board.postPermission === 'USER' ? '可发帖' : '限制发帖'}）` })))
const rules = {
  boardId: [{ required: true, message: '请选择板块' }],
  title: [{ required: true, whitespace: true, message: '请输入主题标题' }, { max: 120, message: '标题不能超过120个字符' }],
  content: [{ required: true, whitespace: true, message: '请输入主题正文' }, { max: 100000, message: '正文不能超过100000个字符' }],
}

onMounted(async () => {
  try {
    const [boardData, topic] = await Promise.all([forumApi.boards(), editingId.value ? userForumApi.topic(editingId.value) : Promise.resolve(null)])
    boards.value = collectionOf(boardData)
    if (topic) Object.assign(form, { boardId: String(topic.boardId), title: topic.title || '', content: topic.content || '', imageFileIds: [...(topic.imageFileIds || [])] })
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
})

function addImage(data) {
  if (form.imageFileIds.length >= 20) {
    message.warning('每个主题最多插入20张图片')
    return
  }
  form.imageFileIds.push(data.id)
  form.content += `\n![${String(data.name || '图片').replace(/[\[\]]/g, '')}](${data.accessUrl})\n`
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const data = { boardId: form.boardId, title: form.title.trim(), content: form.content.trim(), imageFileIds: [...form.imageFileIds] }
    if (editingId.value) await userForumApi.updateTopic(editingId.value, data)
    else await userForumApi.createTopic(data)
    message.success(editingId.value ? '主题已更新并重新提交审核' : '主题已提交审核')
    router.push({ path: '/my/activity', query: { tab: 'topics' } })
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="content-page narrow-page topic-editor-page">
    <div class="page-heading"><div><span class="eyebrow">FORUM EDITOR</span><h1>{{ editingId ? '编辑主题' : '发布主题' }}</h1><p>主题保存后将进入内容审核，公开版本不会因审核中的修改而下线。</p></div><a-button @click="router.push(editingId ? '/my/activity' : '/forum')">返回</a-button></div>
    <a-spin :spinning="loading">
      <a-alert v-if="editingId" message="编辑时不能变更所属板块，已下线主题不能编辑。" type="info" show-icon class="editor-notice" />
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" class="content-card topic-editor-card">
        <a-form-item label="所属板块" name="boardId"><a-select v-model:value="form.boardId" :options="boardOptions" :disabled="Boolean(editingId)" placeholder="请选择允许发帖的板块" /></a-form-item>
        <a-form-item label="主题标题" name="title"><a-input v-model:value="form.title" :maxlength="120" show-count placeholder="用清晰的标题概括问题或分享" /></a-form-item>
        <a-tabs v-model:active-key="activeTab">
          <a-tab-pane key="edit" tab="Markdown 编辑">
            <div class="content-upload-bar"><span>支持 Markdown，最多插入20张图片</span><ImageUploadButton @uploaded="addImage" /></div>
            <a-form-item name="content" class="content-editor-item"><a-textarea v-model:value="form.content" class="markdown-textarea topic-content-input" :rows="20" placeholder="描述背景、过程和你希望讨论的内容…" /></a-form-item>
          </a-tab-pane>
          <a-tab-pane key="preview" tab="安全预览"><div v-if="form.content" class="markdown-body editor-preview" v-html="previewHtml" /><a-empty v-else description="暂无正文内容" /></a-tab-pane>
        </a-tabs>
        <div class="topic-editor-actions"><a-button @click="router.back()">取消</a-button><a-button type="primary" :loading="saving" @click="submit">{{ editingId ? '保存并重新审核' : '提交审核' }}</a-button></div>
      </a-form>
    </a-spin>
  </div>
</template>
