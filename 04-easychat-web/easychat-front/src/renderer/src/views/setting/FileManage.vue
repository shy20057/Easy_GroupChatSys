<template>
  <ContentPanel v-loading="copying" element-loading-text="正在复制文件">
    <el-form
      label-position="top"
      :model="formData"
      :rules="rules"
      ref="formDataRef"
      label-width="80px"
      @submit.prevent
    >
      <!--input输入-->
      <el-form-item label="文件管理" prop="" class="file-manage">
        <div class="file-input" :title="formData.sysSetting">{{ formData.sysSetting }}</div>
        <div class="tips">文件的默认保存位置</div>
      </el-form-item>
      <el-form-item label="" prop="">
        <el-button type="primary" @click="changeFolder">更改</el-button>
        <el-button type="primary" @click="openLocalFolder">打开文件夹</el-button>
      </el-form-item>
    </el-form>
  </ContentPanel>
</template>

<script setup>
  import { ref, reactive, getCurrentInstance, nextTick, onMounted, onUnmounted } from "vue"
  const { proxy } = getCurrentInstance();
  import { useRoute,useRouter} from 'vue-router'
  const route = useRoute()
  const router = useRouter()

  import { useUserInfoStore } from "@/stores/UserInfoStore"
  const userInfoStore = useUserInfoStore()

  const copying = ref(false)  
  const formData = ref({
    sysSetting: ''
  });
  const formDataRef = ref();
  const rules = {
    title: [{ required: true, message: "请输入内容" }],
  };

  const getLocalStoreCallback = (e, path) => {
    if (path) {
      const userId = userInfoStore.getInfo().userId;
      if (path.endsWith(userId)) {
        formData.value.sysSetting = path.substring(0, path.length - userId.length);
      } else if (path.endsWith(userId + "\\")) {
        formData.value.sysSetting = path.substring(0, path.length - userId.length - 1);
      } else if (path.endsWith(userId + "/")) {
        formData.value.sysSetting = path.substring(0, path.length - userId.length - 1);
      } else {
        formData.value.sysSetting = path;
      }
    }
  }

  const changeFileFolderCallback = (e, data) => {
    if (data.status === "start") {
      copying.value = true;
    } else if (data.status === "success") {
      copying.value = false;
      formData.value.sysSetting = data.path;
      proxy.Message.success("更改成功");
    } else if (data.status === "error") {
      copying.value = false;
      proxy.Message.error("更改失败：" + data.msg);
    }
  }

  onMounted(() => {
    window.ipcRenderer.send("getLocalStore", userInfoStore.getInfo().userId + "localFileFolder")
    window.ipcRenderer.on("getLocalStoreCallback", getLocalStoreCallback)
    window.ipcRenderer.on("changeFileFolderCallback", changeFileFolderCallback)
  })

  onUnmounted(() => {
    window.ipcRenderer.removeListener("getLocalStoreCallback", getLocalStoreCallback);
    window.ipcRenderer.removeListener("changeFileFolderCallback", changeFileFolderCallback);
  })

  // 更改文件缓存路径
  const changeFolder = () => {
    window.ipcRenderer.send("changeFileFolder")
  }

  const openLocalFolder = () => {  
    window.ipcRenderer.send("openLocalFolder")
  }
</script>

<style lang="scss" scoped>
.file-manage {
  ::v-deep(.el-form-item__content) {
    display: block;
  }
  .file-input {
    background: #fff;
    padding: 0px 5px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 16px;
    border: 1px solid #ddd;
    min-height: 30px;
    line-height: 30px;
  }
  .tips {
    color: #888888;
    font-size: 13px;
  }
}
</style>
