<template>
  <div class="avatar-upload">
   <div class="avatar-show">
    <template v-if="modelValue">
        <el-image v-if="preview" :src="localFile" fit="scale-down"></el-image>

        <ShowLocalImage
        :fileId="props.modelValue"
        partType="avatar"
        :width="40"
        v-else
        ></ShowLocalImage>
    </template>

    <template v-else>
        <el-upload
        name="file"
        :show-file-list="false"
        accept=".png,.PNG,.jpg,.JPG,.jpeg,.JPEG,.gif,.GIF,.bmp,.BMP"
        :multiple="false"
        :http-request="uploadImage">
    <span class="iconfont icon-add"></span>
    </el-upload>
    </template>
   </div>

   <div class="select-btn">
    <el-upload
    name="file"
    :show-file-list="false"
    accept=".png,.PNG,.jpg,.JPG,.jpeg,.JPEG,.gif,.GIF,.bmp,.BMP"
    :multiple="false"
    :http-request="uploadImage"
    >
    <el-button type="primary" size="small">选择</el-button>
   </el-upload>
   </div>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick, computed, onMounted, onUnmounted } from "vue"
const { proxy } = getCurrentInstance();

const props = defineProps({
    modelValue: {
        type: [String , Object],
        default: null
    }
})

const preview = computed(() => {
  return props.modelValue instanceof File
})

const localFile = ref(null)
const emit = defineEmits(["coverFile"])
// ###p48 上传头像
const uploadImage = async (file) => { 
    file = file.file;
    window.ipcRenderer.send("createCover",file.path);
}

onMounted(() => { 
  window.ipcRenderer.on("createCoverCallback",(e,{avatarStream,coverStream}) => {
    const coverBlob = new Blob([coverStream], { type: "image/png" }); // Blob（Binary Large Object）是浏览器提供的一个内置对象，用于表示不可变的、原始的二进制数据。它代表一个类文件对象，可以包含不同类型的数据，比如图片、音频、视频等。
    const coverFile = new File([coverBlob],"thhumbnail.jpg")
    let img = new FileReader()
    img.readAsDataURL(coverFile)
    img.onload = ({target})=>{
      localFile.value = target.result
    }
      const avatarBlob = new Blob([avatarStream], { type: "image/png" });
      const avatarFile = new File([avatarBlob],"thhumbnail2.jpg")
      emit("coverFile",{avatarFile,coverFile})

   })
})

onUnmounted(() => {
  window.ipcRenderer.removeAllListeners("createCoverCallback")
 })



</script>

<style lang="scss" scoped>
.avatar-upload {
  display: flex;
  justify-content: center;
  align-items: end;
  line-height: normal;
}
.avatar-show {
  background: #ededed;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
  .icon-add {
    font-size: 30px;
    color: #b9b9b9;
    width: 60px;
    height: 60px;
    text-align: center;
    line-height: 60px;
  }
  img {
    width: 100%;
    height: 100%;
  }
  .op {
    position: absolute;
    color: #e8aef0;
    top: 80px;
  }
}
.select-btn {
  vertical-align: bottom;
  margin-left: 5px;
}
</style>