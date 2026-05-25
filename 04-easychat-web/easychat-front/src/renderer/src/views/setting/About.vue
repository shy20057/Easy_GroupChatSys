<template>
  <ContentPanel>
    <el-form ref="formDataRef" label-width="80px" @submit.prevent></el-form>
    <el-form-item label="版本信息">
      <div class="version-info">
        <div>Noshy Chat {{ config.version }}</div>
        <div>
          <el-button type="primary" @click="checkUpdate">检查更新</el-button>
        </div>
      </div>
    </el-form-item>
  </ContentPanel>
</template>

<script setup>
import config from '../../../../../package.json'
import { ref, reactive, getCurrentInstance, nextTick } from 'vue'
const { proxy } = getCurrentInstance()
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()

import { useUserInfoStore } from '@/stores/UserInfoStore'
const userInfoStore = useUserInfoStore()

//TODO 检测更新
const checkUpdate = async () => {
  let result = await proxy.Request({
    url: proxy.Api.checkVersion,
    params: {
      appVersion: config.version,
      uid: userInfoStore.getInfo().userId
    }
  })
  if (!result) {
    return
  }
  if (result.data == null) {
    proxy.Message.success("当前已是最新版本")
  } else {
    proxy.Confirm({
      message: `发现新版本 v${result.data.version}，更新日志：\n${result.data.updateList.join('\n')}\n是否前往更新？`,
      okfun: () => {
        if (result.data.outerLink) {
          window.ipcRenderer.send("openUrl", result.data.outerLink)
        } else {
          proxy.Message.info("更新包下载功能暂未启用，请联系管理员")
        }
      }
    })
  }
}
</script>

<style lang="scss" scoped>

</style>