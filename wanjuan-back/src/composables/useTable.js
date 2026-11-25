import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { errorText, pageResult } from '../utils'

export function useTable(fetcher, defaultParams = () => ({})) {
  const loading = ref(false)
  const records = ref([])
  const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showQuickJumper: true, showTotal: (total) => `共 ${total} 条` })

  async function load(page = pagination.current, size = pagination.pageSize) {
    loading.value = true
    try {
      const data = await fetcher({ page, size, ...defaultParams() })
      const result = pageResult(data)
      records.value = result.records
      pagination.current = page
      pagination.pageSize = size
      pagination.total = result.total
    } catch (error) {
      message.error(errorText(error))
    } finally {
      loading.value = false
    }
  }

  function changeTable(page) {
    load(page.current, page.pageSize)
  }

  function search() {
    load(1, pagination.pageSize)
  }

  return { loading, records, pagination, load, changeTable, search }
}
