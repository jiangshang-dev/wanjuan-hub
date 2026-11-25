import { useAuthStore } from '../stores/auth'

function applyPermission(el, binding) {
  const auth = useAuthStore()
  if (!auth.hasPermission(binding.value)) el.parentNode?.removeChild(el)
}

export default {
  mounted: applyPermission,
}
