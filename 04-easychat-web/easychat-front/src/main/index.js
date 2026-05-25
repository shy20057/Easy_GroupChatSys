import { app, shell, BrowserWindow ,Menu,Tray } from 'electron'

import { join } from 'path'
import { electronApp, optimizer, is } from '@electron-toolkit/utils'
import icon from '../../resources/icon.png?asset'
const NODE_ENV = process.env.NODE_ENV /* 打开控制台 */

import { onGetLocalStore, onLoginOrRegister,onLoginSuccess,onSetLocalStore,winTitleOp,onLoadSessionData, onCreateChatSession, onDelChatSession,onTopChatSession, onLoadChatMessage, onAddLocalMessage, onSetSessionSelect, onCreateCover, onOpenNewWindow, onSaveAs, onSaveClipBoardFile, onReLogin, onOpenLocalFolder, onChangeFileFolder, onOpenUrl} from './ipc'
import { createTable } from './db/ADB'
import { saveWindow } from './windowProxy'

const login_width =300;
const login_height = 370;
const register_height = 490;

// 将托盘和主窗口声明为全局变量，确保在函数作用域外可访问
let mainWindow = null;
let tray = null;

function createWindow() {
  // Create the browser window.
  mainWindow = new BrowserWindow({
    title: 'Noshy Chat',
    icon: icon,
    width: login_width,
    height: login_height,
    show: false,
    autoHideMenuBar: true,
    titleBarStyle: 'hidden', // 边框样式
    resizable: false, // 禁止拉伸
    frame: true, // 边框
    transparent: true, // 透明
  
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false,
      contextIsolation: false, // 是否开启上下文隔离
    }
  });

  //## p50
  saveWindow("main",mainWindow)

  if (NODE_ENV === 'development') {
    mainWindow.webContents.openDevTools();
  }

  mainWindow.on('ready-to-show', () => {
    mainWindow.show()
    mainWindow.setTitle("Noshy Chat")
  })

  mainWindow.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url)
    return { action: 'deny' }
  })

  // HMR for renderer base on electron-vite cli.
  // Load the remote URL for development or the local html file for production.
  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    mainWindow.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
  }

    // // 监听渲染进程消息 wok！ 渲染进程 P06 登录与注册边框自适应   抽出来了
  // ipcMain.on("loginOrRegister",(e,isLogin)=>{
  //   console.log("收到渲染进程消息：",isLogin);
  //   mainWindow.setResizable(true);
  //   if(isLogin){
  //       mainWindow.setSize(login_width, login_height);
  //   }else{
  //       mainWindow.setSize(login_width, register_height);
  //   }
  //   mainWindow.setResizable(false); 
  // })

  // 监听 登录注册 自适应屏幕大小
  onLoginOrRegister((isLogin)=>{ 
    mainWindow.setResizable(true);
    if(isLogin){
        mainWindow.setSize(login_width, login_height);
    }else{
        mainWindow.setSize(login_width, register_height);
    }
    mainWindow.setResizable(false); 
  })

  // 监听登录成功  自适应屏幕大小
  onLoginSuccess((config)=>{
    mainWindow.setResizable(true); // 可拖动
    mainWindow.setSize(850,800);
    // 居中显示
    mainWindow.center();
    // 可以最大化
    mainWindow.setMaximizable(true);
    // 设置最小的窗口大小
    mainWindow.setMinimumSize(800,600)
    
    //TODO 管理后台窗口操作，托盘操作
    if(config.admin){

    }
    
    // 更新托盘菜单，添加用户信息
    if (tray) {
      const contextMenu = [
        { label: "用户：" + config.nickName, click: function() {} },
        {
          label: '退出PigChat', click: function() {
            app.exit()
          }
        }
      ];
      const menu = Menu.buildFromTemplate(contextMenu);
      tray.setContextMenu(menu);
    }
  });

  // 托盘处理
  tray = new Tray(icon);
  let contextMenu = [
    {
      label: '退出登录', click: function() {
        if (mainWindow) {
          mainWindow.hide(); // 隐藏窗口
          mainWindow.setSkipTaskbar(true); // 设置任务栏图标不显示
        }
      }
    },
    {
      label: '退出PigChat', click: function() {
        app.quit(); // 退出程序
      }
    }
  ];
  const menu = Menu.buildFromTemplate(contextMenu);
  tray.setToolTip('PigChat');
  tray.setContextMenu(menu);
  tray.on('click', () => {
    if (!mainWindow || mainWindow.isDestroyed()) {
      createWindow();
    } else {
      mainWindow.setSkipTaskbar(false);
      mainWindow.show();
    }
  });

  // 主进程控制窗口关闭 隐藏 放大缩小
  winTitleOp((e, { action, data }) => {
    const webContents = e.sender;
    const win = BrowserWindow.fromWebContents(webContents);
    switch (action) {
      case "close": {
        if (data.closeType === 0) {
          // 如果是主窗口，不关闭，只隐藏
          if (win === mainWindow) {
            win.hide();
            win.setSkipTaskbar(true);
          } else {
            win.close(); // 其他窗口正常关闭
          }
        } else {
          win.setSkipTaskbar(true);
          win.hide();
        }
        break;
      }
      case "minimize": {
        win.minimize();
        break;
      }
      case "maximize": {
        win.maximize();
        break;
      }
      case "unmaximize": {
        win.unmaximize();
        break;
      }
      case "top": {
        win.setAlwaysOnTop(data.top);
        break;
      }
    }
  });

  onSetLocalStore()
  onGetLocalStore()
  onLoadSessionData()
  onCreateChatSession()
  onDelChatSession()
  onTopChatSession()
  onLoadChatMessage()
  onAddLocalMessage()
  onSetSessionSelect()
  onCreateCover()
  onOpenNewWindow()
  onSaveAs()
  onSaveClipBoardFile()
  onOpenLocalFolder()
  onChangeFileFolder()
  onOpenUrl()

  onReLogin(() => {
    mainWindow.setResizable(true);
    mainWindow.setSize(login_width, login_height);
    mainWindow.setResizable(false);
    mainWindow.center();
    
    // 重新加载页面，从而重置所有的Vue路由和Store状态
    if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
      mainWindow.loadURL(process.env['ELECTRON_RENDERER_URL'])
    } else {
      mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
    }
  })

  // 拦截主窗口的关闭事件，改为隐藏到托盘
  mainWindow.on('close', (event) => {
    event.preventDefault();
    mainWindow.hide();
    mainWindow.setSkipTaskbar(true);
  });
}

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.whenReady().then(() => {
  // Set app user model id for windows
  electronApp.setAppUserModelId('com.electron')

  // Default open or close DevTools by F12 in development
  // and ignore CommandOrControl + R in production.
  // see https://github.com/alex8088/electron-toolkit/tree/master/packages/utils
  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })

  createWindow()

  app.on('activate', function () {
    // On macOS it's common to re-create a window in the app when the
    // dock icon is clicked and there are no other windows open.
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

// Quit when all windows are closed, except on macOS. There, it's common
// for applications and their menu bar to stay active until the user quits
// explicitly with Cmd + Q.
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    return; // 不退出应用，保持托盘运行
  }
  app.quit();
})

// In this file you can include the rest of your app"s specific main process
// code. You can also put them in separate files and require them here.