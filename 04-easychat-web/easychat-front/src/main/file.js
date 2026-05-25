const fs = require('fs');  // 操作文件
const fse = require('fs-extra'); // fs的扩展
const NODE_ENV = process.env.NODE_ENV // 获取当前 Node.js 环境变量中的环境标识（如 development 或 production），用于区分开发和生产环境
const path = require('path'); // 用于处理和转换文件路径，确保不同操作系统的兼容性
const { app, ipcMain, shell,dialog,BrowserView } = require('electron') // app控制应用的生命周期 ipcMain：处理主进程与渲染进程之间的 IPC 通信 shell：提供原生操作系统的相关功能，如打开文件夹等
const { exec } = require("child_process"); // 从 child_process 模块中解构导入 exec 方法，用于执行系统命令（如 ffmpeg 命令）
const FormData = require('form-data'); // 用于构建 multipart/form-data 格式的表单数据，通常用于文件上传
const axios = require('axios'); // 用于发起 HTTP 请求，如与后端 API 通信
import { response } from "express"; // 用于处理 HTTP 响应
import { selectByMessageId } from "./db/ChatMessageModel";
import store from "./store"
import { title } from "process";
const moment = require('moment') //用于处理日期时间格式化等操作
moment.locale('zh-cn', {}); // 设置 moment 库的本地化语言为中文，用于显示中文日期格式
// express 服务器
const express = require('express'); // 用于在 Electron 主进程中创建本地 HTTP 服务器
const expressServer = express(); // 创建 Express 应用实例，作为本地文件服务的服务器对象


const cover_image_suffix = "_cover.png" 
const image_suffix = ".png"

const ffprobePath = "/assets/ffprobe.exe"
const ffmpegPath = "/assets/ffmpeg.exe"

const getDomain = () => { 
    return NODE_ENV !== "development" ? store.getData("prodDomain") : store.getData("devDomain")  
}
// 创建多层目录 确保创建子目录前先创建父目录
const mkdirs = (dir) => {
    if(!fs.existsSync(dir)){
        const parentDir = path.dirname(dir); // dirname 从路径上提取目录 path.dirname('/foo/bar/baz/asdf/quux') 会返回 '/foo/bar/baz/asdf'
        if(parentDir != dir){
            mkdirs(parentDir); 
        }
        fs.mkdirSync(dir); 
    }
}
const getLocalFilePath = (partType,showCover,fileId) => {
    
    return new Promise(async(resolve, reject) => { 
        let localFolder = store.getUserData("localFileFolder") // C:/Users/s3324/.easychat/fileStorage/U0889234722
        let localPath = null;
        if(partType == "avatar"){
           localFolder = localFolder + '/avatar/'
           if(!fs.existsSync(localFolder)){
            mkdirs(localFolder)
           }
           localPath = localFolder  + fileId + image_suffix
        }else if(partType == "chat"){
            let messageInfo = await selectByMessageId(fileId)
            const month = moment(Number.parseInt(messageInfo.sendTime)).format('YYYYMM')
            localFolder = localFolder + '/' + month;
            if(!fs.existsSync(localFolder)){
                mkdirs(localFolder)
            }
            let fileSuffix = messageInfo.fileName;
            fileSuffix = fileSuffix.substring(fileSuffix.lastIndexOf(".")) 
            localPath = localFolder + '/' + fileId + fileSuffix
        }else if(partType == "tmp"){
            localFolder = localFolder + '/tmp/'
            if(!fs.existsSync(localFolder)){
                mkdirs(localFolder)
            }
            localPath = localFolder + '/' + fileId 
        }
        if(showCover){
            localPath = localPath + cover_image_suffix
        }
        resolve(localPath)
    })
}

const getResourcesPath = () => { 
    let resourcesPath = app.getAppPath() // 获取项目路径
    if(NODE_ENV !== "development"){ // 生产环境 会将ffmpeg和ffmpeg移动到resources/assets/目录下
        resourcesPath = path.dirname(app.getPath("exe")+"/resources")
    }
    return resourcesPath
}
const getFFprobePath = () => { 
   return path.join(getResourcesPath(),ffprobePath)
}   
const getFFmegPath = () => { 
    return path.join(getResourcesPath(),ffmpegPath)
}

const execCommand = (command) => {      
    return new Promise((resolve, reject) => { 
        exec(command, (error, stdout, stderr) => { 
            console.log("ffmpeg命令:",command)
            if (error) { 
                console.error("执行命令失败:",error)
            }
            resolve(stdout) 
})
    })
}

// savePath 原图   coverPath 封面图
const uploadFile = (messageId, savePath, coverPath) => {
    const formData = new FormData() // 用于构建 multipart/form-data 格式的表单数据，通常用于文件上传
    formData.append("messageId", messageId)
    formData.append("file", fs.createReadStream(savePath)) // 创建一个可读流来读取savePath下的文件
    if (coverPath) {
        formData.append("cover", fs.createReadStream(coverPath))
    }
    const url = `${getDomain()}/api/chat/uploadFile`
    const token = store.getUserData("token")
    const config = { headers: { 'Content-Type': 'multipart/form-data', "token": token } } // 配置请求头（headers） 注意区分后面的响应头信息判断
    axios.post(url, formData, config).then((response) => { //formData前端构建的multipart/form-data 格式的数据，可以同时发送文件和其他字段，发送到服务器进行文件存储   config这是请求的配置选项，通常包含请求头信息。在这里设置了认证 token 和内容类型
    }).catch((error) => {
        console.log("文件上传失败", error)
    })
}

// 重点方法
const saveFile2local = (messageId, filePath, fileType) => { // 
    // promise 将异步操作整成同步的
    return new Promise(async (resolve, reject) => {    
        let ffmpegPath = getFFmegPath()                  // C:/Users/s3324/.easychat/fileStorage/U0889234722/avatar/U0889234722.png 不保持原格式
        let savePath = await getLocalFilePath("chat", false, messageId) // C:/Users/s3324/.easychat/fileStorage/U0889234722/2026-01/1778.保持原格式
        fs.copyFileSync(filePath, savePath) // 将 filePath(E:\\Download_loading\\微信图片_20251019154817_209_86.jpg) 路径下的文件(微信图片_20251019154817_209_86.jpg)复制到 savePath (1778.png)  ---> 完成了上传 加 规则命名
        let coverPath = null;
        if (fileType != 2) { // 文件类型为视频
            let command = `${getFFprobePath()} -v error -select_streams v:0 -show_entries stream=codec_name "${filePath}"` // filePath(E:\\Download_loading\\微信图片_20251019154817_209_86.jpg)
            /* 
              command分析：1，-v error - 设置日志级别为错误级别，只输出错误信息，减少冗余输出 2，-select_streams v:0 - 选择视频流的第一个流（v:0 表示第一个视频流） 3，-show_entries stream=codec_name - 指定输出流的编码名称信息 4，"${filePath}" - 要检测的视频文件路径
            */
            let result = await execCommand(command)
            result = result.replaceAll("\r\n", '')
            result = result.substring(result.indexOf('=') + 1) //从=开始截取到字符串的末尾 h264[/STREAM]
            let codec = result.substring(0, result.indexOf('[')) // h264
            console.log("视频编码:", codec)
            if (codec == "hevc") {  // -c:v libx264 - 设置视频编码器为 libx264 (H.264 编码) 兼容性
                command = `${ffmpegPath} -y -i "${filePath}" -c:v libx264 -crf 20 "${savePath}"`;
                await execCommand(command);
            }
            coverPath = savePath + cover_image_suffix;
            command = `${ffmpegPath} -i "${savePath}" -y -vframes 1 -vf "scale=min(170\\,iw*min(170/iw\\,170/ih)):min(170\\,ih*min(170/iw\\,170/ih))" "${coverPath}"` 
            await execCommand(command);
        }
        uploadFile(messageId, savePath, coverPath)
        resolve()
    })
}

// 关联本地服务器获取媒体消息
let server = null;
const startLocalServer = (serverPort)=>{ 
  server = expressServer.listen(serverPort,()=>{
    console.log("本地文件服务启动成功:http://127.0.0.1:"+serverPort)

  })
}
const closeLocalServer = ()=>{ 
    if(server){
        server.close()
    }
}

//express 本地服务器 (本地浏览器)
const FILE_TYPE_CONTENT_TYPE = {
    "0":"image/",
    "1":"video/",
    "2":"application/octet-stream",
}

//从服务器下载文件
const downloadFile = async (fileId,showCover,savePath,partType)=>{
    showCover = showCover + ""
    let url = `${getDomain()}/api/chat/downloadFile`
    const token = store.getUserData("token")
    return new Promise(async(resolve, reject) => { 
        const config = { // 配置请求头
            responseType: "stream",
            headers: { 'Content-Type': 'multipart/form-data',"token":token}
        }
        let response = await axios.post(url,{fileId,showCover},config)
        const folder = savePath.substring(0,savePath.lastIndexOf('/'))
        mkdirs(folder)
        const stream = fs.createWriteStream(savePath)
        if(response.headers["content-type"] == "application/json"){
            let resourcesPath = getResourcesPath()
            if(partType == "avatar"){
                fs.createReadStream(resourcesPath + '/assets/user.png').pipe(stream)
            }else{
                fs.createReadStream(resourcesPath + '/assets/404.png').pipe(stream)
            }
        }else{
            response.data.pipe(stream)
        }
        stream.on("finish",()=>{
            stream.close()
            resolve()
        })
    })
}



// expressServer是express.js的实例  这里开启了一个本地的服务端 也就是意味着这个项目有三个服务端
expressServer.get("/file",async(req,res)=>{
    //partType:avatar,chat | fileType:0-图片,1-视频,2-文件 | fileId:文件id | showCover:是否显示封面 | forceGet:是否强制从服务器下载
    let {partType,fileType,fileId,showCover,forceGet} = req.query;
    if(!partType || !fileId){
        res.send("请求参数错误")
        return
    }
    showCover = showCover == undefined?false:Boolean(showCover)
    const localPath = await getLocalFilePath(partType,showCover,fileId)

    if(!fs.existsSync(localPath)||forceGet == "true"){ // 文件在本地不存在 或者强制必须下载
         if(forceGet == "true" && partType == "avatar"){
            await downloadFile(fileId,true,localPath+cover_image_suffix,partType) 
         }
         await downloadFile(fileId,showCover,localPath,partType);
    }
    const fileSuffix = localPath.substring(localPath.lastIndexOf('.')+1)
    let contentType = FILE_TYPE_CONTENT_TYPE[fileType] + fileSuffix
    res.setHeader("Access-Control-Allow-Origin", "*") // 配置响应头 允许跨域请求
    res.setHeader("Content-Type",contentType)
    if(showCover || fileType != 1){
      fs.createReadStream(localPath).pipe(res)
      return;
    }

    let stat = fs.statSync(localPath)
    let fileSize = stat.size;
    let range = req.headers.range;
    if(range) {
        let parts = range.replace(/bytes=/, "").split("-");
        let start = parseInt(parts[0], 10);
        let end = parts[1] ? parseInt(parts[1], 10) : start + 9999999999;
        end = end > fileSize -1 ? fileSize-1 : end;
        let chunksize = (end-start)+1;
        let stream = fs.createReadStream(localPath, { start, end });
        let headers = {
            'Content-Range': `bytes ${start}-${end}/${fileSize}`,
            'Accept-Ranges': 'bytes',
            'Content-Length': chunksize,
            'Content-Type': 'video/mp4'
        }
        res.writeHead(206, headers);
        stream.pipe(res);
    }else { 
        let head = {
            'Content-Length': fileSize,
            'Content-Type': 'video/mp4'
        }
        res.writeHead(200, head);
        fs.createReadStream(localPath).pipe(res);
    }

    return;
})

//### p48 创建封面路径的buffer流
const createCover = (filePath)=>{
    return new Promise(async(resolve, reject) => { 
        let ffmpegPath = getFFmegPath()
        let avatarPath = await getLocalFilePath("avatar",false,store.getUserId()+"_temp") // getLocalFilePath将目标文件拷贝到系统文件，并返回文件路径
        let command = `${ffmpegPath} -i "${filePath}" "${avatarPath}" -y`
        await execCommand(command)

        let coverPath = await getLocalFilePath("avatar",false,store.getUserId()+"_temp_cover")
        command = `${ffmpegPath} -i "${filePath}" -y -vframes 1 -vf "scale=min(170\\,iw*min(170/iw\\,170/ih)):min(170\\,ih*min(170/iw\\,170/ih))" "${coverPath}"` 
        await execCommand(command)

        resolve({
            avatarStream:fs.readFileSync(avatarPath),
            coverStream:fs.readFileSync(coverPath)
        })

    })
}

const saveAs = async (data)=>{

    const { partType, fileId } = data;

    let fileName = ""
    if(partType == "avatar"){
        fileName = fileId + image_suffix
    }else if(partType == "chat"){
        let messageInfo = await selectByMessageId(fileId)
        fileName = messageInfo.fileName
    }
    const localPath = await getLocalFilePath(partType,false,fileId)
    const options = {
        title: '保存文件',
        defaultPath: fileName,
    }
    let result = await dialog.showSaveDialog(options)
    if(result.canceled || result.filePath == "" ){
        return
    }
    const filePath = result.filePath
    fs.copyFileSync(localPath,filePath)
}

const saveClipBoardFile = async(file) =>{ 
     const fileSuffix = file.name.substring(file.name.lastIndexOf('.')+1)
     const filePath =await getLocalFilePath("tmp",false,"temp"+fileSuffix)
     let byteArray = file.byteArray;
     const buffer = Buffer.from(byteArray)  
     fs.writeFileSync(filePath,buffer)
     return {
        size:byteArray.length,
        name:file.name,
        path:filePath,
     }
}

export {
    saveFile2local,
    startLocalServer,
    closeLocalServer,
    createCover,
    saveAs,
    saveClipBoardFile
}