<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LockOutlined, PushpinFilled, TrophyFilled } from '@ant-design/icons-vue'
import { forumApi, interactionApi, userForumApi } from '../../api/s3'
import { useAuthStore } from '../../stores/auth'
import { collectionOf } from '../../utils/blog'
import { errorText, formatDate, renderMarkdown } from '../../utils/format'
import CommentSection from '../../components/CommentSection.vue'
import ImageUploadButton from '../../components/ImageUploadButton.vue'
import InteractionButtons from '../../components/InteractionButtons.vue'
import ReportAction from '../../components/ReportAction.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(true)
const postsLoading = ref(false)
const submitting = ref(false)
const followPending = ref(false)
const following = ref(false)
const topic = ref(null)
const loadError = ref(null)
const posts = ref([])
const replyTarget = ref(null)
const postContent = ref('')
const imageFileIds = ref([])
const filters = reactive({ order: 'ASC', authorOnly: false })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: false, showTotal: (total) => `共 ${total} 条公开跟帖` })
const contentHtml = computed(() => renderMarkdown(topic.value?.content))
const isSelf = computed(() => String(auth.user?.id || '') === String(topic.value?.authorId || ''))

async function loadPosts(page = 1) {
  if (!topic.value) return
  postsLoading.value = true
  try {
    const data = await forumApi.posts(topic.value.id, { page, size: pagination.pageSize, order: filters.order, authorOnly: filters.authorOnly })
    posts.value = collectionOf(data)
    pagination.current = page
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    postsLoading.value = false
  }
}

async function loadFollowing() {
  if (!auth.isLoggedIn || isSelf.value) return
  try {
    const data = await interactionApi.following({ page: 1, size: 100 })
    following.value = collectionOf(data).some((item) => String(item.followeeId) === String(topic.value?.authorId))
  } catch {}
}

async function load() {
  loading.value = true
  loadError.value = null
  topic.value = null
  posts.value = []
  cancelReply()
  try {
    topic.value = await forumApi.topic(route.params.id)
    document.title = `${topic.value?.title || '主题详情'} - 万卷技术社区`
    await Promise.all([loadPosts(1), loadFollowing()])
  } catch (error) {
    loadError.value = error
  } finally {
    loading.value = false
  }
}

function requireLogin() {
  if (auth.isLoggedIn) return true
  router.push({ name: 'login', query: { redirect: route.fullPath } })
  return false
}

async function toggleFollow() {
  if (!requireLogin() || isSelf.value) return
  followPending.value = true
  try {
    await interactionApi[following.value ? 'unfollow' : 'follow'](topic.value.authorId)
    following.value = !following.value
    message.success(following.value ? '已关注作者' : '已取消关注')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    followPending.value = false
  }
}

function startReply(post = null) {
  if (!requireLogin()) return
  replyTarget.value = post
  postContent.value = ''
  imageFileIds.value = []
  document.getElementById('post-editor')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function cancelReply() {
  replyTarget.value = null
  postContent.value = ''
  imageFileIds.value = []
}

function addImage(data) {
  if (imageFileIds.value.length >= 20) {
    message.warning('每条跟帖最多插入20张图片')
    return
  }
  imageFileIds.value.push(data.id)
  postContent.value += `\n![${String(data.name || '图片').replace(/[\[\]]/g, '')}](${data.accessUrl})\n`
}

async function submitPost() {
  if (!requireLogin()) return
  if (!postContent.value.trim()) {
    message.warning('请输入跟帖内容')
    return
  }
  submitting.value = true
  try {
    await userForumApi.createPost(topic.value.id, { topicId: topic.value.id, content: postContent.value.trim(), parentId: replyTarget.value?.id ?? null, replyToId: replyTarget.value?.id ?? null, imageFileIds: imageFileIds.value })
    message.success('跟帖已提交审核，可在我的互动中查看进度')
    cancelReply()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    submitting.value = false
  }
}

onMounted(load)
watch(() => route.params.id, (value, oldValue) => { if (value && value !== oldValue) load() })
</script>

<template>
  <div class="content-page topic-detail-page">
    <a-spin :spinning="loading">
      <template v-if="topic">
        <div class="article-back"><router-link to="/forum">← 返回技术论坛</router-link></div>
        <article class="public-article topic-article">
          <header class="article-header">
            <div class="topic-flags"><a-tag v-if="Number(topic.topped)" color="blue"><PushpinFilled /> 置顶</a-tag><a-tag v-if="Number(topic.essence)" color="gold"><TrophyFilled /> 精华</a-tag><a-tag v-if="Number(topic.locked)"><LockOutlined /> 锁帖</a-tag><span>主题 #{{ topic.id }}</span></div>
            <h1>{{ topic.title }}</h1>
            <div class="article-author"><a-avatar :size="38">{{ String(topic.authorId).slice(0, 1) }}</a-avatar><div><strong>用户 #{{ topic.authorId }}</strong><span>发布于 {{ formatDate(topic.createTime, true) }} · {{ topic.viewCount || 0 }} 次浏览</span></div><a-button v-if="!isSelf" size="small" :loading="followPending" :type="following ? 'default' : 'primary'" @click="toggleFollow">{{ following ? '已关注' : '关注作者' }}</a-button></div>
          </header>
          <div class="markdown-body article-content" v-html="contentHtml" />
          <footer class="topic-actions"><InteractionButtons target-type="FORUM" :target-id="topic.id" :like-count="topic.likeCount" :favorite-count="topic.favoriteCount" check-state /><ReportAction target-type="FORUM" :target-id="topic.id" :compact="false" /><a-button v-if="!Number(topic.locked)" type="primary" @click="startReply()">回复主题</a-button></footer>
        </article>

        <section class="content-card post-section">
          <div class="post-section-heading"><h2>全部跟帖 <span>{{ pagination.total }}</span></h2><a-space wrap><a-checkbox v-model:checked="filters.authorOnly" @change="loadPosts(1)">只看楼主</a-checkbox><a-select v-model:value="filters.order" size="small" :options="[{ value: 'ASC', label: '正序' }, { value: 'DESC', label: '倒序' }]" @change="loadPosts(1)" /></a-space></div>
          <a-alert v-if="Number(topic.locked)" message="主题已锁定，不能继续回复。" type="warning" show-icon />
          <a-spin :spinning="postsLoading">
            <div v-if="posts.length" class="post-list">
              <article v-for="post in posts" :key="post.id" class="post-item">
                <div class="post-floor">#{{ post.floorNo || post.id }}</div>
                <div class="post-body"><div class="comment-meta"><strong>用户 #{{ post.authorId }}</strong><span v-if="post.replyToId">引用跟帖 #{{ post.replyToId }}</span><time>{{ formatDate(post.createTime, true) }}</time></div><div class="markdown-body post-content" v-html="renderMarkdown(post.content)" /><div class="comment-actions"><InteractionButtons target-type="FORUM_POST" :target-id="post.id" :like-count="post.likeCount" :show-favorite="false" compact /><a-button v-if="!Number(topic.locked)" type="text" size="small" @click="startReply(post)">回复</a-button><ReportAction target-type="FORUM_POST" :target-id="post.id" /></div></div>
              </article>
            </div>
            <a-empty v-else-if="!postsLoading" description="暂无公开跟帖" />
          </a-spin>
          <a-pagination v-if="pagination.total > pagination.pageSize" v-model:current="pagination.current" :total="pagination.total" :page-size="pagination.pageSize" :show-total="pagination.showTotal" @change="loadPosts" />
          <div v-if="!Number(topic.locked)" id="post-editor" class="post-editor">
            <a-alert v-if="replyTarget" :message="`正在回复跟帖 #${replyTarget.floorNo || replyTarget.id}`" type="info" show-icon closable @close="cancelReply" />
            <a-textarea v-model:value="postContent" :rows="5" :maxlength="50000" show-count placeholder="友善回复，支持 Markdown 与图片" />
            <div class="comment-editor-actions"><ImageUploadButton @uploaded="addImage" /><a-space><a-button v-if="replyTarget" @click="cancelReply">取消引用</a-button><a-button type="primary" :loading="submitting" @click="submitPost">提交跟帖</a-button></a-space></div>
          </div>
        </section>
        <CommentSection target-type="FORUM" :target-id="topic.id" />
      </template>
      <a-result v-else-if="loadError" :status="loadError.response?.status === 403 ? '403' : '404'" title="主题暂不可访问" :sub-title="loadError.response?.status === 403 || loadError.response?.status === 404 ? '主题不存在、尚未公开或已下线。' : errorText(loadError)"><template #extra><a-button type="primary" @click="router.replace('/forum')">返回公开论坛</a-button><a-button @click="load">重新加载</a-button></template></a-result>
    </a-spin>
  </div>
</template>
