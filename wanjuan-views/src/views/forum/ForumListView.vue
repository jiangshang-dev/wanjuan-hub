<script setup>
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { CommentOutlined, EyeOutlined, LikeOutlined, LockOutlined, PushpinFilled, TrophyFilled } from '@ant-design/icons-vue'
import { forumApi } from '../../api/s3'
import { collectionOf } from '../../utils/blog'
import { errorText, formatDate } from '../../utils/format'

const loading = ref(false)
const boards = ref([])
const records = ref([])
const filters = reactive({ keyword: '', boardId: undefined, sort: 'LATEST' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: false, showTotal: (total) => `共 ${total} 个公开主题` })

async function load() {
  loading.value = true
  try {
    const data = await forumApi.topics({ page: pagination.current, size: pagination.pageSize, keyword: filters.keyword.trim() || undefined, boardId: filters.boardId, sort: filters.sort })
    records.value = collectionOf(data)
    pagination.total = Number(data?.total || 0)
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.current = 1
  load()
}

function selectBoard(value) {
  filters.boardId = value
  search()
}

function changePage(page) {
  pagination.current = page
  load()
}

onMounted(async () => {
  load()
  try {
    boards.value = collectionOf(await forumApi.boards())
  } catch (error) {
    message.error(errorText(error))
  }
})
</script>

<template>
  <div class="content-page forum-list-page">
    <div class="page-heading"><div><span class="eyebrow">COMMUNITY FORUM</span><h1>技术论坛</h1><p>提问、分享与讨论，让每一次实践都有回响。</p></div><a-button type="primary" @click="$router.push('/my/forum/topics/new')">发布主题</a-button></div>
    <div class="forum-filter-bar">
      <div class="forum-filter-top"><a-input-search v-model:value="filters.keyword" allow-clear placeholder="搜索公开主题" class="forum-search" @search="search" @press-enter="search" /><a-radio-group v-model:value="filters.sort" button-style="solid" @change="search"><a-radio-button value="LATEST">最新</a-radio-button><a-radio-button value="HOT">热门</a-radio-button><a-radio-button value="ESSENCE">精华</a-radio-button></a-radio-group></div>
      <div class="category-filter"><button type="button" :class="{ active: filters.boardId === undefined }" @click="selectBoard(undefined)">全部板块</button><button v-for="board in boards" :key="board.id" type="button" :class="{ active: String(filters.boardId) === String(board.id) }" @click="selectBoard(board.id)">{{ board.name }} · {{ board.topicCount || 0 }}</button></div>
    </div>
    <a-spin :spinning="loading">
      <div v-if="records.length" class="topic-list">
        <article v-for="topic in records" :key="topic.id" class="topic-card">
          <div class="topic-card-main">
            <div class="topic-flags"><a-tag v-if="Number(topic.topped)" color="blue"><PushpinFilled /> 置顶</a-tag><a-tag v-if="Number(topic.essence)" color="gold"><TrophyFilled /> 精华</a-tag><a-tag v-if="Number(topic.locked)"><LockOutlined /> 锁帖</a-tag><span>{{ boards.find(board => String(board.id) === String(topic.boardId))?.name || `板块 #${topic.boardId}` }}</span></div>
            <h2><router-link :to="`/forum/topics/${topic.id}`">{{ topic.title }}</router-link></h2>
            <p>{{ topic.content || '暂无正文摘要' }}</p>
            <div class="topic-meta"><span>用户 #{{ topic.authorId }}</span><time>{{ formatDate(topic.createTime, true) }}</time></div>
          </div>
          <div class="topic-stats"><span><EyeOutlined /> {{ topic.viewCount || 0 }}</span><span><CommentOutlined /> {{ topic.postCount || 0 }}</span><span><LikeOutlined /> {{ topic.likeCount || 0 }}</span></div>
        </article>
      </div>
      <a-empty v-else-if="!loading" class="forum-empty" description="暂无符合条件的公开主题"><a-button type="primary" @click="$router.push('/my/forum/topics/new')">发布第一个主题</a-button></a-empty>
    </a-spin>
    <a-pagination v-if="pagination.total > pagination.pageSize" v-model:current="pagination.current" class="list-pagination" :total="pagination.total" :page-size="pagination.pageSize" :show-total="pagination.showTotal" @change="changePage" />
  </div>
</template>
