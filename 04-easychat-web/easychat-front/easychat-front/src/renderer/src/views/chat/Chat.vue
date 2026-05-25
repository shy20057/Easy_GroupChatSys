const onReceiveMessage = () => {
  window.ipcRenderer.on("receiveMessage",(e,message) =>{
    console.log("收到消息",message);

    let curSession = chatSessionList.value.find( (item) =>{
      return item.sessionId == message.sessionId
    })

    if(curSession == null){
      chatSessionList.value.push(message.extendData)
    }else { 
        Object.assign(currentChatSession.value,message.extendData)
    }
    sortChatSessionList(chatSessionList.value)
    if(message.sessionId != currentChatSession.value.sessionId){
      //TODO 未读消息气泡
    }else{
      // 检查消息是否已存在
      const exists = messageList.value.some(msg => msg.messageId === message.messageId);
      if (!exists) {
        messageList.value.push(message)
        gotoBottom()
      }
    }
   
  })
}

const loadChatMessage =  () => { 
  if(messageCountInfo.noData){
     return
  }
  messageCountInfo.pageNo++
  console.log( '加载消息列表参数：', currentChatSession.value.sessionId,messageCountInfo.pageNo,messageCountInfo.maxMessageId)
  window.ipcRenderer.send("loadChatMessage", {
    sessionId: currentChatSession.value.sessionId,
    pageNo: messageCountInfo.pageNo,
    maxMessage: messageCountInfo.maxMessageId,
  })
  
}

// 修改 loadChatMessageCallback 回调函数，过滤已存在的消息
const onLoadChatMessage =  (data) => { 
   window.ipcRenderer.on("loadChatMessageCallback",(e,result) =>{ 

    const {dataList, pageTotal, pageNo} = result; 
  
    if(pageNo == pageTotal){
      messageCountInfo.noData =  true
    }
    dataList.sort( (a,b)=>{
      return a.messageId -  b.messageId
    })
    
    // 过滤掉已存在的消息
    const newMessages = dataList.filter(msg => !messageList.value.some(existingMsg => existingMsg.messageId === msg.messageId));
    messageList.value = messageList.value.concat(newMessages) 
    messageCountInfo.pageNo =pageNo;
    messageCountInfo.pageTotal =  pageTotal;
    if(pageNo ==1){
      messageCountInfo.maxMessageId =dataList.length > 0? dataList[dataList.length  - 1].messageId:null
      // 滚动条滚动到最底部
      gotoBottom()
    }
    console.log('获取到的消息列表：', dataList) 
    
  })
}