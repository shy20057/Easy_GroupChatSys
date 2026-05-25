<template>
  <div class="win-top no-drag win-op-container">
    <!-- 按钮内容 -->
    <div v-if="showSetTop" :class="['iconfont icon-top', isTop ? 'win-top' : '']"
      @click="top" :title="isTop ? '取消置顶' : '置顶'"></div>

    <!-- 最小化 -->
    <div class="iconfont icon-min" @click="minimize" title="最小化" v-if="showMin"></div>
    <!-- 最大化 -->
    <div v-if="showMax" :class="['iconfont', isMax ? 'icon-maximize' : 'icon-max']"
      :title="isMax ? '向下还原' : '最大化'"
      @click="maximize"></div>

    <!-- 关闭 -->
    <div v-if="showClose" class="iconfont icon-close" @click="close" title="关闭"></div>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick, onMounted } from "vue";
const { proxy } = getCurrentInstance();

// defineProps 是 vue3中的一个宏 用于在使用组合式API的Vue组件中定义props props用于父子组件之间传递数据
const props = defineProps({
  showSetTop: {
    type: Boolean,
    default: true
  },
  showMin: {
    type: Boolean,
    default: true
  },
  showMax: {
    type: Boolean,
    default: true
  },
  showClose: {
    type: Boolean,
    default: true
  },
  // 关闭类型 0：关闭 1：隐藏
  closeType: {
    type: Number,
    default: 0
  }
})


const isMax = ref(false);
const isTop = ref(false);

onMounted(() => {
  isMax.value = false
  isTop.value = false
})

const winOp = (action, data) => {
  window.ipcRenderer.send('winTitleOp', { action, data })
}


const emit = defineEmits(["closeCallback"]);
const close = () => {
  winOp('close', { closeType: props.closeType }) /* 交给主进程来做这些页面关闭，隐藏，缩小，放大 */
  emit("closeCallback")
}

const minimize = () => {
  winOp('minimize')
}

const maximize = () => {
  if (isMax.value) {
    winOp('unmaximize')
    isMax.value = false
  } else {
    winOp('maximize')
    isMax.value = true
  }
}

const top = () => {
  isTop.value = !isTop.value
  winOp('top', { isTop: isTop.value })
}

</script>

<style lang="scss" scoped>
.win-top {
  top: 0px;
  right: 0px;
  position: absolute;
  z-index: 1;
  overflow: hidden;
  border-radius: 0px 3px 0px 0px;
  .iconfont {
    float: left;
    font-size: 12px;
    color: #101010;
    text-align: center;
    display: flex;
    justify-content: center;
    cursor: pointer;
    height: 25px;
    align-items: center;
    padding: 0px 10px;
    &:hover {
      background: #ddd;
    }
  }
  .icon-close {
    &:hover {
      background: #fb7373;
      color: #fff;
    }
  }
}


/* 调整内部样式 */
.win-op-container {
  display: flex;
  align-items: center;
}

.iconfont {
  margin-left: 5px; /* 调整按钮之间的间距 */
}
</style>
