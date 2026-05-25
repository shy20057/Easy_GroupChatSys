import moment from "moment";


const isEmpty = (str) => {
    if(str == null || str=='' || str == undefined){
        return true;
    }
    return false;
}

const getAreaInfo = (data) => {
    if(isEmpty(data)){
        return '-';
    }
    // 替换多种分隔符号为空格
    return data.replace(/[，,-]/g, " ")
}

const formatDate = (tiemstamp) =>{
  const timestampTime = moment(tiemstamp);
  const days = Number.parseInt(moment().format("YYYYMMDD")) - Number.parseInt(timestampTime.format("YYYYMMDD"));
  if(days == 0){
    return timestampTime.format("HH:mm");
  }else if(days == 1){
    return "昨天 " + timestampTime.format("HH:mm");
  }else if(days > 1 && days < 7){
    // 将星期数字转换为中文大写
    const dayMap = {
      '0': '日',
      '1': '一',
      '2': '二',
      '3': '三',
      '4': '四',
      '5': '五',
      '6': '六'
    };
    const dayNumber = timestampTime.format("d");
    return `星期${dayMap[dayNumber]}`;
  }else if(days >= 7 ){
    return timestampTime.format("YY/MM/DD")
  }
}

const size2Str = (limit) => {
    var size = "";
    if (limit < 0.1 * 1024) { //小于0.1KB，则转化成B
        size = limit.toFixed(2) + "B"
    } else if (limit < 1024 * 1024) { //小于0.1M，则转化成KB
        size = (limit / 1024).toFixed(2) + "KB"
    } else if (limit < 1024 * 1024 * 1024) { //小于0.1GB，则转化成MB
        size = (limit / (1024 * 1024)).toFixed(2) + "MB"
    } else { //其他转化成GB
        size = (limit / (1024 * 1024 * 1024)).toFixed(2) + "GB"
    }
    var sizeStr = size + ""; //转成字符串
    var index = sizeStr.indexOf("."); //获取小数点处的索引
    var dou = sizeStr.substring(index + 1, 2); //获取小数点后两位的值
    if (dou == "00") { //判断后两位是否为00，如果是则删除00
        return sizeStr.substring(0, index) + sizeStr.substr(index + 3, 2)
    }
    return size;
}

export default{
    isEmpty,
    getAreaInfo,
    formatDate,
    size2Str
}