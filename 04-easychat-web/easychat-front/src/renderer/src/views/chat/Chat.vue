<template>
  <Layout>
    <template #left-content>
      <div class="drag-panel drag"></div>
      <div class="top-search">
        <el-input clearable placeholder="搜索" v-model="searchKey" size="small" @keyup="search">
          <template #suffix>
            <span class="iconfont icon-search"></span>
          </template>
        </el-input>
      </div>

      <div class="chat-session-list">
        <template v-for="item in chatSessionList">
          <ChatSession 
            :data="item" 
            @click="chatSessionClickHandler(item)"
            @contextmenu.stop="onContextMenu(item, $event)"
            :currentSession="item.contactId == currentChatSession.contactId"
          ></ChatSession>
        </template>
      </div>
    </template>

    <template #right-content>
      <div class="title-panel drag" v-if="Object.keys(currentChatSession).length > 0">
        <div class="title">
          <span>{{ currentChatSession.contactName }}</span>
          <span v-if="currentChatSession.contactType == 1">
            ({{ currentChatSession.memberCount }})
          </span>
        </div>
      </div>
      <div 
      v-if="currentChatSession.contactType == 1" 
      class="iconfont icon-more no-drag" 
      @click="showGroupDetail"
      ></div>
      <div class="chat-panel" v-show="Object.keys(currentChatSession).length > 0">
        <div class="message-panel" id="message-panel">
          <div class="message-item" v-for="data in messageList" :id="'message'+data?.messageId">

            <template v-if="data.messageType == 1 || data.messageType == 2 || data.messageType == 5">
              <ChatMessage :data="data" :currentChatSession="currentChatSession" @showMediaDetail="showMediaDetailHandler"></ChatMessage>
            </template>
          </div>
        </div>
        <MessageSend :currentChatSession="currentChatSession" @sendMessage4Local="sendMessage4LocalHandler">
        </MessageSend>
      </div>

      <div class="chat-panel" v-show="Object.keys(currentChatSession).length == 0">
        <Blank></Blank>
      </div>
    </template>
  </Layout>

</template>

<script setup>
import Blank  from '@/components/Blank.vue'
import ChatMessage from './ChatMessage.vue'
import MessageSend from './MessageSend.vue'
import ContextMenu from '@imengyu/vue3-context-menu'
import '@imengyu/vue3-context-menu/lib/vue3-context-menu.css'

import ChatSession from './ChatSession.vue'
import { ref, reactive, getCurrentInstance, nextTick, onMounted, onUnmounted } from 'vue'
const { proxy } = getCurrentInstance()
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()


const chatSessionList = ref([])
const searchKey = ref('')



// 右键点击
const setTop = (data) => {
  data.topType = data.topType == 0 ? 1 : 0;
  // 会话排序
  sortChatSessionList(chatSessionList.value)
  window.ipcRenderer.send("topChatSession", { contactId: data.contactId, topType: data.topType });
}

// 删除会话
const delChatSession = (contactId) => {
  //从当前列表中删除
  delChatSessionList(contactId)
  currentChatSession.value = {}
  //TODO 设置选中的会话
  window.ipcRenderer.send("delChatSession", contactId)
}

// 当前选中的会话
const currentChatSession = ref({})
const messageCountInfo = {
  totalPage: 0,
  pageNo: 0,
  maxMessageId: null,
  noData: false
}
//  会话点击
const messageList = ref([])
const chatSessionClickHandler = (item) => {
  console.log('点击的会话:', item) // 添加调试日志
  currentChatSession.value = Object.assign({}, item)
  //TODO 消息记录数要清空
  messageList.value = []

  messageCountInfo.pageNo = 0
  messageCountInfo.totalPage = 1
  messageCountInfo.maxMessageId = null
  messageCountInfo.noData = false

  loadChatMessage();

  //设置选中session
  setSessionSelect({ contactId: item.contactId, sessionId: item.sessionId })

}

const setSessionSelect = ({ contactId, sessionId }) => {
  window.ipcRenderer.send('setSessionSelect', {
    contactId,
    sessionId
  })
}

//  向主进程发送加载会话消息
const loadChatMessage = () => {
  if (messageCountInfo.noData) {
    return
  }
  messageCountInfo.pageNo++
  console.log('加载消息列表参数：', currentChatSession.value.sessionId, messageCountInfo.pageNo, messageCountInfo.maxMessageId)
  window.ipcRenderer.send("loadChatMessage", {
    sessionId: currentChatSession.value.sessionId,
    pageNo: messageCountInfo.pageNo,
    maxMessage: messageCountInfo.maxMessageId,
  })

}

// 置顶与删除
const onContextMenu = (data, e) => {
  e.preventDefault(); // 阻止浏览器默认右键菜单
  ContextMenu.showContextMenu({
    x: e.x,
    y: e.y,
    items: [{
      label: data.topType == 0 ? '置顶' : '取消置顶',
      onClick: () => {
        setTop(data)
      }
    }, {
      label: '删除聊天',
      onClick: () => {
        console.log(data.contactName)
        const contactName = data.contactName || '未知联系人';
        proxy.Confirm({
          message: `确定要删除聊天【${contactName}】吗？`,
          okfun: async () => {
            delChatSession(data.contactId);
          }
        })
      }
    }]
  })
}

// 排序会话
const sortChatSessionList = (dataList) => {
  dataList.sort((a, b) => {
    const topTypeResult = b['topType'] - a['topType'];
    if (topTypeResult == 0) {
      return b["lastReceiveTime"] - a["lastReceiveTime"]
    }
    return topTypeResult
  })
}

// 删除会话
const delChatSessionList = (contactId) => {
  chatSessionList.value = chatSessionList.value.filter(item => {
    return item.contactId !== contactId
  })
}

//  监听收到的消息
const onLoadChatMessage = (data) => {
  window.ipcRenderer.on("loadChatMessageCallback", (e, result) => {

    const { dataList, pageTotal, pageNo } = result;

    if (pageNo == pageTotal) {
      messageCountInfo.noData = true
    }
    dataList.sort((a, b) => {
      return a.messageId - b.messageId
    })
    messageList.value = messageList.value.concat(dataList)
    messageCountInfo.pageNo = pageNo;
    messageCountInfo.pageTotal = pageTotal;
    if (pageNo == 1) {
      messageCountInfo.maxMessageId = dataList.length > 0 ? dataList[dataList.length - 1].messageId : null
      // 滚动条滚动到最底部
      gotoBottom()
    }
    console.log('获取到的消息列表：', messageList)

  })
}

const onReceiveMessage = () => {
  window.ipcRenderer.on("receiveMessage", (e, message) => {

    // 处理 WS 初始化消息（messageType=0）：重新加载会话列表
    if (message.messageType === 0) {
      console.log('收到 WS 初始化消息，重新加载会话列表')
      loadChatSession()
      return
    }

    let curSession = chatSessionList.value.find((item) => {
      return item.sessionId == message.sessionId
    })


    if(message.messageType == 6){
      // messageType = 6 文件上传已完成 则在本地消息列表中查找对应的消息 并更新status
      const localMessage = messageList.value.find(item  => { 
        if(item.messageId == message.messageId){
           return item;
        }
      })
      if(localMessage != null){
        localMessage.status = 1
      }
      return 
    }

    if (curSession == null) {
      chatSessionList.value.push(message.extendData)
    } else {
      Object.assign(curSession, message.extendData)
      curSession = message.extendData
    }
    sortChatSessionList(chatSessionList.value)
    if (message.sessionId != currentChatSession.value.sessionId) {
      //TODO 未读消息气泡
    } else {
      Object.assign(currentChatSession.value, message.extendData)
      messageList.value.push(message)
      gotoBottom()
    }
    

  })
}

// ###P34 会话列表 onLoadSession接受主进程IPC回调 loadChatSession向主进程发送请求会话信息的事件（通知）
const onLoadSessionData = () => {
  window.ipcRenderer.on('loadSessionDataCallback', (e, dataList) => {

    sortChatSessionList(dataList)
    chatSessionList.value = dataList

  })
} 
const loadChatSession = () => {
  window.ipcRenderer.send("loadSessionData", route.query.userId)
}

//监听主进程保存文件的进度 保存完之后通过messageId 去更新 status 发送中/已发送
const onAddLocalMessage = ()=> {
  window.ipcRenderer.on('addLocalCallback',(e,{messageId,status})=>{
    const findMessage = messageList.value.find(item=>{
      if(item.messageId == messageId){
          return item;
      }
    })
    if(findMessage != null){
      findMessage.status  = status
    }
  })
}


const sendMessage4LocalHandler = (messageObj) => {
  console.log('发送消息:', messageObj)
  
  messageList.value.push(messageObj)
  const chatSession = chatSessionList.value.find(item => {
    return item.sessionId == messageObj.sessionId
  })
  if (chatSession) {
    chatSession.lastMessage = messageObj.lastMessage
    chatSession.lastReceiveTime = messageObj.sendTime
  }

  sortChatSessionList(chatSessionList.value)
  gotoBottom()
}

// 滚动到底部
const gotoBottom = () => {
  nextTick(() => {
    const items = document.querySelectorAll('.message-item')
    if (items.length > 0) {
      setTimeout(() => {
        items[items.length - 1].scrollIntoView();
      }, 10);
    }
  })

}

//  查看媒体消息预览
const showMediaDetailHandler =  (messageId) => { 
  let showFileList = messageList.value.filter( (item) => { 
   return item.messageType == 5
  })
  showFileList = showFileList.map( item =>{
    return {
      partType: "chat",
      fileId: item.messageId,
      fileType: item.fileType,
      fileName: item.fileName,
      fileSize: item.fileSize,
      forceGet: false

    }
  })
  window.ipcRenderer.send('newWindow',{
    windowId: "media",
    title: "图片查看",
    path: "/showMedia",
    data:{
      currentFileId: messageId,
      fileList: showFileList
    }
  })
}

onMounted(() => {
  onReceiveMessage(),
    onLoadSessionData(),
    onLoadChatMessage(),
    loadChatSession(),
    onAddLocalMessage()
})

// 监听路由参数变化，自动打开对应会话
import { watch } from 'vue'
watch(
  () => route.query.chatId,
  (newChatId) => {
    if (newChatId && chatSessionList.value.length > 0) {
      // 在会话列表中查找对应的会话
      const targetSession = chatSessionList.value.find(item => item.contactId == newChatId)
      if (targetSession) {
        // 找到会话，自动点击打开
        chatSessionClickHandler(targetSession)
        console.log('自动打开会话:', targetSession)
      } else {
        // 兜底：找不到会话时创建临时会话
        console.log('未找到会话，创建临时会话:', newChatId)
        createTemporarySession(newChatId)
      }
    }
  },
  { immediate: true }
)

// 监听会话列表加载完成，如果路由有 chatId 参数则自动打开
watch(
  () => chatSessionList.value.length,
  (newLength) => {
    if (newLength > 0 && route.query.chatId) {
      const targetSession = chatSessionList.value.find(item => item.contactId == route.query.chatId)
      if (targetSession) {
        setTimeout(() => {
          chatSessionClickHandler(targetSession)
          console.log('会话加载后自动打开:', targetSession)
        }, 100)
      } else {
        // 兜底：还是找不到就创建临时会话
        console.log('会话列表加载后仍未找到，创建临时会话:', route.query.chatId)
        setTimeout(() => {
          createTemporarySession(route.query.chatId)
        }, 200)
      }
    }
  }
)

// 创建临时会话（兜底方案）
const createTemporarySession = (contactId) => {
  const tempSession = {
    contactId: contactId,
    sessionId: `temp_${Date.now()}`, // 临时 ID
    contactName: '', // 将在后续更新
    lastMessage: '',
    lastReceiveTime: new Date().getTime(),
    contactType: 0, // 个人聊天
    status: 1,
    topType: 0,
    noReadCount: 0,
    memberCount: null
  }

  console.log('创建临时会话对象:', tempSession)

  // 直接设置当前会话并打开聊天界面
  currentChatSession.value = tempSession
  messageList.value = []
  messageCountInfo.pageNo = 0
  messageCountInfo.totalPage = 1
  messageCountInfo.maxMessageId = null
  messageCountInfo.noData = false

  // 设置选中状态
  setSessionSelect({ contactId: tempSession.contactId, sessionId: tempSession.sessionId })
}

onUnmounted(() => {
  window.ipcRenderer.removeAllListeners("receiveMessage")
  window.ipcRenderer.removeAllListeners("loadSessionDataCallback")
  window.ipcRenderer.removeAllListeners("loadChatMessageCallback")
  window.ipcRenderer.removeAllListeners("loadChatMessage")
   window.ipcRenderer.removeAllListeners("addLocalCallback")
})

</script>

<style lang="scss" scoped>
.drag-panel {
  height: 25px;
  background: #f7f7f7;
}

.top-search {
  padding: 0px 10px 9px 10px;
  background: #f7f7f7;
  display: flex;
  align-items: center;

  .iconfont {
    font-size: 12px;
  }
}

.chat-session-list {
  height: calc(100vh - 62px);
  overflow: hidden;
  border-top: 1px solid #ddd;

  &:hover {
    overflow: auto;
  }
}

.search-list {
  height: calc(100vh - 62px);
  background: #f7f7f7;
  overflow: hidden;

  &:hover {
    overflow: auto;
  }
}

.title-panel {
  display: flex;
  align-items: center;

  .title {
    height: 60px;
    line-height: 60px;
    padding-left: 10px;
    font-size: 18px;
    color: #000000;
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.icon-more {
  position: absolute;
  z-index: 1;
  top: 30px;
  right: 3px;
  width: 20px;
  font-size: 20px;
  margin-right: 5px;
  cursor: pointer;
}

.chat-panel {
  border-top: 1px solid #ddd;
  background: #f5f5f5;

  .message-panel {
    padding: 10px 30px 0px 30px;
    height: calc(100vh - 200px - 62px);
    overflow-y: auto;

    .message-item {
      margin-bottom: 15px;
      text-align: center;
    }
  }
}
</style>