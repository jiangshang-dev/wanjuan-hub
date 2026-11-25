<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { interactionApi, userCommentApi, userForumApi, userReportApi } from '../../api/s3'
import { blogStatus, collectionOf } from '../../utils/blog'
import { errorText, formatDate, renderMarkdown } from '../../utils/format'
import ImageUploadButton from '../../components/ImageUploadButton.vue'

const tabs = [
  { key: 'topics', label: '我的主题' }, { key: 'posts', label: '我的跟帖' }, { key: 'comments', label: '我的评论' },
  { key: 'likes', label: '我的点赞' }, { key: 'favorites', label: '我的收藏' }, { key: 'following', label: '我的关注' },
  { key: 'followers', label: '我的粉丝' }, { key: 'reports', label: '我的举报' },
]
const route = useRoute()
const router = useRouter()
const activeTab = ref(tabs.some((tab) => tab.key === route.query.tab) ? route.query.tab : 'topics')
const loading = ref(false)
const records = ref([])
const filters = reactive({ keyword: '', status: undefined, targetType: undefined })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: false, showTotal: (total) => `共 ${total} 条` })
const editOpen = ref(false)
const editSaving = ref(false)
const editForm = reactive({ id: '', topicId: '', content: '', imageFileIds: [] })
const isContentTab = computed(() => ['topics', 'posts', 'comments'].includes(activeTab.value))
const isTargetTab = computed(() => ['likes', 'favorites', 'reports'].includes(activeTab.value))
const targetOptions = [{ value: 'BLOG', label: '博客' }, { value: 'FORUM', label: '论坛主题' }, { value: 'FORUM_POST', label: '论坛跟帖' }, { value: 'COMMENT', label: '评论' }, { value: 'RESOURCE', label: '资源' }]
const contentStatuses = ['DRAFT', 'PENDING', 'PUBLISHED', 'REJECTED', 'MANUAL_REVIEW', 'OFFLINE'].map((value) => ({ value, label: blogStatus(value).text }))
const reportStatuses = [{ value: 'PENDING', label: '待处理' }, { value: 'PROCESSED', label: '已处理' }, { value: 'REJECTED', label: '已驳回' }]

function targetName(type) {
  return targetOptions.find((item) => item.value === type)?.label || type || '内容'
}

function targetLink(type, id) {
  if (type === 'BLOG') return `/blogs/${id}`
  if (type === 'FORUM') return `/forum/topics/${id}`
  return ''
}

function commentLink(record) {
  return targetLink(record.targetType, record.targetId)
}

async function load(page = 1) {
  loading.value = true
  try {
    const base = { page, size: pagination.pageSize }
    let data
    if (activeTab.value === 'topics') data = await userForumApi.topics({ ...base, keyword: filters.keyword.trim() || undefined, status: filters.status })
    else if (activeTab.value === 'posts') data = await userForumApi.posts({ ...base, status: filters.status })
    else if (activeTab.value === 'comments') data = await userCommentApi.list({ ...base, status: filters.status })
    else if (activeTab.value === 'likes') data = await interactionApi.likes({ ...base, targetType: filters.targetType })
    else if (activeTab.value === 'favorites') data = await interactionApi.favorites({ ...base, targetType: filters.targetType })
    else if (activeTab.value === 'following') data = await interactionApi.following(base)
    else if (activeTab.value === 'followers') data = await interactionApi.followers(base)
    else data = await userReportApi.list({ ...base, status: filters.status, targetType: filters.targetType })
    records.value = collectionOf(data)
    pagination.current = page
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    records.value = []
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}

function changeTab(key) {
  activeTab.value = key
  Object.assign(filters, { keyword: '', status: undefined, targetType: undefined })
  router.replace({ query: { ...route.query, tab: key } })
  load(1)
}

function resetFilters() {
  Object.assign(filters, { keyword: '', status: undefined, targetType: undefined })
  load(1)
}

async function remove(kind, record) {
  try {
    if (kind === 'topic') await userForumApi.deleteTopic(record.id)
    else if (kind === 'post') await userForumApi.deletePost(record.id)
    else await userCommentApi.remove(record.id)
    message.success('已删除')
    load(records.value.length === 1 && pagination.current > 1 ? pagination.current - 1 : pagination.current)
  } catch (error) {
    message.error(errorText(error))
  }
}

async function openPostEditor(record) {
  try {
    const post = await userForumApi.post(record.id)
    Object.assign(editForm, { id: String(post.id), topicId: String(post.topicId), content: post.content || '', imageFileIds: [...(post.imageFileIds || [])] })
    editOpen.value = true
  } catch (error) {
    message.error(errorText(error))
  }
}

function addPostImage(data) {
  if (editForm.imageFileIds.length >= 20) {
    message.warning('每条跟帖最多插入20张图片')
    return
  }
  editForm.imageFileIds.push(data.id)
  editForm.content += `\n![${String(data.name || '图片').replace(/[\[\]]/g, '')}](${data.accessUrl})\n`
}

async function savePost() {
  if (!editForm.content.trim()) {
    message.warning('请输入跟帖内容')
    return
  }
  editSaving.value = true
  try {
    await userForumApi.updatePost(editForm.id, { topicId: editForm.topicId, content: editForm.content.trim(), imageFileIds: editForm.imageFileIds })
    message.success('跟帖已更新并重新提交审核')
    editOpen.value = false
    load()
  } catch (error) {
    message.error(errorText(error))
  } finally {
    editSaving.value = false
  }
}

async function unfollow(record) {
  try {
    await interactionApi.unfollow(record.followeeId)
    message.success('已取消关注')
    load()
  } catch (error) {
    message.error(errorText(error))
  }
}

function reportStatus(status) {
  return { PENDING: { text: '待处理', color: 'processing' }, PROCESSED: { text: '已处理', color: 'success' }, REJECTED: { text: '已驳回', color: 'default' } }[status] || { text: status || '未知', color: 'default' }
}

onMounted(() => load())
watch(() => route.query.tab, (tab) => {
  if (tab && tabs.some((item) => item.key === tab) && tab !== activeTab.value) {
    activeTab.value = tab
    load(1)
  }
})
</script>

<template>
  <div class="content-page activity-page">
    <div class="page-heading"><div><span class="eyebrow">MY COMMUNITY</span><h1>我的互动</h1><p>集中管理论坛内容、评论、互动记录与举报进度。</p></div><a-button type="primary" @click="router.push('/my/forum/topics/new')">发布主题</a-button></div>
    <div class="content-card activity-card">
      <a-tabs :active-key="activeTab" @change="changeTab"><a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.label" /></a-tabs>
      <div class="activity-filters"><a-space wrap><a-input-search v-if="activeTab === 'topics'" v-model:value="filters.keyword" allow-clear placeholder="搜索我的主题" style="width: 240px" @search="load(1)" /><a-select v-if="isContentTab" v-model:value="filters.status" allow-clear placeholder="全部审核状态" style="width: 160px" :options="contentStatuses" /><a-select v-if="activeTab === 'reports'" v-model:value="filters.status" allow-clear placeholder="全部处理状态" style="width: 150px" :options="reportStatuses" /><a-select v-if="isTargetTab" v-model:value="filters.targetType" allow-clear placeholder="全部目标类型" style="width: 150px" :options="targetOptions" /><a-button type="primary" @click="load(1)">查询</a-button><a-button @click="resetFilters">重置</a-button></a-space></div>
      <a-spin :spinning="loading">
        <div v-if="records.length" class="activity-list">
          <template v-if="activeTab === 'topics'"><article v-for="record in records" :key="record.id" class="activity-item"><div class="activity-item-main"><div class="activity-item-title"><router-link v-if="record.status === 'PUBLISHED'" :to="`/forum/topics/${record.id}`">{{ record.title }}</router-link><strong v-else>{{ record.title }}</strong><a-tag :color="blogStatus(record.status).color">{{ blogStatus(record.status).text }}</a-tag></div><p>{{ record.content }}</p><div class="activity-meta"><span>板块 #{{ record.boardId }}</span><span>跟帖 {{ record.postCount || 0 }}</span><span>更新 {{ formatDate(record.updateTime || record.createTime, true) }}</span></div><a-alert v-if="record.auditReason" :message="`审核说明：${record.auditReason}`" type="warning" show-icon /></div><a-space wrap><a-button v-if="record.status === 'PUBLISHED'" size="small" @click="router.push(`/forum/topics/${record.id}`)">查看</a-button><a-button v-if="record.status !== 'OFFLINE'" size="small" @click="router.push(`/my/forum/topics/${record.id}/edit`)">编辑</a-button><a-popconfirm title="确定删除该主题吗？" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="remove('topic', record)"><a-button size="small" danger>删除</a-button></a-popconfirm></a-space></article></template>
          <template v-else-if="activeTab === 'posts'"><article v-for="record in records" :key="record.id" class="activity-item"><div class="activity-item-main"><div class="activity-item-title"><strong>跟帖 #{{ record.id }} · 主题 #{{ record.topicId }}</strong><a-tag :color="blogStatus(record.status).color">{{ blogStatus(record.status).text }}</a-tag></div><div class="markdown-body activity-content" v-html="renderMarkdown(record.content)" /><div class="activity-meta"><span>发布 {{ formatDate(record.createTime, true) }}</span><span>点赞 {{ record.likeCount || 0 }}</span></div><a-alert v-if="record.auditReason" :message="`审核说明：${record.auditReason}`" type="warning" show-icon /></div><a-space wrap><a-button v-if="record.status === 'PUBLISHED'" size="small" @click="router.push(`/forum/topics/${record.topicId}`)">查看主题</a-button><a-button v-if="record.status !== 'OFFLINE'" size="small" @click="openPostEditor(record)">编辑</a-button><a-popconfirm title="确定删除该跟帖吗？" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="remove('post', record)"><a-button size="small" danger>删除</a-button></a-popconfirm></a-space></article></template>
          <template v-else-if="activeTab === 'comments'"><article v-for="record in records" :key="record.id" class="activity-item"><div class="activity-item-main"><div class="activity-item-title"><strong>{{ targetName(record.targetType) }} #{{ record.targetId }}</strong><a-tag :color="blogStatus(record.status).color">{{ blogStatus(record.status).text }}</a-tag></div><div class="markdown-body activity-content" v-html="renderMarkdown(record.content)" /><div class="activity-meta"><span>发布 {{ formatDate(record.createTime, true) }}</span><span>回复 {{ record.replyCount || 0 }}</span><span>点赞 {{ record.likeCount || 0 }}</span></div><a-alert v-if="record.auditReason" :message="`审核说明：${record.auditReason}`" type="warning" show-icon /></div><a-space wrap><a-button v-if="record.status === 'PUBLISHED' && commentLink(record)" size="small" @click="router.push(commentLink(record))">查看内容</a-button><a-popconfirm title="确定删除该评论吗？" ok-text="确认删除" ok-type="danger" cancel-text="取消" @confirm="remove('comment', record)"><a-button size="small" danger>删除</a-button></a-popconfirm></a-space></article></template>
          <template v-else-if="['likes', 'favorites'].includes(activeTab)"><article v-for="record in records" :key="record.id" class="activity-item compact-item"><div><strong>{{ targetName(record.targetType) }} #{{ record.targetId }}</strong><div class="activity-meta"><span>{{ activeTab === 'likes' ? '点赞' : '收藏' }}于 {{ formatDate(record.createTime, true) }}</span></div></div><a-button v-if="targetLink(record.targetType, record.targetId)" size="small" @click="router.push(targetLink(record.targetType, record.targetId))">查看内容</a-button></article></template>
          <template v-else-if="['following', 'followers'].includes(activeTab)"><article v-for="record in records" :key="record.id" class="activity-item compact-item"><div><strong>用户 #{{ activeTab === 'following' ? record.followeeId : record.followerId }}</strong><div class="activity-meta"><span>建立关系于 {{ formatDate(record.createTime, true) }}</span></div></div><a-popconfirm v-if="activeTab === 'following'" title="确定取消关注该用户吗？" ok-text="确定" cancel-text="取消" @confirm="unfollow(record)"><a-button size="small">取消关注</a-button></a-popconfirm></article></template>
          <template v-else><article v-for="record in records" :key="record.id" class="activity-item"><div class="activity-item-main"><div class="activity-item-title"><strong>{{ targetName(record.targetType) }} #{{ record.targetId }} · {{ record.reason }}</strong><a-tag :color="reportStatus(record.status).color">{{ reportStatus(record.status).text }}</a-tag></div><p>{{ record.detail || '未填写补充说明' }}</p><div class="activity-meta"><span>提交于 {{ formatDate(record.createTime, true) }}</span><span v-if="record.handleTime">处理于 {{ formatDate(record.handleTime, true) }}</span></div><a-alert v-if="record.handleNote" :message="`处理说明：${record.handleNote}`" type="info" show-icon /></div><a-tag v-if="record.action">{{ record.action }}</a-tag></article></template>
        </div>
        <a-empty v-else-if="!loading" description="暂无记录" />
      </a-spin>
      <a-pagination v-if="pagination.total > pagination.pageSize" v-model:current="pagination.current" class="list-pagination" :total="pagination.total" :page-size="pagination.pageSize" :show-total="pagination.showTotal" @change="load" />
    </div>
    <a-modal v-model:open="editOpen" title="编辑跟帖" ok-text="保存并重新审核" cancel-text="取消" :confirm-loading="editSaving" width="720px" @ok="savePost"><a-alert message="已下线或所属主题已锁定的跟帖不能编辑。" type="info" show-icon class="editor-notice" /><div class="content-upload-bar"><span>支持 Markdown，最多20张图片</span><ImageUploadButton @uploaded="addPostImage" /></div><a-textarea v-model:value="editForm.content" :rows="14" :maxlength="50000" show-count /></a-modal>
  </div>
</template>
