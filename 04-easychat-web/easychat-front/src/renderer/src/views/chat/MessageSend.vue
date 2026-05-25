<template>
  <div class="send-panel">
    <div class="toolbar">
      <el-popover
        :visible="showEmojiPopover"
        trigger="click"
        placement="top"
        :teleported="false"
        @show="openPopover"
        @hide="closePopover"
        :popper-style="{
          padding: '0px 10px 10px 10px',
          width: '490px'
        }">
        <template #default>
          <el-tabs v-model="activeEmoji">
            <el-tab-pane :label="emoji.name" :name="emoji.name" v-for="emoji in emojiList">
              <div class="emoji-list">
                <div class="emoji-item" v-for="item in emoji.emojiList" @click="sendEmoji(item)">
                  {{ item }}
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>
        <template #reference>
          <div class="iconfont icon-emoji" @click="showEmojiPopoverHandler"></div>
        </template>
      </el-popover>

      <el-upload
        ref="uploadRef"
        name="file"
        :show-file-list="false"
        :multiple="true"
        :limit="fileLimit"
        :http-request="uploadFile"
        :on-exceed="uploadExceed"> <!-- 阻止超出限制的 -->
        <div class="iconfont icon-folder"></div>
      </el-upload>
    </div>

    <div class="input-area" @drop="dropHandler" @dragover="dragOverHandler">
      <el-input 
        rows="5"
        v-model="msgContent"
        type="textarea"
        resize="none"
        maxlength="500"
        show-word-limit
        spellcheck="false"
        input-style="background:#f5f5f5;border:none;"
        @keydown.enter="sendMessage"
        @paste="pasteFile"></el-input>
    </div>
    <div class="send-btn-panel">
      <el-popover
        trigger="click"
        :visible="showSendMsgPopover"
        :hide-after="1500"
        placement="top"
        :teleported="false"
        @show="openPopover"
        @hide="closePopover"
        :popper-style="{
          padding: '5px',
          'min-width': '0px',
          width: '120px'
        }">
        <template #default><span class="empty-msg">不能发送空白消息</span></template>
        <template #reference>
          <span class="send-btn" @click="sendMessage">发送(s)</span>
        </template> 
      </el-popover>
    </div>

    <SearchAdd ref="searchAddRef" @reload="resetFrom"></SearchAdd>
  </div>
</template>

<script setup>
  import SearchAdd from "@/views/contact/SearchAdd.vue"
  import emojiList from "../../utils/Emoji";
  import { ref, reactive, getCurrentInstance, nextTick, onMounted } from "vue"
  const { proxy } = getCurrentInstance();
  import { useRoute, useRouter } from 'vue-router'
  const route = useRoute()
  const router = useRouter()

  import { useUserInfoStore } from "@/stores/UserInfoStore"
  import { getFileType } from "../../utils/Constants";
  const userInfoStore = useUserInfoStore()

  import { useSysSettingStore } from "../../stores/SysSettingStore";
import { fa } from "element-plus/es/locales.mjs";
  const sysSettingStore = useSysSettingStore()

  const props = defineProps ({
    currentChatSession: {
      type: Object,
      default: {}
    }
  })

  const activeEmoji = ref('笑脸与情感')
  const msgContent = ref('')

  // 添加表情弹窗控制
  const showEmojiPopover = ref(false)
  const showSendMsgPopover = ref(false)
  const hidePopover = () => {
     showSendMsgPopover.value = false
     showEmojiPopover.value = false
   }
  // 打开弹窗
  const openPopover = () => {
    document.addEventListener("click", hidePopover,false)
  }

  const closePopover = () =>{
    document.removeEventListener("click", hidePopover,false)
  }

  const showEmojiPopoverHandler = () => { 
    showEmojiPopover.value = true
  }


  // 发送表情
  const sendEmoji = (item) => {
    msgContent.value += item
    showEmojiPopover.value = false
  }

 

  const sendMessage = async(e) => { 
    if(e.shiftKey && e.keyCode === 13){
      return
    }
    e.preventDefault();
    const messageContent = msgContent.value ? msgContent.value.replace(/\s*$/g, '') : ''
    if(messageContent === ''){
      showSendMsgPopover.value = true;
      return;
    }
    sendMessageDo({
      messageContent,
      messageType:2
    },true)
  }

  const emit = defineEmits(['sendMessage4Local'])

  // 真正的发送消息
  const sendMessageDo = async(
    messageObj ={
      messageContent,
      messageType,
      localFilePath,
      fileSize,
      fileName,
      filePath,
      fileType
    },cleanMsgContent) => {
    //TODO 判断文件大小
    if(!checkFileSize(messageObj.fileType,messageObj.fileSize,messageObj.fileName)){
      return;
    }
    if(messageObj.fileSize == 0){
      proxy.Confirm({
        message: `【${messageObj.fileName}】是一个空文件无法发送，请重新选择`,
        showCancelBtn:false,
      })
      return
    }
    messageObj.sessionId  = props.currentChatSession.sessionId;
    messageObj.sendUserId = userInfoStore.getInfo().sendUserId;
    let result = await proxy.Request({
       url: proxy.Api.sendMessage, 
       showLoading:false,
       params:{
        messageContent:messageObj.messageContent,
        contactId: props.currentChatSession.contactId,
        messageType:messageObj.messageType,
        fileSize:messageObj.fileSize,
        fileName:messageObj.fileName,
        fileType:messageObj.fileType,
       },
       showError:false,
       errorCallback:(responseData) => {
         proxy.Confirm({
           message: responseData.info,
           okfun:()=>{ 
            addContact(props.currentChatSession.contactId,responseData.code)
           },
           okText:'重新申请'

         })
       }
    })
    if(!result){
      return;
    }
    if(cleanMsgContent){
      msgContent.value = ''
    }
    Object.assign(messageObj,result.data)
    //TODO 更新列表
    emit("sendMessage4Local",messageObj)
    //TODO 保存消息到本地
    window.ipcRenderer.send('addLocalMessage',messageObj)
    
  }

  const uploadExceed = (files) => { 
    checkFileLimit(files)
  }

  // 文件上传
  const uploadRef = ref(null)
  // 选择文件后触发 这个file是el-upload在选择文件后自动封装的返回值  :http-request不可以换
  const uploadFile = async(file) => { 
    uploadFileDo(file.file);
    uploadRef.value.clearFiles() // 清空文件 选完就清

  }

  const uploadFileDo = (file) => {
    const fileType = getFileTypeByName(file.name)
   sendMessageDo({
    messageContent: '['+getFileType(fileType) +']',
    messageType :5 ,
    fileSize:file.size,
    fileName:file.name,
    filePath:file.path,
    fileType:fileType,
   },false)   
  }

    const getFileTypeByName = (fileName) => { 
    const fileSuffix = fileName.substr(fileName.lastIndexOf('.') + 1)
    return getFileType(fileSuffix)
  }

  // 添加好友
  const searchAddRef = ref(null)
  const addContact = async(contactId,code) => { 
    searchAddRef.value.show({
        contactId,
       contactType : code == 902 ? 'USER' : 'GROUP',
    })
  }

  // 检查文件大小
  const checkFileSize = (fileType,fileSize,fileName) => { 
     const SIZE_MB = 1024 * 1024
     const settingArray = Object.values(sysSettingStore.getSetting())
     const fileSizeNumber = settingArray[fileType]
     if(fileSize > fileSizeNumber * SIZE_MB){
      proxy.Confirm({
        message: `【${fileName}】文件过大，请选择小于${settingArray[fileType]}MB的文件`,
        showCancelBtn:false,
      })
      return false
     }
     return true
  } 

  // 发送文件数量
  const fileLimit = 10
  const checkFileLimit = (files) => { 
    if(files.length > fileLimit){
      proxy.Confirm({
        message: `最多只能发送${fileLimit}个文件`,
        showCancelBtn:false,
      })
      return
    }
    return true
  }

  // 拖入文件 拖入文件未松开
  const dragOverHandler = (e) =>{
    e.preventDefault();

  }

  // 已经托入文件
  const dropHandler = (e) =>{
    e.preventDefault();
    const files = e.dataTransfer.files
    if(!checkFileLimit(files)){
      return
    }
    for(let i = 0; i < files.length; i++){
      uploadFileDo(files[i])
    }
  }

  // 截图上传
  const pasteFile = async(e) => { 
    let items = e.clipboardData && e.clipboardData.items
    const fileData = {}

    for(const item of items){
      if(item.kind != 'file'){
        break
      }
      const file = await item.getAsFile()
      if(file.path != ""){ // 粘贴上传（能拿到路劲）
        uploadFileDo(file)
      }else{ // 如果是截图在上传
        const imageFile = new File([file],'temp.png')
        let fileReader = new FileReader()
        fileReader.onloadend = function() {
          const byteArray = new Uint8Array(this.result)
          fileData.byteArray = byteArray
          fileData.name = imageFile.name
          window.ipcRenderer.send('saveClipBoardFile',fileData)
        }
        fileReader.readAsArrayBuffer(imageFile)

      }
    }
  }

  onMounted(() => { 
    window.ipcRenderer.on('saveClipBoardFileCallback',(e,file) => { 
      const fileType = 0
      sendMessageDo({
        messageContent: '['+ getFileType(fileType) +']',
        messageType :5 ,
        fileSize:file.size,
        fileName:file.name,
        filePath:file.path,
        fileType:fileType,
      },false)
    })
  })

  
</script>

<style lang="scss" scoped>
.emoji-list {
  .emoji-item {
    float: left;
    font-size: 23px;
    padding: 2px;
    text-align: center;
    border-radius: 3px;
    margin-left: 10px;
    margin-top: 5px;
    cursor: pointer;
    &:hover {
      background: #ddd;
    }
  }
}

.send-panel {
  height: 200px;
  border-top: 1px solid #ddd;
  .toolbar {
    height: 40px;
    display: flex;
    align-items: center;
    padding-left: 10px;
    .iconfont {
      color: #494949;
      font-size: 20px;
      margin-left: 10px;
      cursor: pointer;
    }
    :deep(.el-tabs__header) {
      margin-bottom: 0px;
    }
  }
  .input-area {
    padding: 0px 10px;
    outline: none;
    width: 100%;
    height: 115px;
    overflow: auto;
    word-wrap: break-word;
    word-break: break-all;
    :deep(.el-textarea__inner) {
      box-shadow: none;
    }
    :deep(.el-input__count) {
      background: none;
      right: 12px;
    }
  }
  .send-btn-panel {
    text-align: right;
    padding-top: 10px;
    margin-right: 22px;
    .send-btn {
      cursor: pointer;
      color: #07c160;
      background: #e9e9e9;
      border-radius: 5px;
      padding: 8px 25px;
      &:hover {
        background: #d2d2d2;
      }
    }
  }
  .empty-msg {
    font-size: 13px;
  }
}
</style>
