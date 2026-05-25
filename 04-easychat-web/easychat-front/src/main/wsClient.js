import WebSocket from "ws";  // package.json中已配置ws
const NODE_ENV = process.env.NODE_ENV; // 环境变量

import store from "./store"
import { saveOrUpdate4Message, saveOrUpdateChatSessionBatch4Init,selectUserSessionByContactId } from "./db/ChatSessionUserModel";
import { saveMessage, saveMessageBatch, updateMessage } from "./db/ChatMessageModel"; // 导入缺失的函数
import { updateContactNoReadCount } from "./db/UserSettingUserModel";
import { session } from "electron";

let ws = null;
let maxReConnectTimes = null;
let lockReconnect = false;
let wsUrl =null;
let sender = null;
let needReconnect = null; 

const initWs = (config,_sender) =>{
    // 从config对象中提取token，而不是直接使用整个config对象
    const token = typeof config === 'object' ? config.token : config;
    wsUrl = `${NODE_ENV === "development" ? store.getData("devWsDomain") : store.getData("proWsDomain")}?token=${token}`;
    sender = _sender;
    needReconnect = true;
    maxReConnectTimes = 5;

    createWs();
    
 }

 const closeWs = ()=>{
    needReconnect = false;
    if (ws) {
        ws.close();
    }
 }

 const createWs = ()=>{ 
   if(wsUrl == null){
    return;
   }
   ws = new WebSocket(wsUrl);
   ws.onopen = function () { 
     console.log("ws客户端连接成功");
     ws.send("heart beat")
     maxReConnectTimes = 5;
   }
   //从服务器接受到信息的回调函数 这些特殊的Websocket方法和服务端的真挺像的
   ws.onmessage = async function (e) {
      // console.log("ws客户端收到消息：",e.data);
      try {
        const message = JSON.parse(e.data); 
        const messageType = message.messageType;
        switch (messageType) {
          case 0: //ws连接成功
            // 保存会话消息
            await saveOrUpdateChatSessionBatch4Init(message.extendData.chatSessionList);

            // 保存消息
            await saveMessageBatch(message.extendData.chatMessageList);

            // 更新联系人数量
            await updateContactNoReadCount({ userId: store.getUserId, noReadCount: message.extendData.noReadCount });

            // 发送消息至渲染端
            sender.send("receiveMessage", { messageType: message.messageType });
            break;
          // ### p49
          case 6: // 文件上传完成
             updateMessage({status:message.status},{messageId:message.messageId});
             sender.send("receiveMessage",message)
             break;
          case 2: // 聊天消息
          case 5: // 图片 视频消息
            if(message.sendUserId == store.getUserId && message.contactType == 1){
              break;
            }
          const sessionInfo = {}
          if(message.extendData && typeof message.extendData=="object"){
            Object.assign(sessionInfo,message.extendData)
          }else{
            Object.assign(sessionInfo,message)
            if(message.contactType == 0 && message.contactType != 1){
              sessionInfo.contactName = message.sendUserNickName;
            }
            sessionInfo.lastReceiveTime = message.sendTime;
          }
            
          await saveOrUpdate4Message(store.getUserData("currentSessionId"),sessionInfo);
          // 写入本地消息
          await saveMessage(message);

          const dbSessionInfo = await selectUserSessionByContactId(message.contactId)
          message.extendData = dbSessionInfo
          sender.send("receiveMessage",message)
          break;


        }
      } catch (error) {
        console.error('处理WebSocket消息时出错:', error);
      }
    }

    ws.onclose = function () { 
        console.log("ws客户端连接关闭");
        reconnect()
    }

    ws.onerror = function () { 
        console.log("ws客户端连接错误");
        reconnect()
    }


    const reconnect = ()=>{
        if(!needReconnect){
            console.log("连接断开，不需要重连");
            return;
        }
        if(ws != null){
            ws.close()
        }
        if(lockReconnect){
            return;
        }
        lockReconnect = true;
        if(maxReConnectTimes > 0){
            console.log("ws客户端准备重连，剩余重连次数："+maxReConnectTimes,new Date().getTime());
            maxReConnectTimes--;
            setTimeout(()=>{
                 createWs()
                 lockReconnect = false;
            },5000)
        }else{
            console.log("ws客户端重连失败，已断开重连");
        }
     }

     setInterval(()=>{
       if(ws != null && ws.readyState === 1){
        ws.send("heart beat")
       }
     },5000)

}   

 export {
    initWs,
    closeWs,
 }