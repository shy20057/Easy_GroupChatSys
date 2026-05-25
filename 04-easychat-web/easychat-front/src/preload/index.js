import { contextBridge, ipcRenderer } from 'electron'
import { electronAPI } from '@electron-toolkit/preload'
window.ipcRenderer = ipcRenderer; // 联通渲染进程
// Custom APIs for renderer
const api = {}

// Use `contextBridge` APIs to expose Electron APIs to
// renderer only if context isolation is enabled, otherwise
// just add to the DOM global.
if (process.contextIsolated) {
  try {
    contextBridge.exposeInMainWorld('electron', electronAPI)
    contextBridge.exposeInMainWorld('api', api)
  } catch (error) {
    console.error(error)
  }
} else {
  window.electron = electronAPI
  window.api = api
}

// preload目录 - 预加载脚本
// index.js: 预加载脚本，作为主进程和渲染进程之间的安全桥梁
// 使用contextBridge暴露API给渲染进程
// 通过ipcRenderer实现进程间通信
// 提供安全的API访问机制