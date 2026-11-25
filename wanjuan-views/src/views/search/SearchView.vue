<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import DOMPurify from 'dompurify'
import { searchApi } from '../../api/s6'
import { useAuthStore } from '../../stores/auth'
import { errorText } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const searched = ref(false)
const results = ref([])
const hotKeywords = ref([])
const history = ref([])
const filters = reactive({ keyword: '', type: undefined })
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showTotal: (total) => `共 ${total} 条结果` })
const typeOptions = [{ value: 'BLOG', label: '技术博客' }, { value: 'RESOURCE', label: '学习资源' }, { value: 'FORUM_TOPIC', label: '论坛主题' }]

function typeName(type) {
  return typeOptions.find((item) => item.value === type)?.label || type || '公开内容'
}

function safeHighlight(value) {
  return DOMPurify.sanitize(value || '', { ALLOWED_TAGS: ['mark'], ALLOWED_ATTR: [] })
}

function safeTitle(value) {
  return DOMPurify.sanitize(value || '', { ALLOWED_TAGS: [], ALLOWED_ATTR: [] })
}

async function loadSuggestions() {
  const requests = [searchApi.hotKeywords(10)]
  if (auth.isLoggedIn) requests.push(searchApi.history(20))
  const responses = await Promise.allSettled(requests)
  if (responses[0].status === 'fulfilled') hotKeywords.value = responses[0].value || []
  if (responses[1]?.status === 'fulfilled') history.value = responses[1].value || []
  const failed = responses.find((result) => result.status === 'rejected')
  if (failed) message.error(errorText(failed.reason))
}

async function executeSearch(page = 1, size = pagination.pageSize) {
  const keyword = filters.keyword.trim()
  if (!keyword) {
    results.value = []
    pagination.total = 0
    searched.value = false
    return
  }
  loading.value = true
  searched.value = true
  try {
    const data = await searchApi.search({ keyword, type: filters.type, page, size })
    results.value = data?.records || []
    pagination.current = Number(data?.page || page)
    pagination.pageSize = Number(data?.size || size)
    pagination.total = Number(data?.total || 0)
    if (auth.isLoggedIn) {
      searchApi.history(20).then((items) => { history.value = items || [] }).catch(() => {})
    }
  } catch (error) {
    results.value = []
    pagination.total = 0
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
}

function submit() {
  const keyword = filters.keyword.trim()
  router.push({ name: 'search', query: { ...(keyword ? { keyword } : {}), ...(filters.type ? { type: filters.type } : {}) } })
}

function useKeyword(keyword) {
  filters.keyword = keyword
  submit()
}

function changePage(page, size) {
  const query = { ...route.query, page: String(page) }
  if (size !== pagination.pageSize) query.size = String(size)
  router.push({ name: 'search', query })
}

function syncRoute() {
  filters.keyword = String(route.query.keyword || '')
  filters.type = typeOptions.some((item) => item.value === route.query.type) ? String(route.query.type) : undefined
  const page = Math.max(1, Number(route.query.page) || 1)
  const size = Math.min(100, Math.max(1, Number(route.query.size) || 10))
  executeSearch(page, size)
}

onMounted(() => {
  syncRoute()
  loadSuggestions()
})
watch(() => route.query, syncRoute)
</script>

<template>
  <div class="content-page search-page">
    <div class="search-heading"><span class="eyebrow">DISCOVER</span><h1>全站搜索</h1><p>统一检索社区公开博客、学习资源和论坛主题。</p></div>
    <section class="search-box-panel">
      <a-input-search v-model:value="filters.keyword" size="large" allow-clear :maxlength="100" placeholder="输入关键词，发现感兴趣的内容" enter-button="搜索" @search="submit" />
      <div class="search-type-filter"><span>搜索范围</span><a-radio-group v-model:value="filters.type" button-style="solid" @change="submit"><a-radio-button :value="undefined">全部</a-radio-button><a-radio-button v-for="item in typeOptions" :key="item.value" :value="item.value">{{ item.label }}</a-radio-button></a-radio-group></div>
    </section>
    <div v-if="!searched" class="search-discovery-grid">
      <a-card :bordered="false" class="content-card" title="热门搜索">
        <div v-if="hotKeywords.length" class="keyword-cloud"><button v-for="(keyword, index) in hotKeywords" :key="keyword" type="button" @click="useKeyword(keyword)"><span>{{ index + 1 }}</span>{{ keyword }}</button></div>
        <a-empty v-else description="暂无热门搜索词" />
      </a-card>
      <a-card v-if="auth.isLoggedIn" :bordered="false" class="content-card" title="我的搜索历史">
        <div v-if="history.length" class="keyword-cloud history-cloud"><button v-for="keyword in history" :key="keyword" type="button" @click="useKeyword(keyword)">{{ keyword }}</button></div>
        <a-empty v-else description="暂无搜索历史" />
      </a-card>
      <a-card v-else :bordered="false" class="content-card" title="搜索历史"><a-empty description="登录后可保存并查看搜索历史"><a-button type="primary" @click="router.push({ name: 'login', query: { redirect: route.fullPath } })">立即登录</a-button></a-empty></a-card>
    </div>
    <section v-else class="content-card search-results-card">
      <div class="results-heading"><div><h2>“{{ filters.keyword }}”的搜索结果</h2><p v-if="!loading">共找到 {{ pagination.total }} 条公开内容</p></div><a-button @click="filters.keyword = ''; submit()">清空搜索</a-button></div>
      <a-spin :spinning="loading">
        <div v-if="results.length" class="search-result-list">
          <article v-for="result in results" :key="`${result.contentType}-${result.contentId}`" class="search-result-item">
            <a-tag color="blue">{{ typeName(result.contentType) }}</a-tag>
            <router-link class="result-title" :to="result.url" v-html="safeTitle(result.title)" />
            <p v-html="safeHighlight(result.highlight)" />
            <router-link class="result-link" :to="result.url">查看完整内容 →</router-link>
          </article>
        </div>
        <a-empty v-else-if="!loading" description="没有找到匹配的公开内容，试试其他关键词" />
      </a-spin>
      <a-pagination v-if="pagination.total > pagination.pageSize" class="list-pagination" :current="pagination.current" :page-size="pagination.pageSize" :total="pagination.total" :show-size-changer="true" :show-total="pagination.showTotal" @change="changePage" />
    </section>
  </div>
</template>

<style scoped>
.search-page { max-width: 1000px; }
.search-heading { margin-bottom: 26px; text-align: center; }
.search-heading h1 { margin: 0 0 10px; color: var(--ink); font-size: 34px; }
.search-heading p { color: var(--muted); }
.search-box-panel { max-width: 820px; padding: 28px 32px; margin: 0 auto 26px; border: 1px solid var(--line); border-radius: 16px; background: #fff; box-shadow: 0 12px 34px rgba(27,45,80,.06); }
.search-type-filter { display: flex; margin-top: 20px; align-items: center; flex-wrap: wrap; gap: 14px; }
.search-type-filter > span { color: var(--muted); font-size: 12px; }
.search-discovery-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; }
.keyword-cloud { display: flex; flex-direction: column; gap: 5px; }
.keyword-cloud button { display: flex; padding: 10px 12px; align-items: center; gap: 12px; border: 0; border-radius: 8px; color: #44516a; text-align: left; background: transparent; cursor: pointer; }
.keyword-cloud button:hover { color: var(--primary); background: #f4f7fd; }
.keyword-cloud button span { display: inline-flex; width: 22px; height: 22px; align-items: center; justify-content: center; border-radius: 6px; color: #7d899e; background: #eef1f6; font-size: 11px; }
.keyword-cloud button:nth-child(-n+3) span { color: #fff; background: #6683d9; }
.history-cloud { flex-direction: row; flex-wrap: wrap; }
.history-cloud button { border: 1px solid var(--line); border-radius: 18px; }
.search-results-card { padding: 6px 30px 26px; }
.results-heading { display: flex; padding: 22px 0; align-items: center; justify-content: space-between; gap: 18px; border-bottom: 1px solid var(--line); }
.results-heading h2 { margin: 0 0 5px; color: var(--ink); font-size: 18px; }
.results-heading p { margin: 0; color: var(--muted); font-size: 12px; }
.search-result-item { padding: 24px 0; border-bottom: 1px solid var(--line); }
.result-title { display: inline-block; margin: 10px 0; color: var(--ink); font-size: 20px; font-weight: 600; line-height: 1.5; }
.result-title:hover { color: var(--primary); }
.search-result-item p { margin: 0 0 10px; color: #647087; line-height: 1.8; }
.search-result-item :deep(mark) { padding: 0 2px; color: #b53545; background: #fff0c7; }
.result-link { font-size: 13px; }
@media (max-width: 650px) { .search-box-panel { padding: 22px 18px; } .search-discovery-grid { grid-template-columns: 1fr; } .search-results-card { padding-inline: 18px; } .results-heading { align-items: flex-start; flex-direction: column; } }
</style>
