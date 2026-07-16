import { createRouter, createWebHistory } from 'vue-router'
import ChatInterface from '../views/ChatInterface.vue'

const routes = [
  {
    path: '/',
    name: 'chat',
    component: ChatInterface
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router