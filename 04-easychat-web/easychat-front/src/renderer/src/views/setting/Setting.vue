<template>
  <Layout>
    <template #left-content>
      <div class="title-panel drag">设置</div>
      <div class="menu-list">
        <div 
          :class="['menu-item',route.path== item.path? 'menu-active':'']"
          v-for="item in settingMenuList"
          @click="Jump(item)"
          >
         <div :class="['iconfont',item.icon]" :style="{background: item.bgColor}"></div>
         <div class="menu-name">{{item.name}}</div>
        </div> 
      </div>
    </template> 

    <template #right-content>
      <div class="title-panel drag"></div>
      <router-view v-slot="{Component}">
         <component :is="Component" ref="componentRef"></component>
      </router-view>
      <!-- router-view 是子组件渲染的插槽 用于在指定位置动态渲染当前路由匹配的组件
           它的作用类似于一个占位符，当路由发生变化时，Vue Router 会自动将匹配的组件插入到 <router-view> 的位置 
      -->
    </template>
  </Layout>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from "vue"
const { proxy } = getCurrentInstance();
import { useRoute,useRouter} from 'vue-router'
const route = useRoute()
const router = useRouter()

const settingMenuList = ref([
  {
    name: '账号设置',
    icon: 'icon-user-setting',
    path: '/setting/userInfo',
    bgColor: '#0294f5'
  },
  {
    name: '文件管理',
    icon: 'icon-folder',
    path: '/setting/fileManage',
    bgColor: '#ffd04f'
  },
  {
    name: '关于pigChat',
    icon: 'icon-about',
    path: '/setting/about',
    bgColor: '#08bf61'
  }
])

const Jump = (data) => {
   router.push(data.path)
}
</script>

<style lang="scss" scoped>
.title-panel {
  height: 60px;
  background: #f7f7f7;
  text-align: center;
  line-height: 60px;
}

.menu-list {
  border-top: 1px solid #ddd;
  .menu-item {
    display: flex;
    align-items: center;
    padding: 10px;
    &:hover {
      cursor: pointer;
      background: #d6d6d7;
    }
    .iconfont {
      font-size: 20px;
      width: 35px;
      height: 35px;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .menu-name {
      color: #000000;
      margin-left: 10px;
      flex: 1;
    }
  }
  .menu-active {
    background: #c4c4c4;
    &:hover {
      background: #c4c4c4;
    }
  }
}
</style>
