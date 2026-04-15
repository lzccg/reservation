import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'
import StandardLayout from '@/layout/StandardLayout.vue'
import BlankLayout from '@/layout/BlankLayout.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/',
    component: BlankLayout,
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/login/index.vue'),
        meta: { title: '宣讲会系统 - 登录', requiresAuth: false }
      }
    ]
  },
  // 1. 独立签到模块 (使用空白布局)
  {
    path: '/kiosk',
    component: BlankLayout,
    children: [
      {
        path: 'checkin',
        name: 'FaceCheckin',
        component: () => import('@/views/checkin/index.vue'),
        meta: { title: '现场人脸签到终端', requiresAuth: true }
      }
    ]
  },
  {
    path: '/',
    component: StandardLayout,
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页概览', requiresAuth: true }
      },
      // === Admin Routes ===
      {
        path: 'admin/students',
        name: 'AdminStudents',
        component: () => import('@/views/admin/StudentManagement.vue'),
        meta: { title: '学生管理', requiresAuth: true, role: 'admin' }
      },
      {
        path: 'admin/admins',
        name: 'AdminAdmins',
        component: () => import('@/views/admin/AdminManagement.vue'),
        meta: { title: '管理员管理', requiresAuth: true, role: 'admin' }
      },
      {
        path: 'admin/companies',
        name: 'AdminCompanies',
        component: () => import('@/views/admin/CompanyManagement.vue'),
        meta: { title: '企业管理', requiresAuth: true, role: 'admin' }
      },
      {
        path: 'admin/sessions',
        name: 'AdminSessions',
        component: () => import('@/views/admin/SessionStatus.vue'),
        meta: { title: '宣讲会调度审查', requiresAuth: true, role: 'admin' }
      },
      {
        path: 'admin/statistics',
        name: 'AdminStatistics',
        component: () => import('@/views/admin/GlobalStatistics.vue'),
        meta: { title: '全局统计', requiresAuth: true, role: 'admin' }
      },
      {
        path: 'admin/checkin',
        name: 'AdminCheckin',
        component: () => import('@/views/admin/CheckinAdmin.vue'),
        meta: { title: '现场签到', requiresAuth: true, role: 'admin' }
      },
      {
        path: 'admin/today-sessions',
        name: 'AdminTodaySessions',
        component: () => import('@/views/admin/TodaySessions.vue'),
        meta: { title: '今日宣讲会', requiresAuth: true, role: 'admin' }
      },
      // === Company Routes ===
      {
        path: 'company/profile',
        name: 'CompanyProfile',
        component: () => import('@/views/company/Profile.vue'),
        meta: { title: '企业资质维护', requiresAuth: true, role: 'company' }
      },
      {
        path: 'company/sessions',
        name: 'CompanySessions',
        component: () => import('@/views/company/SessionManagement.vue'),
        meta: { title: '发布与管理', requiresAuth: true, role: 'company' }
      },
      {
        path: 'company/records',
        name: 'CompanyRecords',
        component: () => import('@/views/company/CheckinRecords.vue'),
        meta: { title: '预约与签到名单', requiresAuth: true, role: 'company' }
      },
      // === Student Routes ===
      {
        path: 'student/face',
        name: 'StudentFace',
        component: () => import('@/views/student/FaceRegistration.vue'),
        meta: { title: '人脸信息采集', requiresAuth: true, role: 'student' }
      },
      {
        path: 'student/profile',
        name: 'StudentProfile',
        component: () => import('@/views/student/Profile.vue'),
        meta: { title: '信息维护', requiresAuth: true, role: 'student' }
      },
      {
        path: 'student/sessions',
        name: 'StudentSessions',
        component: () => import('@/views/student/SessionList.vue'),
        meta: { title: '宣讲会大厅', requiresAuth: true, role: 'student' }
      },
      {
        path: 'student/history',
        name: 'StudentHistory',
        component: () => import('@/views/student/History.vue'),
        meta: { title: '预约签到记录', requiresAuth: true, role: 'student' }
      },
      {
        path: 'student/checkin-entrance',
        name: 'CheckinEntrance',
        component: () => import('@/views/student/CheckinEntrance.vue'),
        meta: { title: '近期宣讲会', requiresAuth: true, role: 'student' }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/layout/BlankLayout.vue'), // 简化替代为BlankLayout不展示内容或可以补一页
    meta: { title: '无权限访问' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '校园宣讲会预约签到系统'
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.token) {
    next('/login')
  } else if (to.meta.role && userStore.role !== to.meta.role) {
    next('/403') // 角色不匹配拦截
  } else if (userStore.role === 'admin' && userStore.userInfo?.adminRoleLevel === 2) {
    const allow = ['/dashboard', '/admin/checkin', '/admin/today-sessions', '/kiosk/checkin']
    if (to.path.startsWith('/admin') && !allow.includes(to.path)) {
      next('/admin/checkin')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
