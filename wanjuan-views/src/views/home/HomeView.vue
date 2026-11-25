<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { CommentOutlined, DislikeOutlined, EyeOutlined, LikeOutlined, NotificationOutlined, RightOutlined } from '@ant-design/icons-vue'
import { blogApi, operationApi } from '../../api'
import { forumApi } from '../../api/s3'
import { useAuthStore } from '../../stores/auth'
import { blogAuthor, blogTags, collectionOf, tagName } from '../../utils/blog'
import { displayName, errorText, formatDate } from '../../utils/format'

const PAGE_SIZE = 8
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const contentType = ref('blog')
const sort = ref('LATEST')
const selectedCategoryId = ref()
const selectedBoardId = ref()
const categories = ref([])
const boards = ref([])
const announcements = ref([])
const records = ref([])
let requestSequence = 0

const avatarText = computed(() => displayName(auth.user).slice(0, 1).toUpperCase())
const moreLink = computed(() => contentType.value === 'blog' ? '/blogs' : '/forum')
const emptyText = computed(() => contentType.value === 'blog' ? '当前分类暂无公开博客' : '当前板块暂无公开主题')
const feedItems = computed(() => records.value.map((item) => contentType.value === 'blog' ? blogItem(item) : topicItem(item)))

function excerpt(value, fallback, maxLength = 130) {
  const text = String(value || '')
    .replace(/<[^>]*>/g, ' ')
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
    .replace(/[`#>*_~|]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  if (!text) return fallback
  return text.length > maxLength ? `${text.slice(0, maxLength)}…` : text
}

function categoryName(id) {
  return categories.value.find((category) => String(category.id) === String(id))?.name || '技术博客'
}

function boardName(id) {
  return boards.value.find((board) => String(board.id) === String(id))?.name || '论坛讨论'
}

function blogItem(blog) {
  return {
    id: blog.id,
    url: `/blogs/${blog.id}`,
    title: blog.title,
    summary: excerpt(blog.summary, '作者暂未填写摘要，点击查看完整内容。'),
    channel: blog.categoryName || blog.category?.name || categoryName(blog.categoryId),
    author: blogAuthor(blog),
    avatar: blog.authorAvatar || blog.author?.avatar,
    time: formatDate(blog.publishedAt || blog.publishTime || blog.createdAt || blog.createTime),
    views: Number(blog.viewCount || 0),
    likes: Number(blog.likeCount || 0),
    dislikes: Number(blog.dislikeCount || 0),
    comments: Number(blog.commentCount || 0),
    tags: blogTags(blog).slice(0, 3).map(tagName),
  }
}

function topicItem(topic) {
  const author = topic.authorNickname || topic.authorName || topic.author?.nickname || `用户 #${topic.authorId}`
  return {
    id: topic.id,
    url: `/forum/topics/${topic.id}`,
    title: topic.title,
    summary: excerpt(topic.content, '作者暂未填写正文摘要，点击查看完整讨论。'),
    channel: boardName(topic.boardId),
    author,
    avatar: topic.authorAvatar || topic.author?.avatar,
    time: formatDate(topic.createTime, true),
    views: Number(topic.viewCount || 0),
    likes: Number(topic.likeCount || 0),
    dislikes: Number(topic.dislikeCount || 0),
    comments: Number(topic.postCount ?? topic.commentCount ?? 0),
    topped: Number(topic.topped),
    essence: Number(topic.essence),
    tags: [],
  }
}

async function loadContent() {
  const sequence = ++requestSequence
  const requestedType = contentType.value
  const requestedSort = sort.value
  loading.value = true
  try {
    if (requestedType === 'blog') {
      const data = await blogApi.list({ page: 1, size: PAGE_SIZE, categoryId: selectedCategoryId.value })
      if (sequence !== requestSequence) return
      const items = collectionOf(data)
      records.value = requestedSort === 'HOT'
        ? [...items].sort((a, b) => Number(b.viewCount || 0) - Number(a.viewCount || 0))
        : items
    } else {
      const data = await forumApi.topics({ page: 1, size: PAGE_SIZE, boardId: selectedBoardId.value, sort: requestedSort })
      if (sequence !== requestSequence) return
      records.value = collectionOf(data)
    }
  } catch (error) {
    if (sequence !== requestSequence) return
    records.value = []
    message.error(errorText(error))
  } finally {
    if (sequence === requestSequence) loading.value = false
  }
}

async function loadMetadata() {
  const responses = await Promise.allSettled([
    blogApi.categories(),
    forumApi.boards(),
    operationApi.announcements(5),
  ])
  if (responses[0].status === 'fulfilled') categories.value = collectionOf(responses[0].value)
  if (responses[1].status === 'fulfilled') boards.value = collectionOf(responses[1].value)
  if (responses[2].status === 'fulfilled') announcements.value = collectionOf(responses[2].value)
  const failed = responses.find((response) => response.status === 'rejected')
  if (failed) message.error(errorText(failed.reason))
}

function switchContent(type) {
  if (contentType.value === type) return
  contentType.value = type
  loadContent()
}

function selectCategory(id) {
  contentType.value = 'blog'
  selectedCategoryId.value = id
  loadContent()
}

function selectBoard(id) {
  contentType.value = 'forum'
  selectedBoardId.value = id
  loadContent()
}

onMounted(() => {
  loadContent()
  loadMetadata()
  if (auth.isLoggedIn && !auth.loaded) auth.loadUser().catch(() => {})
})
</script>

<template>
  <div class="community-home">
    <div class="home-container">
      <section class="channel-panel">
        <div class="channel-heading">
          <div><h1>社区内容</h1><p>发现技术文章，参与开发者讨论</p></div>
          <div class="channel-actions"><a-button type="primary" @click="router.push('/my/blogs/new')">创作博客</a-button><a-button @click="router.push('/my/forum/topics/new')">发布主题</a-button></div>
        </div>
        <div class="taxonomy-row">
          <span class="taxonomy-label">博客分类</span>
          <div class="taxonomy-options">
            <button type="button" :class="{ active: contentType === 'blog' && selectedCategoryId === undefined }" @click="selectCategory(undefined)">全部</button>
            <button v-for="category in categories" :key="category.id" type="button" :class="{ active: contentType === 'blog' && String(selectedCategoryId) === String(category.id) }" @click="selectCategory(category.id)">{{ category.name }}</button>
          </div>
          <router-link class="taxonomy-more" to="/blogs">博客首页 <RightOutlined /></router-link>
        </div>
        <div class="taxonomy-row">
          <span class="taxonomy-label">论坛板块</span>
          <div class="taxonomy-options">
            <button type="button" :class="{ active: contentType === 'forum' && selectedBoardId === undefined }" @click="selectBoard(undefined)">全部</button>
            <button v-for="board in boards" :key="board.id" type="button" :class="{ active: contentType === 'forum' && String(selectedBoardId) === String(board.id) }" @click="selectBoard(board.id)">{{ board.name }}<span v-if="board.topicCount !== undefined" class="board-count">{{ board.topicCount || 0 }}</span></button>
          </div>
          <router-link class="taxonomy-more" to="/forum">论坛首页 <RightOutlined /></router-link>
        </div>
      </section>

      <div class="home-layout">
        <section class="feed-panel">
          <header class="feed-toolbar">
            <div class="feed-tabs" aria-label="首页内容类型">
              <button type="button" :class="{ active: contentType === 'blog' }" @click="switchContent('blog')">技术博客</button>
              <button type="button" :class="{ active: contentType === 'forum' }" @click="switchContent('forum')">论坛讨论</button>
            </div>
            <a-radio-group v-model:value="sort" size="small" button-style="solid" @change="loadContent">
              <a-radio-button value="LATEST">最新</a-radio-button>
              <a-radio-button value="HOT">热门</a-radio-button>
            </a-radio-group>
          </header>
          <a-spin :spinning="loading">
            <div v-if="feedItems.length" class="feed-list">
              <article v-for="item in feedItems" :key="`${contentType}-${item.id}`" class="feed-item">
                <div class="feed-item-top"><span class="feed-channel">{{ item.channel }}</span><span v-if="item.topped" class="feed-flag">置顶</span><span v-if="item.essence" class="feed-flag essence">精华</span></div>
                <h2><router-link :to="item.url">{{ item.title }}</router-link></h2>
                <p class="feed-summary">{{ item.summary }}</p>
                <div v-if="item.tags.length" class="feed-tags"><span v-for="tag in item.tags" :key="tag"># {{ tag }}</span></div>
                <footer class="feed-item-footer">
                  <div class="feed-author"><a-avatar :src="item.avatar" :size="24">{{ item.author.slice(0, 1).toUpperCase() }}</a-avatar><span>{{ item.author }}</span><time>{{ item.time }}</time></div>
                  <div class="feed-stats" aria-label="内容互动数据">
                    <span title="浏览量"><EyeOutlined /> {{ item.views }}</span>
                    <span title="点赞数"><LikeOutlined /> {{ item.likes }}</span>
                    <span title="点踩数"><DislikeOutlined /> {{ item.dislikes }}</span>
                    <span title="评论数"><CommentOutlined /> {{ item.comments }}</span>
                  </div>
                </footer>
              </article>
            </div>
            <div v-else-if="!loading" class="feed-empty"><p>{{ emptyText }}</p><a-button size="small" @click="loadContent">重新加载</a-button></div>
          </a-spin>
          <footer class="feed-more"><router-link :to="moreLink">查看全部{{ contentType === 'blog' ? '博客' : '主题' }} <RightOutlined /></router-link></footer>
        </section>

        <aside class="home-sidebar">
          <section class="sidebar-card user-card">
            <template v-if="auth.isLoggedIn">
              <div class="user-profile"><a-avatar :src="auth.user?.avatar" :size="46">{{ avatarText }}</a-avatar><div><h2>{{ displayName(auth.user) }}</h2><p>{{ auth.user?.signature || '记录学习，分享实践。' }}</p></div></div>
              <div class="user-actions"><router-link to="/my/blogs/new">写博客</router-link><router-link to="/my/forum/topics/new">发讨论</router-link></div>
              <router-link class="user-center-link" to="/my">进入个人中心 <RightOutlined /></router-link>
            </template>
            <template v-else>
              <h2>加入万卷技术社区</h2>
              <p class="guest-description">登录后发布博客、参与讨论，管理你的社区内容。</p>
              <a-button type="primary" block @click="router.push('/login')">立即登录</a-button>
              <a-button block class="register-button" @click="router.push('/register')">注册账号</a-button>
            </template>
          </section>

          <section class="sidebar-card announcement-card">
            <h2><NotificationOutlined /> 社区公告</h2>
            <div v-if="announcements.length" class="announcement-list">
              <article v-for="announcement in announcements" :key="announcement.id">
                <h3>{{ announcement.title }}</h3>
                <p>{{ announcement.content }}</p>
                <time>{{ formatDate(announcement.publishStart || announcement.createTime) }}</time>
              </article>
            </div>
            <p v-else class="announcement-empty">暂无社区公告</p>
          </section>
        </aside>
      </div>
    </div>
  </div>
</template>

<style scoped>
.community-home { min-height: calc(100vh - 132px); background: #f4f6f9; }
.home-container { max-width: 1180px; padding: 22px 28px 52px; margin: 0 auto; }
.channel-panel, .feed-panel, .sidebar-card { border: 1px solid #e7eaf0; border-radius: 10px; background: #fff; }
.channel-panel { padding: 18px 22px 8px; margin-bottom: 18px; }
.channel-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding-bottom: 15px; border-bottom: 1px solid #edf0f4; }
.channel-heading h1 { margin: 0 0 4px; color: var(--ink); font-size: 20px; line-height: 1.4; }
.channel-heading p { margin: 0; color: var(--muted); font-size: 13px; }
.channel-actions { display: flex; flex: 0 0 auto; gap: 8px; }
.taxonomy-row { display: grid; grid-template-columns: 72px minmax(0, 1fr) 76px; align-items: start; gap: 12px; padding: 12px 0 4px; }
.taxonomy-label { padding-top: 6px; color: #8a93a5; font-size: 13px; }
.taxonomy-options { display: flex; min-width: 0; flex-wrap: wrap; gap: 3px 5px; }
.taxonomy-options button { padding: 5px 10px; border: 0; border-radius: 5px; color: #515b6d; background: transparent; cursor: pointer; font-size: 13px; line-height: 20px; }
.taxonomy-options button:hover { color: var(--primary); background: #f2f5fc; }
.taxonomy-options button.active { color: #fff; background: var(--primary); }
.board-count { margin-left: 5px; opacity: .72; font-size: 11px; }
.taxonomy-more { display: inline-flex; align-items: center; justify-content: flex-end; gap: 3px; padding-top: 6px; color: #7c8799; font-size: 12px; white-space: nowrap; }
.taxonomy-more:hover { color: var(--primary); }
.taxonomy-more :deep(svg) { width: 10px; height: 10px; }
.home-layout { display: grid; grid-template-columns: minmax(0, 1fr) 290px; align-items: start; gap: 18px; }
.feed-panel { min-width: 0; overflow: hidden; }
.feed-toolbar { display: flex; height: 58px; padding: 0 22px; align-items: center; justify-content: space-between; gap: 18px; border-bottom: 1px solid #e9edf2; }
.feed-tabs { display: flex; height: 100%; align-items: stretch; gap: 26px; }
.feed-tabs button { position: relative; padding: 0 2px; border: 0; color: #6a7486; background: transparent; cursor: pointer; font-size: 15px; }
.feed-tabs button.active { color: var(--primary); font-weight: 600; }
.feed-tabs button.active::after { content: ''; position: absolute; right: 0; bottom: 0; left: 0; height: 2px; border-radius: 2px; background: var(--primary); }
.feed-item { padding: 20px 22px 17px; border-bottom: 1px solid #edf0f4; }
.feed-item-top { display: flex; min-height: 20px; align-items: center; gap: 7px; margin-bottom: 5px; }
.feed-channel { color: var(--primary); font-size: 12px; }
.feed-flag { padding: 1px 5px; border-radius: 3px; color: #3569c7; background: #edf4ff; font-size: 11px; }
.feed-flag.essence { color: #ad6a00; background: #fff6df; }
.feed-item h2 { margin: 0 0 8px; font-size: 18px; line-height: 1.5; }
.feed-item h2 a { display: block; overflow: hidden; color: #20293a; text-overflow: ellipsis; white-space: nowrap; }
.feed-item h2 a:hover { color: var(--primary); }
.feed-summary { display: -webkit-box; overflow: hidden; margin: 0 0 13px; color: #697386; font-size: 13px; line-height: 1.75; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.feed-tags { display: flex; flex-wrap: wrap; gap: 10px; margin: -3px 0 12px; color: #7c8799; font-size: 12px; }
.feed-item-footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.feed-author { display: flex; min-width: 0; align-items: center; gap: 7px; color: #707b8e; font-size: 12px; }
.feed-author > span { max-width: 110px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.feed-author time { color: #9aa3b2; white-space: nowrap; }
.feed-stats { display: flex; flex: 0 0 auto; align-items: center; gap: 15px; color: #8a94a5; font-size: 12px; }
.feed-stats > span { display: inline-flex; align-items: center; gap: 4px; }
.feed-more { padding: 13px 22px; text-align: center; }
.feed-more a { display: inline-flex; align-items: center; gap: 4px; color: #778295; font-size: 13px; }
.feed-more a:hover { color: var(--primary); }
.feed-empty { display: flex; min-height: 320px; align-items: center; justify-content: center; flex-direction: column; gap: 12px; color: #8a94a5; }
.feed-empty p { margin: 0; }
.home-sidebar { display: flex; min-width: 0; flex-direction: column; gap: 18px; }
.sidebar-card { padding: 20px; }
.user-card h2, .announcement-card h2 { margin: 0; color: #252e3f; font-size: 16px; }
.user-profile { display: flex; align-items: center; gap: 12px; }
.user-profile > div { min-width: 0; }
.user-profile h2 { overflow: hidden; margin-bottom: 5px; text-overflow: ellipsis; white-space: nowrap; }
.user-profile p { display: -webkit-box; overflow: hidden; margin: 0; color: #8a94a5; font-size: 12px; line-height: 1.6; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.user-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 9px; margin: 18px 0 14px; }
.user-actions a { padding: 7px 8px; border: 1px solid #dce4f7; border-radius: 5px; color: var(--primary); text-align: center; background: #f6f8fe; font-size: 13px; }
.user-actions a:hover { background: #edf2ff; }
.user-center-link { display: flex; align-items: center; justify-content: space-between; color: #7d8798; font-size: 12px; }
.guest-description { margin: 12px 0 18px; color: #7c8799; font-size: 13px; line-height: 1.75; }
.register-button { margin-top: 10px; }
.announcement-card h2 { display: flex; align-items: center; gap: 7px; padding-bottom: 14px; border-bottom: 1px solid #edf0f4; }
.announcement-list article { padding: 14px 0; border-bottom: 1px solid #f0f2f5; }
.announcement-list article:last-child { padding-bottom: 0; border-bottom: 0; }
.announcement-list h3 { overflow: hidden; margin: 0 0 7px; color: #424c5e; font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.announcement-list p { display: -webkit-box; overflow: hidden; margin: 0 0 6px; color: #7d8798; font-size: 12px; line-height: 1.65; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.announcement-list time { color: #a0a8b5; font-size: 11px; }
.announcement-empty { margin: 24px 0 10px; color: #98a1af; text-align: center; font-size: 12px; }
@media (max-width: 960px) {
  .home-layout { grid-template-columns: 1fr; }
  .home-sidebar { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 680px) {
  .home-container { padding: 14px 12px 36px; }
  .channel-panel { padding: 15px 14px 8px; margin-bottom: 12px; }
  .channel-heading { align-items: flex-start; gap: 12px; }
  .channel-actions { gap: 6px; }
  .channel-actions :deep(.ant-btn) { padding-inline: 10px; font-size: 12px; }
  .taxonomy-row { grid-template-columns: 1fr auto; gap: 4px 10px; }
  .taxonomy-label { padding-top: 0; }
  .taxonomy-options { grid-column: 1 / -1; grid-row: 2; flex-wrap: nowrap; overflow-x: auto; padding-bottom: 4px; }
  .taxonomy-options button { flex: 0 0 auto; }
  .taxonomy-more { grid-column: 2; grid-row: 1; padding-top: 0; }
  .feed-toolbar { height: 54px; padding: 0 15px; }
  .feed-tabs { gap: 18px; }
  .feed-item { padding: 17px 15px 15px; }
  .feed-item h2 { font-size: 16px; }
  .feed-item-footer { align-items: flex-start; flex-direction: column; gap: 10px; }
  .feed-stats { width: 100%; justify-content: space-between; gap: 8px; }
  .home-sidebar { grid-template-columns: 1fr; gap: 12px; }
}
</style>
