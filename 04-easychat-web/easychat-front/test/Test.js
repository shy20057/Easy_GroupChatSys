const { exec } = require('child_process');

const execCommand = (command) => {
    return new Promise((resolve, reject) => { 
        exec(command, (error, stdout, stderr) => { 
            console.log("ffmpeg命令:",command)
            if (error) { 
                console.error("执行命令失败:",error)
            }
            resolve(stdout) // 输出结果
})
    })
}

const test = async () => {
    const ffprobePath = "E:/NanJiStar_Database/04-easychat-web/easychat-front/assets/ffprobe.exe"
    const ffmpegPath = "E:/NanJiStar_Database/04-easychat-web/easychat-front/assets/ffmpeg.exe"
    const filePath = "E:/Download_loading/test.mp4"
    const savePath = "E:/Download_loading/testConverted.mp4"  // 修正savePath

    let command = `${ffprobePath} -v error -select_streams v:0 -show_entries stream=codec_name "${filePath}"` 
    let result = await execCommand(command)
    result = result.replaceAll("\r\n",'')
    result = result.substring(result.indexOf('=')+1)
    let codec = result.substring(0,result.indexOf('['))
    console.log("视频编码:",codec)
    if(codec == "hevc"){
        command = `${ffmpegPath} -y -i "${filePath}" -c:v libx264 -crf 20 "${savePath}"`;
        await execCommand(command);
    }
    // 生成缩略图
    const coverPath = filePath + "_cover.png"; 
    command = `${ffmpegPath} -i "${filePath}" -y -vframes 1 -vf "scale=min(170\\,iw*min(170/iw\\,170/ih)):min(170\\,ih*min(170/iw\\,170/ih))" "${coverPath}"` 
    await execCommand(command);
}


test();