import {run,queryAll,queryOne,queryCount,insert,insertOrReplace,insertOrIgnore,update} from "./ADB.js"  
import store from "../store.js"
import { updateNoReadCount } from "./ChatSessionUserModel.js"
import { session } from "electron";

const saveMessage = (data) => {
   data.userId =store.getUserId();
   return insertOrReplace('chat_message',data)
}

const saveMessageBatch = (chatMessageList) => {
    return new Promise(async (resolve, reject) => {
        const chatSessionCountMap = {}
        chatMessageList.forEach(item => {
            let contactId = item.contactType == 1 ? item.contactId : item.sendUserId;
            let noReadCount = chatSessionCountMap[contactId];
            if(!noReadCount){
                chatSessionCountMap[contactId] = 1;
            }else{
                chatSessionCountMap[contactId] = noReadCount + 1;
            }
        });

        // 更新未读数
        for(let item in chatSessionCountMap){
            await updateNoReadCount({contactId:item,noReadCount:chatSessionCountMap[item]});
        }

        // 批量插入数据
        chatMessageList.forEach(async item => {
           await saveMessage(item)
         })

    })

}

const getPageOffset = (pageNo=1,totalCount) => { 
   const pageSize =20;
   const pageTotal =totalCount % pageSize == 0 ? totalCount / pageSize : Number.parseInt(totalCount / pageSize) + 1
   pageNo = pageNo <=1 ? 1 : pageNo
   pageNo = pageNo >= pageTotal ? pageTotal : pageNo
   return {pageTotal,offset:pageSize * (pageNo - 1),limit:pageSize}
}

const selectMessageList = (query) => {
    return new Promise(async (resolve, reject) => {
      
         const  { sessionId, pageNo, maxMessageId } =query
         let sql = `select count(1) from chat_message where session_id = ?`
         
         const totalCount = await queryCount(sql, [sessionId])
         const {pageTotal,offset,limit} = getPageOffset(pageNo,totalCount)

         const params = [sessionId]
         sql = "select * from chat_message where session_id = ?"
         if(maxMessageId){
            sql = sql + " and message_id < ?"
            params.push(maxMessageId)
         }
        params.push(offset)
        params.push(limit)
        sql = sql + " order by message_id desc limit ?,?"
        const dataList = await queryAll(sql, params)
        resolve({dataList,pageTotal,pageNo})
    })
}

// 更新消息 从数据库中更新消息
const updateMessage = (data,paramData) => {
    paramData.userId = store.getUserId()
    return update("chat_message",data,paramData);

}

const selectByMessageId = (messageId) => { 
    let sql = "select * from chat_message where message_id = ? and user_id = ?" 
    const params = [messageId,store.getUserId()]
    return queryOne(sql, params)
}

export {
    saveMessage,
    saveMessageBatch,
    selectMessageList,
    updateMessage,
    selectByMessageId
}