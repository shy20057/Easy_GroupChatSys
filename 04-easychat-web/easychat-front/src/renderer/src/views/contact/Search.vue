<template>
    <!-- 接上ContentPanel的插槽 在插槽里面渲染数据 这里在main.js里面引了 这里不需要单独再引 -->
  <ContentPanel>
    <div class="search-form">
        <el-input clearable placeholder="请输入用户ID或群组ID" v-model="contactId" size="large" @keydown.enter="search"></el-input>
        <div class="search-btn iconfont icon-search" @click="search"></div>
    </div>
    <div v-if="searchResult && Object.keys(searchResult).length > 0" class="search-result-panel">
       <div class="search-result">
         <span class="contact-type">{{ contactTypeName }}</span>
          <UserBaseInfo :userInfo="searchResult" 
                        :showArea="searchResult.contactTypePrefix== 'U'">
          </UserBaseInfo>
       </div>

       <div class="op-btn" v-if="searchResult.contactId != userInfoStore.getInfo().userId">
        <el-button type="primary" 
        v-if="
          searchResult.status == null ||
          searchResult.status == 0 ||
          searchResult.status == 2 ||
          searchResult.status == 3 ||
          searchResult.status == 4
        "
        @click="applyContact">{{ searchResult.contactTypePrefix == 'U' ? '添加联系人':'申请加入群组' }}</el-button>
       
        <el-button type="primary" v-if="searchResult.status == 1" @click="sendMessage">发消息</el-button>
        <span type="primary" v-if="searchResult.status == 5 || searchResult.status == 6">对方拉黑了你</span>
    
       </div>
    </div>

    <div v-id="!searchResult" class="no-data">没有搜索到任何结果</div>
  </ContentPanel> <!-- ref 用于在父组件中直接访问子组件的实例 或DOM元素 ref="searchAddRef"：为 SearchAd 组件实例创建了一个引用，命名为 searchAddRef-->
  <SearchAdd ref="searchAddRef" @reload="resetFrom"></SearchAdd>
</template>

<script setup>
import { ref ,reactive,getCurrentInstance,nextTick, computed} from 'vue'
import { useUserInfoStore } from '../../stores/UserInfoStore'
const { proxy } = getCurrentInstance()
import SearchAdd from './SearchAdd.vue'

const userInfoStore = useUserInfoStore()
const contactTypeName = computed(() => {
  if(userInfoStore.getInfo().userId == searchResult.value.contactId){
      return '自己'
  }
  if(searchResult.value.contactTypePrefix == 'U'){
    return '用户'
  }
  if(searchResult.value.contactTypePrefix == 'G'){
    return '群组'
  }
})

const contactId = ref('')
const searchResult = ref({})
const search = async() => { 
  if(!contactId.value){
    proxy.Message.warning('请输入用户ID或群组ID');
    return;
  }
  // 这里是与后端交互的关键部分 这里的Request也需要自己定义
   let result = await proxy.Request({
    url: proxy.Api.search,
    params:{
      contactId:contactId.value
    }
   })
   if(!result){
    return;
   }
   searchResult.value = result.data
}


const searchAddRef = ref(null)

// 这里将数据传输到searchAdd.vue层
const applyContact = async() => { 
  searchAddRef.value.show(searchResult.value) //这里的searchAddRef就是这个子组件的实例啊 ref 调用子组件的 show 方法，将 searchResult.value 作为参数传递
}

const resetFrom = () => {
  searchResult.value = {}
  contactId.value = ''
}


</script>

<style lang="scss" scoped>
.search-form {
  padding-top: 50px;
  display: flex;
  align-items: center;
  :deep(.el-input__wrapper) {
    border-radius: 4px 0px 0px 4px;
    border-right: none;
  }
}
.search-btn {
  background: #07c160;
  color: #fff;
  line-height: 40px;
  width: 80px;
  text-align: center;
  border-radius: 0px 5px 5px 0px;
  cursor: pointer;
  &:hover {
    background: #0dd36c;
  }
}
.no-data {
  padding: 30px 0px;
}
.search-result-panel {
  .search-result {
    padding: 30px 20px 20px 20px;
    background: #fff;
    border-radius: 5px;
    margin-top: 10px;
    position: relative;
    .contact-type {
      position: absolute;
      left: 0px;
      top: 0px;
      background: #2cb6fe;
      padding: 2px 5px;
      color: #fff;
      border-radius: 5px 0px 0px 0px;
      font-size: 12px;
    }
  }
  .op-btn {
    border-radius: 5px;
    margin-top: 10px;
    padding: 10px;
    background: #fff;
    text-align: center;
  }
}
</style>
