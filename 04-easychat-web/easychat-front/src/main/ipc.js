import { app, shell, BrowserWindow ,ipcMain, session, dialog } from 'electron'
import { join } from 'path'
import { electronApp, optimizer, is } from '@electron-toolkit/utils'
import icon from '../../resources/icon.png?asset'
const NODE_ENV = process.env.NODE_ENV /* 打开控制台 */
import store from './store'
import {initWs, closeWs, sendWsMessage} from './wsClient'
import { addUserSetting, updateUserSetting } from './db/UserSettingUserModel'
import { selectUserSessionList, delChatSession, topChatSession, updateSessionInfo4Message, readAll, selectUserSessionByContactId, addChatSession } from './db/ChatSessionUserModel'
import { saveMessage, selectMessageList, updateMessage } from './db/ChatMessageModel'
import { createCover, saveAs, saveClipBoardFile, saveFile2local } from './file'
import { delWindow, getWindow, saveWindow } from './windowProxy'
import fs from 'fs'
import fse from 'fs-extra'

const onLoginOrRegister = (callback) => { 
    // 监听渲染进程消息 wok！ 渲染进程 P06 登录与注册边框自适应 
  ipcMain.on("loginOrRegister",(e,isLogin)=>{
    console.log("收到渲染进程消息：",isLogin);
    callback(isLogin)
  })
}

const onLoginSuccess = (callback) => { 
    ipcMain.on("openChat",(e,config)=>{

      store.initUserId(config.userId);
      store.setUserData("token",config.token);
      //增加用户配置
      addUserSetting(config.userId, config.email);

      callback(config);

      //初始化ws连接
      initWs(config,e.sender);
  })
}

const winTitleOp = (callback)=>{ 
   ipcMain.on("winTitleOp",(e,data)=>{
    callback(e,data);
   })
}

const onSetLocalStore = () => { 
  ipcMain.on("setLocalStore",(e,{key,value})=>{
     store.setData(key,value);
     console.log(store.getData(key))
})
}

const onGetLocalStore = () => { 
  ipcMain.on("getLocalStore",(e,key)=>{
    console.log("主进程收到渲染进程消息：",key);
    e.sender.send("getLocalStoreCallback",store.getData(key) )
  })
}

//### P34 会话列表01 on接受渲染进程通知后触发 查数据库得到result后再 send发通知附带result返回渲染进程
const onLoadSessionData=  () => {
   ipcMain.on("loadSessionData",async (e)=>{
      const result = await selectUserSessionList();
      e.sender.send("loadSessionDataCallback",result)
   })
}

// 创建新会话（用于"发消息"功能）
const onCreateChatSession = () => {
   ipcMain.on("createChatSession", async (e, contactId) => {
      try {
         // 先检查是否已存在该会话
         const existingSession = await selectUserSessionByContactId(contactId);
         if (existingSession) {
            // 已存在则直接返回
            e.sender.send("createChatSessionCallback", existingSession);
            return;
         }

         // 不存在则创建新会话
         // 生成唯一的 sessionId (格式: userId + contactId)
         const sessionId = store.getUserId() + contactId;
         const newSession = {
            contactId: contactId,
            sessionId: sessionId, // 关键！必须设置 sessionId
            contactName: '', // 将由调用方提供或后续更新
            lastMessage: '',
            lastReceiveTime: new Date().getTime(),
            contactType: 0, // 0=个人聊天, 1=群聊
            status: 1,
            topType: 0,
            noReadCount: 0
         };

         await addChatSession(newSession);

         // 查询刚创建的会话（获取完整数据包括 sessionId）
         const createdSession = await selectUserSessionByContactId(contactId);
         e.sender.send("createChatSessionCallback", createdSession);
      } catch (error) {
         console.error('创建会话失败:', error);
         e.sender.send("createChatSessionCallback", null);
      }
   });
}

// 删除会话
const onDelChatSession = () => { 
   ipcMain.on("delChatSession",async (e,contactId)=>{
       delChatSession(contactId)
   })
}  

// 置顶会话
const onTopChatSession = () => { 
  ipcMain.on("topChatSession",(e,{contactId,topType})=>{
     topChatSession(contactId,topType)
  })
}

// 加载聊天消息
const onLoadChatMessage = () => {
   ipcMain.on("loadChatMessage",async (e,data)=>{
     const result = await selectMessageList(data);
     e.sender.send("loadChatMessageCallback",result)
  })
 }

 // P42发送媒体消息----添加本地消息
 const onAddLocalMessage = () => { 
    ipcMain.on("addLocalMessage",async(e,data)=>{
      await saveMessage(data);
      // 保存文件
      if(data.messageType === 5){
        await saveFile2local(data.messageId,data.filePath,data.fileType)
        const updateInfo = {
          status:1,
        }
        await updateMessage(updateInfo,{messageId:data.messageId})
      }
      // 更新session
      data.lastReceiveTime = data.sendTime;
      // TODO 更新会话
      updateSessionInfo4Message(store.getUserData("currentSessionId"),data)
      e.sender.send("addLocalCallback",{status:1,messageId:data.messageId})

    })
 }  

 // 选中会话
 const onSetSessionSelect = async(e,data) => { 
      ipcMain.on("setSessionSelect",async(contactId,sessionId)=>{ 
      
        if(sessionId){
          store.setUserData("currentSessionId",sessionId)
          readAll(contactId)
        }else{
          store.deleteUserData("currentSessionId")
        }
})
 }


 const onCreateCover = () => { 
   ipcMain.on("createCover",async (e,localFilePath)=>{
     const stream = await createCover(localFilePath) // 这里返回的流中包含的两个照片 都是——temp就是那个正常的
     e.sender.send("createCoverCallback",stream)
   })
}

//## p50 查看媒体文件01 on接受渲染进程通知后触发 查数据库得到result后再 send发通知附带result返回渲染进程
const onOpenNewWindow = () => { 
  ipcMain.on("newWindow",(e,config)=>{
     openWindow(config)
  })
}

const openWindow = ({ windowId, title = "Noshy Chat", path, width = 960, height = 720, data }) => {
  const localServerPort = store.getUserData("localServerPort")
  data.localServerPort = localServerPort
  let newWindow = getWindow(windowId)
  if (!newWindow) { // 如果没有这个窗口
    newWindow = new BrowserWindow({
      icon: icon,
      width: width,
      height: height,
      fullscreenable: false,
      fullscreen: false,
      maximizable: false,
      autoHideMenuBar: true,
      titleBarStyle: 'hidden', // 边框样式
      resizable: false, // 禁止拉伸
      frame: true, // 边框
      transparent: true, // 透明
      hasShadow: false,
      webPreferences: {
        preload: join(__dirname, '../preload/index.js'),
        sandbox: false,
        contextIsolation: false, // 是否开启上下文隔离
      }
    })
    saveWindow(windowId, newWindow)
    newWindow.setMinimumSize(600, 484)
    if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
      newWindow.loadURL(`${process.env['ELECTRON_RENDERER_URL']}/index.html#${path}`)
    } else {
      newWindow.loadFile(join(__dirname, '../renderer/index.html'), { hash: `${path}` })
    }
    if (NODE_ENV === 'development') {
      newWindow.webContents.openDevTools()
    }
    newWindow.on('ready-to-show', () => {
      newWindow.setTitle(title)
      newWindow.show()
    })
    newWindow.once('show', () => {
      setTimeout(() => {
        newWindow.webContents.send("pageInitData", data)
      }, 500)
    })
    newWindow.on('close', () => {
      delWindow(windowId)
    })
  }else{ // 如果有这个窗口
    newWindow.show()
    newWindow.setSkipTaskbar(false);
    newWindow.webContents.send("pageInitData", data)
  }
}

const onSaveAs = () => { 
  ipcMain.on("saveAs",async(e,data)=>{
     saveAs(data)
  })
}

const onSaveClipBoardFile = () => { 
  ipcMain.on("saveClipBoardFile",async(e,data)=>{
    const result = await saveClipBoardFile(data)
    e.sender.send("saveClipBoardFileCallback",result)
  })
}

const onReLogin = (callback) => {
  ipcMain.on("reLogin", (e) => {
    console.log("收到重连/退出登录消息");
    closeWs();
    callback();
  })
}

const onOpenLocalFolder = () => {
  ipcMain.on("openLocalFolder", (e) => {
    const localFolder = store.getUserData("localFileFolder");
    if (localFolder) {
      if (!fs.existsSync(localFolder)) {
        fs.mkdirSync(localFolder, { recursive: true });
      }
      shell.openPath(localFolder);
    }
  })
}

const onChangeFileFolder = () => {
  ipcMain.on("changeFileFolder", async (e) => {
    const win = BrowserWindow.fromWebContents(e.sender);
    const result = await dialog.showOpenDialog(win, {
      properties: ['openDirectory']
    });
    if (result.canceled || result.filePaths.length === 0) {
      return;
    }
    const newDirPath = result.filePaths[0];
    const newBaseDir = newDirPath.endsWith("\\") || newDirPath.endsWith("/") ? newDirPath : newDirPath + "\\";
    const userId = store.getUserId();
    const newUserDir = newBaseDir + userId;
    const oldUserDir = store.getUserData("localFileFolder");
    
    // 通知渲染进程开始复制
    e.sender.send("changeFileFolderCallback", { status: "start" });
    
    try {
      // 复制文件夹内容
      if (fs.existsSync(oldUserDir)) {
        fse.copySync(oldUserDir, newUserDir);
      }
      
      // 更新内存存储
      store.setUserData("localFileFolder", newUserDir);
      
      // 更新数据库
      const sysSetting = {
        localFileFolder: newBaseDir
      };
      await updateUserSetting(userId, sysSetting);
      
      // 通知渲染进程复制完成并返回新路径
      e.sender.send("changeFileFolderCallback", { status: "success", path: newBaseDir });
    } catch (err) {
      console.error("更改文件夹失败:", err);
      e.sender.send("changeFileFolderCallback", { status: "error", msg: err.message });
    }
  });
}

const onOpenUrl = () => {
  ipcMain.on("openUrl", (e, url) => {
    shell.openExternal(url);
  })
}

export {
    onLoginOrRegister,
    onLoginSuccess,
    winTitleOp,
    onGetLocalStore,
    onSetLocalStore,
    onLoadSessionData,
    onCreateChatSession,
    onDelChatSession,
    onTopChatSession,
    onLoadChatMessage,
    onAddLocalMessage,
    onSetSessionSelect,
    onCreateCover,
    onOpenNewWindow,
    onSaveAs,
    onSaveClipBoardFile,
    onReLogin,
    onOpenLocalFolder,
    onChangeFileFolder,
    onOpenUrl
}