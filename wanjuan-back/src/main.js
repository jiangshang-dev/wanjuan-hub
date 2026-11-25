import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import permission from './directives/permission'
import './styles/index.css'

createApp(App).use(createPinia()).use(router).use(Antd).directive('permission', permission).mount('#app')
