<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { blogApi, fileApi, userBlogApi } from '../../api'
import { blogCover, collectionOf, tagId, tagName } from '../../utils/blog'
import { errorText, renderMarkdown } from '../../utils/format'

const route = useRoute()
const router = useRouter()
const editingId = ref(route.params.id ? String(route.params.id) : '')
const formRef = ref()
const loading = ref(true)
const saving = ref(false)
const submitting = ref(false)
const activeTab = ref('edit')
const categories = ref([])
const extraTagOptions = ref([])
const coverUploading = ref(false)
const contentUploading = ref(false)
const coverUrl = ref('')
const form = reactive({ title: '', summary: '', coverFileId: null, content: '', categoryId: undefined, tagIds: [], type: 'ORIGINAL', sourceName: '', sourceUrl: '' })
const previewHtml = computed(() => renderMarkdown(form.content))
const categoryOptions = computed(() => categories.value.map((item) => ({ value: String(item.id), label: item.name })))
const tagOptions = computed(() => {
  const options = []
  categories.value.forEach((category) => (category.tags || []).forEach((tag) => options.push({ value: String(tagId(tag)), label: tagName(tag) })))
  options.push(...extraTagOptions.value)
  return [...new Map(options.map((item) => [item.value, item])).values()]
})
const rules = {
  title: [{ required: true, whitespace: true, message: '请输入博客标题' }, { max: 120, message: '标题不能超过120个字符' }],
  summary: [{ max: 500, message: '摘要不能超过500个字符' }],
  content: [{ required: true, whitespace: true, message: '请输入 Markdown 正文' }],
  categoryId: [{ required: true, message: '请选择分类' }],
  tagIds: [{ validator: (_, value) => value.length <= 10 ? Promise.resolve() : Promise.reject(new Error('最多选择10个标签')) }],
  sourceName: [{ validator: (_, value) => form.type !== 'REPRINT' || value?.trim() ? Promise.resolve() : Promise.reject(new Error('转载博客请填写来源名称')) }],
  sourceUrl: [{ validator: (_, value) => {
    if (form.type !== 'REPRINT') return Promise.resolve()
    if (!value?.trim()) return Promise.reject(new Error('转载博客请填写来源地址'))
    return /^https?:\/\//i.test(value) ? Promise.resolve() : Promise.reject(new Error('来源地址必须为 HTTP 或 HTTPS 地址'))
  } }],
}

onMounted(async () => {
  try {
    const [categoryData, blogData] = await Promise.all([
      blogApi.categories(),
      editingId.value ? userBlogApi.detail(editingId.value) : Promise.resolve(null),
    ])
    categories.value = collectionOf(categoryData)
    if (blogData) {
      Object.assign(form, {
        title: blogData.title || '', summary: blogData.summary || '', coverFileId: blogData.coverFileId ?? null,
        content: blogData.content || '', categoryId: blogData.categoryId !== null && blogData.categoryId !== undefined ? String(blogData.categoryId) : undefined,
        tagIds: (blogData.tagIds || blogData.tags || []).map((tag) => String(tagId(tag))).filter(Boolean),
        type: blogData.type || blogData.originalType || 'ORIGINAL', sourceName: blogData.sourceName || '', sourceUrl: blogData.sourceUrl || '',
      })
      extraTagOptions.value = (blogData.tags || []).map((tag) => ({ value: String(tagId(tag)), label: tagName(tag) }))
      coverUrl.value = blogCover(blogData)
    }
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
})

function validateImage(file) {
  if (!['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)) {
    message.error('仅支持 JPG、PNG、GIF、WebP 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

async function upload(file) {
  const data = await fileApi.uploadImage(file)
  if (!data?.id || !data?.accessUrl) throw new Error('上传响应缺少图片 ID 或访问地址')
  return data
}

async function uploadCover({ file, onSuccess, onError }) {
  coverUploading.value = true
  try {
    const data = await upload(file)
    form.coverFileId = data.id
    coverUrl.value = data.accessUrl
    message.success('封面上传成功')
    onSuccess?.(data)
  } catch (error) {
    message.error(errorText(error))
    onError?.(error)
  } finally {
    coverUploading.value = false
  }
}

async function uploadContentImage({ file, onSuccess, onError }) {
  contentUploading.value = true
  try {
    const data = await upload(file)
    const markdown = `\n![${file.name.replace(/[\[\]]/g, '')}](${data.accessUrl})\n`
    form.content = `${form.content || ''}${markdown}`
    message.success('图片已上传并插入正文')
    onSuccess?.(data)
  } catch (error) {
    message.error(errorText(error))
    onError?.(error)
  } finally {
    contentUploading.value = false
  }
}

function payload() {
  return {
    title: form.title.trim(), summary: form.summary.trim(), coverFileId: form.coverFileId || null,
    content: form.content, categoryId: form.categoryId, tagIds: [...form.tagIds], type: form.type,
    sourceName: form.type === 'REPRINT' ? form.sourceName.trim() : null,
    sourceUrl: form.type === 'REPRINT' ? form.sourceUrl.trim() : null,
  }
}

async function persist() {
  const data = payload()
  if (editingId.value) {
    await userBlogApi.update(editingId.value, data)
    return editingId.value
  }
  const result = await userBlogApi.create(data)
  const id = result?.id ?? result
  if (id === null || id === undefined || id === '') throw new Error('创建成功但响应缺少博客 ID')
  editingId.value = String(id)
  router.replace(`/my/blogs/${editingId.value}/edit`)
  return editingId.value
}

async function saveDraft() {
  await formRef.value.validate()
  saving.value = true
  try {
    await persist()
    message.success('草稿已保存')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}

async function saveAndSubmit() {
  await formRef.value.validate()
  Modal.confirm({
    title: '保存并提交审核',
    content: '将保存当前内容并把最新版本提交审核，确定继续吗？',
    okText: '确定提交', cancelText: '取消',
    async onOk() {
      submitting.value = true
      try {
        const id = await persist()
        await userBlogApi.submit(id)
        message.success('已保存并提交审核')
        router.push('/my/blogs')
      } catch (error) {
        message.error(errorText(error))
      } finally {
        submitting.value = false
      }
    },
  })
}
</script>

<template>
  <div class="content-page blog-editor-page">
    <div class="page-heading"><div><span class="eyebrow">MARKDOWN EDITOR</span><h1>{{ editingId ? '编辑博客' : '创作博客' }}</h1><p>保存草稿不会自动公开，提交审核通过后才会发布。</p></div><a-button @click="router.push('/my/blogs')">返回我的博客</a-button></div>
    <a-spin :spinning="loading">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" class="editor-layout">
        <div class="editor-main content-card">
          <a-form-item name="title"><a-input v-model:value="form.title" class="editor-title-input" :bordered="false" :maxlength="120" placeholder="输入博客标题…" /></a-form-item>
          <a-tabs v-model:active-key="activeTab" class="editor-tabs">
            <a-tab-pane key="edit" tab="Markdown 编辑">
              <div class="content-upload-bar"><span>支持 Markdown 语法，图片将上传后插入正文末尾</span><a-upload accept="image/jpeg,image/png,image/gif,image/webp" :show-upload-list="false" :before-upload="validateImage" :custom-request="uploadContentImage"><a-button size="small" :loading="contentUploading">上传正文图片</a-button></a-upload></div>
              <a-form-item name="content" class="content-editor-item"><a-textarea v-model:value="form.content" class="markdown-textarea" :rows="22" placeholder="# 从这里开始记录你的实践…" /></a-form-item>
            </a-tab-pane>
            <a-tab-pane key="preview" tab="安全预览"><div v-if="form.content" class="markdown-body editor-preview" v-html="previewHtml" /><a-empty v-else description="暂无正文内容" /></a-tab-pane>
          </a-tabs>
        </div>
        <aside class="editor-sidebar">
          <section class="content-card editor-panel"><h3>发布设置</h3><a-form-item label="分类" name="categoryId"><a-select v-model:value="form.categoryId" :options="categoryOptions" placeholder="请选择分类" /></a-form-item><a-form-item label="标签 ID" name="tagIds" extra="选择已有标签，或输入标签 ID 后回车，最多10个"><a-select v-model:value="form.tagIds" mode="tags" :options="tagOptions" :token-separators="[',']" placeholder="输入标签 ID" /></a-form-item><a-form-item label="内容类型" name="type"><a-radio-group v-model:value="form.type"><a-radio-button value="ORIGINAL">原创</a-radio-button><a-radio-button value="REPRINT">转载</a-radio-button></a-radio-group></a-form-item><template v-if="form.type === 'REPRINT'"><a-form-item label="来源名称" name="sourceName"><a-input v-model:value="form.sourceName" :maxlength="100" /></a-form-item><a-form-item label="来源地址" name="sourceUrl"><a-input v-model:value="form.sourceUrl" placeholder="https://…" /></a-form-item></template></section>
          <section class="content-card editor-panel"><h3>封面与摘要</h3><a-form-item label="博客封面"><div v-if="coverUrl" class="cover-preview"><img :src="coverUrl" alt="博客封面" /><a-button size="small" danger @click="form.coverFileId = null; coverUrl = ''">移除封面</a-button></div><a-upload accept="image/jpeg,image/png,image/gif,image/webp" :show-upload-list="false" :before-upload="validateImage" :custom-request="uploadCover"><a-button block :loading="coverUploading">{{ coverUrl ? '更换封面' : '上传封面' }}</a-button></a-upload></a-form-item><a-form-item label="摘要" name="summary"><a-textarea v-model:value="form.summary" :rows="5" :maxlength="500" show-count placeholder="简要介绍文章内容" /></a-form-item></section>
          <div class="editor-actions"><a-button block :loading="saving" :disabled="submitting" @click="saveDraft">保存草稿</a-button><a-button type="primary" block :loading="submitting" :disabled="saving" @click="saveAndSubmit">保存并提交审核</a-button></div>
        </aside>
      </a-form>
    </a-spin>
  </div>
</template>
