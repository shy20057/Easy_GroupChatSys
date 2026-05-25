<template>
  <div>
    <el-cascader
      :options="AreaData"
      v-model="modelValue.areaCode"
      @change="change"
      ref="areaSelectRef"
      clearable
      ></el-cascader>
   </div>
</template>

<script setup>
import AreaData from "./AreaData.js"
import { ref, reactive, getCurrentInstance, nextTick } from "vue"
const { proxy } = getCurrentInstance();
import { useRoute,useRouter} from 'vue-router'
const route = useRoute()
const router = useRouter()

const props = defineProps({
    modelValue:{
        type:Object,
        default: {}
    }
})

const areaSelectRef = ref(null)

const emit = defineEmits(['update:modelValue'])
const change = (value) => {
    const areaData = {
        areaName:[],
        areaCode:[]
    }
    const checkNodes = areaSelectRef.value.getCheckedNodes()[0]
    if(!checkNodes){
       emit('update:modelValue',areaData)
       return
    }
    const pathValues = checkNodes.pathValues;
    const pathLabels = checkNodes.pathLabels;
    areaData.areaName = pathLabels
    areaData.areaCode = pathValues
    emit('update:modelValue',areaData)
}

</script>

<style lang="scss" scoped>
</style>