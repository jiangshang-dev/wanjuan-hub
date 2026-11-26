<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { resourceApi, userResourceApi } from '../../api/resource'
import { accessTypeText, resourcePrice, resourceStatus } from '../../utils/resource'
import { errorText, formatDate } from '../../utils/format'
import './resource.css'

const route = useRoute()
const loading = ref(true)
const loadError = ref(null)
const resource = ref(null)
const categories = ref([])
const linksLoading = ref(false)
const links = ref([])
const attachmentLoadingId = ref('')
const attachments = ref({})
const redeeming = ref(false)
const claiming = ref(false)
const reportOpen = ref(false)
const isMemberResource = computed(() => ['MEMBER', 'PAID'].includes(resource.value?.accessType))
const reportSaving = ref(false)
const reportFormRef = ref()
const reportForm = reactive({ linkId: undefined, reason: '' })
const reportRules = { reason: [{ required: true, whitespace: true, message: '请说明失效情况' }, { max: 1000, message: '失效原因不能超过1000个字符' }] }

function categoryName(id) {
  return categories.value.find((item) => String(item.id) === String(id))?.name || '资源'
}
function safeUrl(value) {
  return /^https:\/\/[^\s]+$/i.test(value || '') ? value : ''
}
function safeAttachmentUrl(value) {
  return /^https?:\/\/[^\s]+$/i.test(value || '') ? value : ''
}

async function load() {
  loading.value = true
  loadError.value = null
  resource.value = null
  links.value = []
  attachments.value = {}
  try {
    const [detail, categoryData] = await Promise.all([
      resourceApi.detail(route.params.id),
      resourceApi.categories().catch(() => []),
    ])
    resource.value = detail
    categories.value = Array.isArray(categoryData) ? categoryData : []
    document.title = `${detail?.title || '资源详情'} - 万卷技术社区`
  } catch (error) {
    loadError.value = error
  } finally {
    loading.value = false
  }
}

async function loadLinks() {
  linksLoading.value = true
  links.value = []
  try {
    const data = await resourceApi.links(route.params.id)
    links.value = Array.isArray(data) ? data : []
    if (!links.value.length) message.info('当前公开版本暂无可用网盘链接')
  } catch (error) {
    message.error(errorText(error))
  } finally {
    linksLoading.value = false
  }
}

async function getAttachment(fileId) {
  attachmentLoadingId.value = String(fileId)
  try {
    const data = await resourceApi.attachment(route.params.id, fileId)
    attachments.value = { ...attachments.value, [String(fileId)]: data }
  } catch (error) {
    message.error(errorText(error))
  } finally {
    attachmentLoadingId.value = ''
  }
}

function redeem() {
  Modal.confirm({
    title: '积分兑换资源',
    content: `确定使用 ${resource.value?.pointsPrice ?? '-'} 积分兑换《${resource.value?.title || '该资源'}》吗？`,
    okText: '确认兑换',
    cancelText: '取消',
    async onOk() {
      redeeming.value = true
      try {
        await userResourceApi.redeem(route.params.id)
        message.success('兑换成功，已获得资源访问权益')
        await load()
      } catch (error) {
        message.error(errorText(error))
      } finally {
        redeeming.value = false
      }
    },
  })
}

function claim() {
  Modal.confirm({
    title: '领取会员资源',
    content: `首次领取《${resource.value?.title || '该资源'}》将占用今日 1 个不同资源额度，重复领取不会重复扣减。确定继续吗？`,
    okText: '确认领取',
    cancelText: '取消',
    async onOk() {
      claiming.value = true
      try {
        const result = await userResourceApi.claim(route.params.id)
        if (result?.duplicated) message.success('该资源已领取过，本次未重复占用会员额度')
        else message.success(`领取成功，今日剩余 ${result?.remainingToday ?? '-'} 个会员资源额度`)
        await load()
      } catch (error) {
        message.error(errorText(error))
      } finally {
        claiming.value = false
      }
    },
  })
}

function openReport() {
  Object.assign(reportForm, { linkId: undefined, reason: '' })
  reportOpen.value = true
}

async function submitReport() {
  await reportFormRef.value.validate()
  reportSaving.value = true
  try {
    await userResourceApi.report(route.params.id, { linkId: reportForm.linkId || null, reason: reportForm.reason.trim() })
    message.success('失效举报已提交')
    reportOpen.value = false
  } catch (error) {
    message.error(errorText(error))
  } finally {
    reportSaving.value = false
  }
}

onMounted(load)
watch(() => route.params.id, (value, oldValue) => { if (value && value !== oldValue) load() })
</script>

<template>
  <div class="content-page resource-detail-page">
    <a-spin :spinning="loading">
      <template v-if="resource">
        <div class="article-back"><router-link to="/resources">← 返回资源中心</router-link></div>
        <article class="resource-detail-card">
          <header class="resource-detail-header">
            <div><a-tag color="blue">{{ categoryName(resource.categoryId) }}</a-tag><a-tag>{{ accessTypeText(resource.accessType) }}</a-tag><a-tag v-if="resource.accessGranted" color="success">已获访问授权</a-tag></div>
            <h1>{{ resource.title }}</h1>
            <p>{{ resource.summary }}</p>
            <div class="resource-detail-meta"><strong>{{ resourcePrice(resource) }}</strong><span>公开版本 v{{ resource.versionNo ?? resource.publishedVersionNo ?? '-' }}</span><span>{{ resource.viewCount || 0 }} 次浏览</span><span>发布于 {{ formatDate(resource.createTime, true) }}</span></div>
            <div v-if="resource.tags?.length" class="article-tags" style="margin-top: 18px"><a-tag v-for="tag in resource.tags" :key="tag">{{ tag }}</a-tag></div>
          </header>
          <section class="resource-detail-body">
            <h3>资源说明</h3>
            <p class="resource-description">{{ resource.description }}</p>
            <div class="resource-copyright"><strong>版权声明</strong><br>{{ resource.copyrightNotice }}</div>
          </section>
          <section class="resource-access-panel">
            <div class="resource-access-heading"><h3>网盘链接</h3><a-space wrap><a-button v-if="resource.accessType === 'POINTS' && !resource.accessGranted" type="primary" :loading="redeeming" @click="redeem">积分兑换</a-button><a-button v-if="isMemberResource && !resource.accessGranted" type="primary" :loading="claiming" @click="claim">会员领取</a-button><a-button type="primary" :loading="linksLoading" @click="loadLinks">{{ resource.accessType === 'FREE' || resource.accessGranted ? '查看网盘链接' : '获取网盘链接' }}</a-button></a-space></div>
            <p class="resource-access-tip">链接与提取码由后端统一鉴权后返回。未授权时不会获取或展示任何敏感信息。</p>
            <div v-if="links.length" class="resource-link-list">
              <div v-for="link in links" :key="link.id" class="resource-link-item">
                <div class="resource-link-title"><strong>{{ link.linkName }}</strong><a-tag>{{ link.diskName || link.diskCode }}</a-tag></div>
                <a v-if="safeUrl(link.url)" class="resource-link-address" :href="safeUrl(link.url)" target="_blank" rel="noopener noreferrer">{{ link.url }}</a>
                <span v-else>链接地址不可用</span>
                <span v-if="link.extractionCode" class="resource-extraction-code">提取码：{{ link.extractionCode }}</span>
              </div>
            </div>
            <div v-if="resource.attachmentFileIds?.length" class="resource-attachments">
              <h3>附件</h3>
              <div v-for="fileId in resource.attachmentFileIds" :key="fileId" class="resource-attachment-item">
                <div><strong>附件 #{{ fileId }}</strong><small v-if="attachments[String(fileId)]?.expiresAt">临时地址有效期至 {{ formatDate(attachments[String(fileId)].expiresAt, true) }}</small></div>
                <a-space><a-button size="small" :loading="attachmentLoadingId === String(fileId)" @click="getAttachment(fileId)">授权获取</a-button><a v-if="safeAttachmentUrl(attachments[String(fileId)]?.url)" :href="safeAttachmentUrl(attachments[String(fileId)].url)" target="_blank" rel="noopener noreferrer">打开附件</a></a-space>
              </div>
            </div>
            <div class="resource-detail-actions"><a-button danger @click="openReport">报告资源失效</a-button></div>
          </section>
        </article>
      </template>
      <a-result v-else-if="loadError" :status="loadError.response?.status === 403 ? '403' : '404'" title="资源暂不可访问" :sub-title="loadError.response?.status === 404 ? '资源不存在、尚未公开或已下线。' : errorText(loadError)"><template #extra><a-button type="primary" @click="$router.replace('/resources')">返回资源中心</a-button><a-button @click="load">重新加载</a-button></template></a-result>
    </a-spin>
    <a-modal v-model:open="reportOpen" title="报告资源失效" :confirm-loading="reportSaving" ok-text="提交举报" @ok="submitReport">
      <a-form ref="reportFormRef" :model="reportForm" :rules="reportRules" layout="vertical">
        <a-form-item v-if="links.length" label="失效链接（可选）" name="linkId"><a-select v-model:value="reportForm.linkId" allow-clear placeholder="不选择则举报整个资源" :options="links.map(link => ({ value: String(link.id), label: `${link.diskName || link.diskCode} · ${link.linkName}` }))" /></a-form-item>
        <a-form-item label="失效原因" name="reason"><a-textarea v-model:value="reportForm.reason" :rows="5" :maxlength="1000" show-count placeholder="请说明无法访问的具体情况" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
