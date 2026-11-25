<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { fileApi } from '../../api'
import { resourceApi, userResourceApi } from '../../api/resource'
import ImageUploadButton from '../../components/ImageUploadButton.vue'
import { accessTypeOptions } from '../../utils/resource'
import { errorText } from '../../utils/format'
import './resource.css'

const route = useRoute()
const router = useRouter()
const editingId = ref(route.params.id ? String(route.params.id) : '')
const formRef = ref()
const loading = ref(true)
const saving = ref(false)
const submitting = ref(false)
const categories = ref([])
const diskTypes = ref([])
const coverUrl = ref('')
const attachments = ref([])
const form = reactive({ title: '', summary: '', description: '', coverFileId: null, categoryId: undefined, tagIds: [], accessType: 'GUEST_FREE', pointsPrice: null, price: null, copyrightNotice: '', links: [] })
const categoryOptions = computed(() => categories.value.map((item) => ({ value: String(item.id), label: item.name })))
const diskOptions = computed(() => diskTypes.value.map((item) => ({ value: String(item.id), label: `${item.name}（${item.code}）` })))
const tagOptions = computed(() => form.tagIds.map((id, index) => ({ value: String(id), label: form.tagNames?.[index] || `标签 #${id}` })))
const rules = {
  title: [{ required: true, whitespace: true, message: '请输入资源标题' }, { max: 120, message: '标题不能超过120个字符' }],
  summary: [{ required: true, whitespace: true, message: '请输入资源摘要' }, { max: 500, message: '摘要不能超过500个字符' }],
  description: [{ required: true, whitespace: true, message: '请输入资源描述' }],
  categoryId: [{ required: true, message: '请选择资源分类' }],
  accessType: [{ required: true, message: '请选择访问类型' }],
  copyrightNotice: [{ required: true, whitespace: true, message: '请输入版权声明' }, { max: 1000, message: '版权声明不能超过1000个字符' }],
  tagIds: [{ validator: (_, value) => {
    if (value.length > 10) return Promise.reject(new Error('最多选择10个标签'))
    return value.every((id) => /^\d+$/.test(String(id))) ? Promise.resolve() : Promise.reject(new Error('标签必须使用有效的数字 ID'))
  } }],
  pointsPrice: [{ validator: (_, value) => form.accessType !== 'POINTS' || (Number.isInteger(value) && value >= 1) ? Promise.resolve() : Promise.reject(new Error('请输入不小于1的整数积分价格')) }],
  price: [{ validator: (_, value) => form.accessType !== 'PAID' || (Number(value) >= 0.01) ? Promise.resolve() : Promise.reject(new Error('请输入不小于0.01的现金价格')) }],
}

function newLink() { return { diskTypeId: undefined, url: '', extractionCode: '', linkName: '', sortOrder: 0 } }
if (!editingId.value) form.links.push(newLink())

onMounted(async () => {
  try {
    const [categoryData, diskData, detail] = await Promise.all([
      resourceApi.categories(), resourceApi.diskTypes(), editingId.value ? userResourceApi.detail(editingId.value) : Promise.resolve(null),
    ])
    categories.value = Array.isArray(categoryData) ? categoryData : []
    diskTypes.value = Array.isArray(diskData) ? diskData : []
    if (detail) {
      Object.assign(form, {
        title: detail.title || '', summary: detail.summary || '', description: detail.description || '', coverFileId: detail.coverFileId ?? null,
        categoryId: detail.categoryId == null ? undefined : String(detail.categoryId), tagIds: (detail.tagIds || []).map(String), tagNames: detail.tags || [],
        accessType: detail.accessType || 'GUEST_FREE', pointsPrice: detail.pointsPrice ?? null, price: detail.price == null ? null : Number(detail.price),
        copyrightNotice: detail.copyrightNotice || '', links: [],
      })
      attachments.value = (detail.attachmentFileIds || []).map((id) => ({ id: String(id), name: `附件 #${id}`, url: '' }))
    }
  } catch (error) {
    message.error(errorText(error))
  } finally {
    loading.value = false
  }
})

function addLink() {
  if (form.links.length < 20) form.links.push(newLink())
}
function validateLinks(requireOne) {
  if (requireOne && !form.links.length) {
    message.error('提交审核前至少需要一个有效网盘链接')
    return false
  }
  for (let index = 0; index < form.links.length; index += 1) {
    const link = form.links[index]
    if (!link.diskTypeId) { message.error(`请为第 ${index + 1} 个链接选择网盘类型`); return false }
    if (!link.linkName?.trim()) { message.error(`请填写第 ${index + 1} 个链接名称`); return false }
    if (link.linkName.trim().length > 100) { message.error(`第 ${index + 1} 个链接名称不能超过100个字符`); return false }
    if (!/^https:\/\/[^\s]+$/i.test(link.url || '')) { message.error(`第 ${index + 1} 个链接必须为有效 HTTPS 地址`); return false }
    if (link.url.length > 2000 || (link.extractionCode || '').length > 64) { message.error(`第 ${index + 1} 个链接或提取码超出长度限制`); return false }
  }
  return true
}
function addAttachment(data) {
  if (attachments.value.length >= 20) return
  attachments.value.push({ id: String(data.id), name: data.name || `附件 #${data.id}`, url: data.accessUrl || '' })
}
function onCoverUploaded(data) {
  form.coverFileId = data.id
  coverUrl.value = data.accessUrl
}
function payload() {
  return {
    title: form.title.trim(), summary: form.summary.trim(), description: form.description, coverFileId: form.coverFileId || null,
    attachmentFileIds: attachments.value.map((item) => item.id), categoryId: form.categoryId, tagIds: [...form.tagIds], accessType: form.accessType,
    pointsPrice: form.accessType === 'POINTS' ? form.pointsPrice : null, price: form.accessType === 'PAID' ? form.price : null,
    copyrightNotice: form.copyrightNotice.trim(),
    links: form.links.map((link, index) => ({ diskTypeId: link.diskTypeId, url: link.url.trim(), extractionCode: link.extractionCode?.trim() || null, linkName: link.linkName.trim(), sortOrder: Number(link.sortOrder ?? index) })),
  }
}
async function persist() {
  const data = payload()
  if (editingId.value) {
    await userResourceApi.update(editingId.value, data)
    return editingId.value
  }
  const result = await userResourceApi.create(data)
  const id = result?.id ?? result
  if (id === null || id === undefined || id === '') throw new Error('创建成功但响应缺少资源 ID')
  editingId.value = String(id)
  router.replace(`/my/resources/${editingId.value}/edit`)
  return editingId.value
}
async function saveDraft() {
  await formRef.value.validate()
  if (!validateLinks(false)) return
  saving.value = true
  try {
    await persist()
    message.success('资源草稿已保存')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    saving.value = false
  }
}
async function saveAndSubmit() {
  await formRef.value.validate()
  if (!validateLinks(true)) return
  Modal.confirm({
    title: '保存并提交审核',
    content: '将保存为新版本并提交审核；已有公开版本会在审核期间继续展示。确定继续吗？',
    okText: '确定提交', cancelText: '取消',
    async onOk() {
      submitting.value = true
      try {
        const id = await persist()
        await userResourceApi.submit(id)
        message.success('已保存并提交审核')
        router.push('/my/resources')
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
  <div class="content-page resource-editor-page">
    <div class="page-heading"><div><span class="eyebrow">RESOURCE EDITOR</span><h1>{{ editingId ? '编辑资源' : '发布资源' }}</h1><p>每次保存都会生成新的草稿版本，审核通过后才会切换公开内容。</p></div><a-button @click="router.push('/my/resources')">返回我的发布</a-button></div>
    <a-alert v-if="editingId" message="为保护网盘地址与提取码，历史敏感信息不会回显。新版本需要重新录入网盘链接后才能提交审核。" type="info" show-icon class="editor-notice" />
    <a-spin :spinning="loading">
      <a-form ref="formRef" :model="form" :rules="rules" layout="vertical" class="resource-editor-layout">
        <div class="resource-editor-main content-card">
          <a-form-item label="资源标题" name="title"><a-input v-model:value="form.title" :maxlength="120" placeholder="输入资源标题" /></a-form-item>
          <a-form-item label="资源摘要" name="summary"><a-textarea v-model:value="form.summary" :rows="4" :maxlength="500" show-count placeholder="简要说明资源内容与适用场景" /></a-form-item>
          <a-form-item label="资源描述" name="description"><a-textarea v-model:value="form.description" :rows="12" placeholder="详细介绍资源内容、目录与使用说明" /></a-form-item>
          <div class="resource-link-editor-heading"><h3>多网盘链接</h3><a-button size="small" :disabled="form.links.length >= 20" @click="addLink">添加链接</a-button></div>
          <a-empty v-if="!form.links.length" description="尚未录入新版本的网盘链接"><a-button type="primary" @click="addLink">添加网盘链接</a-button></a-empty>
          <section v-for="(link, index) in form.links" :key="index" class="resource-link-editor">
            <div class="resource-link-editor-heading"><strong>链接 {{ index + 1 }}</strong><a-button type="link" danger size="small" @click="form.links.splice(index, 1)">移除</a-button></div>
            <div class="resource-link-grid">
              <a-form-item label="网盘类型" required><a-select v-model:value="link.diskTypeId" :options="diskOptions" placeholder="请选择网盘类型" /></a-form-item>
              <a-form-item label="链接名称" required><a-input v-model:value="link.linkName" :maxlength="100" placeholder="如：主线路" /></a-form-item>
              <a-form-item class="full-row" label="HTTPS 网盘地址" required><a-input v-model:value="link.url" :maxlength="2000" placeholder="https://…" /></a-form-item>
              <a-form-item label="提取码（可选）"><a-input v-model:value="link.extractionCode" :maxlength="64" autocomplete="off" /></a-form-item>
              <a-form-item label="排序值"><a-input-number v-model:value="link.sortOrder" style="width: 100%" /></a-form-item>
            </div>
          </section>
        </div>
        <aside class="resource-editor-side">
          <section class="resource-editor-panel content-card"><h3>发布设置</h3>
            <a-form-item label="资源分类" name="categoryId"><a-select v-model:value="form.categoryId" :options="categoryOptions" placeholder="请选择分类" /></a-form-item>
            <a-form-item label="标签 ID" name="tagIds" extra="输入已有标签 ID 后回车，最多10个"><a-select v-model:value="form.tagIds" mode="tags" :options="tagOptions" placeholder="输入标签 ID" /></a-form-item>
            <a-form-item label="访问类型" name="accessType"><a-select v-model:value="form.accessType" :options="accessTypeOptions" /></a-form-item>
            <a-form-item v-if="form.accessType === 'POINTS'" label="积分价格" name="pointsPrice"><a-input-number v-model:value="form.pointsPrice" :min="1" :precision="0" style="width: 100%" /></a-form-item>
            <a-form-item v-if="form.accessType === 'PAID'" label="现金价格（元）" name="price"><a-input-number v-model:value="form.price" :min="0.01" :precision="2" style="width: 100%" /></a-form-item>
            <a-form-item label="版权声明" name="copyrightNotice"><a-textarea v-model:value="form.copyrightNotice" :rows="5" :maxlength="1000" show-count /></a-form-item>
          </section>
          <section class="resource-editor-panel content-card"><h3>图片与附件</h3>
            <a-form-item label="封面图片"><img v-if="coverUrl" class="resource-cover-preview" :src="coverUrl" alt="资源封面"><div class="resource-attachment-editor"><span>{{ form.coverFileId ? `封面文件 #${form.coverFileId}` : '暂未设置封面' }}</span><ImageUploadButton :label="form.coverFileId ? '更换封面' : '上传封面'" @uploaded="onCoverUploaded" /></div><a-button v-if="form.coverFileId" size="small" danger @click="form.coverFileId = null; coverUrl = ''">移除封面</a-button></a-form-item>
            <a-form-item label="图片附件" extra="最多20个，附件下载同样经过资源授权"><div v-for="(item, index) in attachments" :key="item.id" class="resource-attachment-editor"><span>{{ item.name }}</span><a-button type="link" danger size="small" @click="attachments.splice(index, 1)">移除</a-button></div><ImageUploadButton label="上传图片附件" :disabled="attachments.length >= 20" @uploaded="addAttachment" /></a-form-item>
          </section>
          <div class="resource-editor-actions"><a-button :loading="saving" :disabled="submitting" @click="saveDraft">保存草稿</a-button><a-button type="primary" :loading="submitting" :disabled="saving" @click="saveAndSubmit">保存并提交</a-button></div>
        </aside>
      </a-form>
    </a-spin>
  </div>
</template>
