<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { commentApi, userCommentApi } from '../api/s3'
import { useAuthStore } from '../stores/auth'
import { collectionOf } from '../utils/blog'
import { errorText, formatDate, renderMarkdown } from '../utils/format'
import ImageMarkdownUpload from './ImageUploadButton.vue'
import InteractionBar from './InteractionButtons.vue'
import ReportButton from './ReportAction.vue'

const props = defineProps({ targetType: { type: String, required: true }, targetId: { type: [String, Number], required: true } })
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const submitting = ref(false)
const roots = ref([])
const replies = reactive({})
const replyLoading = reactive({})
const replyTarget = ref(null)
const content = ref('')
const imageFileIds = ref([])
const pagination = reactive({ current: 1, pageSize: 5, total: 0, showSizeChanger: false, showTotal: (total) => `共 ${total} 条评论` })

async function load(page = 1) {
  loading.value = true
  try {
    const data = await commentApi.list({ page, size: pagination.pageSize, targetType: props.targetType, targetId: props.targetId })
    roots.value = collectionOf(data)
    pagination.current = page
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}

async function loadReplies(root, page = 1) {
  const key = String(root.id)
  replyLoading[key] = true
  try {
    const size = 5
    const data = await commentApi.replies(root.id, { page, size, targetType: props.targetType, targetId: props.targetId })
    const previous = page > 1 ? replies[key]?.records || [] : []
    replies[key] = { records: [...previous, ...collectionOf(data)], page, total: Number(data?.total || 0), size }
  } catch (error) {
    message.error(errorText(error))
  } finally {
    replyLoading[key] = false
  }
}

function startReply(comment) {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  replyTarget.value = comment
  content.value = ''
  imageFileIds.value = []
  document.getElementById(`comment-editor-${props.targetType}-${props.targetId}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function cancelReply() {
  replyTarget.value = null
  content.value = ''
  imageFileIds.value = []
}

function addImage(data) {
  if (imageFileIds.value.length >= 9) {
    message.warning('每条评论最多插入9张图片')
    return
  }
  imageFileIds.value.push(data.id)
  content.value += `\n![${String(data.name || '图片').replace(/[\[\]]/g, '')}](${data.accessUrl})\n`
}

async function submit() {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!content.value.trim()) {
    message.warning('请输入评论内容')
    return
  }
  submitting.value = true
  try {
    await userCommentApi.create({
      targetType: props.targetType, targetId: props.targetId,
      parentId: replyTarget.value?.id ?? null, replyToId: replyTarget.value?.id ?? null,
      content: content.value.trim(), imageFileIds: imageFileIds.value,
    })
    message.success('评论已提交审核，可在我的互动中查看进度')
    cancelReply()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    submitting.value = false
  }
}

onMounted(() => load())
watch(() => [props.targetType, props.targetId], () => {
  Object.keys(replies).forEach((key) => delete replies[key])
  cancelReply()
  load(1)
})
</script>

<template>
  <section class="comment-section content-card">
    <div class="comment-heading"><h2>评论 <span>{{ pagination.total }}</span></h2><span>文明交流，友善互助</span></div>
    <div :id="`comment-editor-${targetType}-${targetId}`" class="comment-editor">
      <a-alert v-if="replyTarget" :message="`正在回复评论 #${replyTarget.id}`" type="info" show-icon closable @close="cancelReply" />
      <a-textarea v-model:value="content" :rows="4" :maxlength="10000" show-count :placeholder="auth.isLoggedIn ? '写下你的看法，支持 Markdown 与图片' : '登录后参与评论'" @focus="!auth.isLoggedIn && router.push({ name: 'login', query: { redirect: route.fullPath } })" />
      <div class="comment-editor-actions"><ImageMarkdownUpload v-if="auth.isLoggedIn" @uploaded="addImage" /><a-space><a-button v-if="replyTarget" @click="cancelReply">取消回复</a-button><a-button type="primary" :loading="submitting" @click="submit">提交评论</a-button></a-space></div>
    </div>
    <a-spin :spinning="loading">
      <div v-if="roots.length" class="comment-list">
        <article v-for="root in roots" :key="root.id" class="comment-item">
          <div class="comment-meta"><strong>用户 #{{ root.authorId }}</strong><time>{{ formatDate(root.createTime, true) }}</time></div>
          <div class="markdown-body comment-content" v-html="renderMarkdown(root.content)" />
          <div class="comment-actions"><InteractionBar target-type="COMMENT" :target-id="root.id" :like-count="root.likeCount" :dislike-count="root.dislikeCount" :show-favorite="false" compact /><a-button type="text" size="small" @click="startReply(root)">回复</a-button><ReportButton target-type="COMMENT" :target-id="root.id" /></div>
          <div class="comment-replies">
            <a-button v-if="Number(root.replyCount) && !replies[String(root.id)]" type="link" size="small" :loading="replyLoading[String(root.id)]" @click="loadReplies(root)">查看 {{ root.replyCount }} 条回复</a-button>
            <template v-if="replies[String(root.id)]">
              <article v-for="reply in replies[String(root.id)].records" :key="reply.id" class="comment-item reply-item">
                <div class="comment-meta"><strong>用户 #{{ reply.authorId }}</strong><span v-if="reply.replyToId">回复评论 #{{ reply.replyToId }}</span><time>{{ formatDate(reply.createTime, true) }}</time></div>
                <div class="markdown-body comment-content" v-html="renderMarkdown(reply.content)" />
                <div class="comment-actions"><InteractionBar target-type="COMMENT" :target-id="reply.id" :like-count="reply.likeCount" :dislike-count="reply.dislikeCount" :show-favorite="false" compact /><a-button type="text" size="small" @click="startReply(reply)">回复</a-button><ReportButton target-type="COMMENT" :target-id="reply.id" /></div>
              </article>
              <a-button v-if="replies[String(root.id)].records.length < replies[String(root.id)].total" type="link" size="small" :loading="replyLoading[String(root.id)]" @click="loadReplies(root, replies[String(root.id)].page + 1)">加载更多回复</a-button>
            </template>
          </div>
        </article>
      </div>
      <a-empty v-else-if="!loading" description="暂无公开评论，来发表第一条看法" />
    </a-spin>
    <a-pagination v-if="pagination.total > pagination.pageSize" v-model:current="pagination.current" size="small" :total="pagination.total" :page-size="pagination.pageSize" :show-total="pagination.showTotal" @change="load" />
  </section>
</template>
