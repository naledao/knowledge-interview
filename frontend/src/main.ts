import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'

// 注册页面路由，PC 端和移动端使用相同的路由定义。
createApp(App).use(router).mount('#app')