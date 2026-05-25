<template>
  <div>
    <ShowLocalImage :fileId="data.messageId" partType="chat" :fileType="data.fileType" :showPlay="true">
    </ShowLocalImage>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from "vue"
const { proxy } = getCurrentInstance();
import { useRoute,useRouter} from 'vue-router'
import ShowLocalImage from "@/components/ShowLocalImage.vue";
import { de } from "element-plus/es/locales.mjs";
const route = useRoute()
const router = useRouter()

const props = defineProps({
    data:{
        type:Object,
        default:{}
    }
})

</script>

<style lang="scss" scoped>
</style>
