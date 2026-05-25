import axios from 'axios'
import { ElLoading } from 'element-plus'
import Message from '../utils/Message'
import Api from '../utils/Api'

// 定义请求头Content-Type
const contentTypeForm = 'application/x-www-form-urlencoded;charset=UTF-8'
const contentTypeJson = 'application/json'
const responseTypeJson = 'json'
let loading = null;

// 创建axios实例
const instance = axios.create({
  withCredentials: true, // 允许携带头部信息 token
  baseURL: (import.meta.env.PROD ? Api.prodDomain : "") + "/api", // 接口地址 -- 判断是开发阶段还是发布阶段
  timeout: 10 * 1000, // 请求超时时间
});

// 发送请求拦截器 
instance.interceptors.request.use(
  (config) => {
    if (config.showLoading) {
        // 显示加载动画 防止重复提交的手段 遮罩 ElLoading
      loading = ElLoading.service({
        lock: true,
        text: '加载中.....',
        background: 'rgba(0, 0, 0, 0.7)',
      });
    }
    return config;
  },
  (error) => {
    if (config.showLoading && loading) {
      loading.close();
    }
    Message.error("请求发送失败");
    return Promise.reject("请求发送失败");
  }
);

// 请求后响应拦截器
instance.interceptors.response.use(
  (response) => {
    const { showLoading, errorCallback, showError = true, responseType } = response.config;
    if (showLoading && loading) {
      loading.close() // 关闭加载遮罩
    }
    const responseData = response.data; // 获取响应数据
    if (responseType == "arraybuffer" || responseType == "blob") { // 返回响应数据的类型是流
      return responseData;
    }

    // 正常请求
    if (responseData.code == 200) {
      return responseData;
    } else if (responseData.code == 901) {
      // 登录超时
      setTimeout(() => {
        window.ipcRenderer.send('reLogin')
      }, 2000);
      return Promise.reject({ showError: true, msg: "登录超时" });
    } else {
      // 其他错误
      if (errorCallback) {
        errorCallback(responseData);
      }
      return Promise.reject({ showError: showError, msg: responseData.info }); // 错误信息弹窗显示 errorCallback触发机制 响应错误非200
    }
  },
  (error) => {
    if (error.config.showLoading && loading) {
      loading.close();
    }
    return Promise.reject({ showError: true, msg: "网络异常" })
  }
);

// 真正发请求的位置
const request = (config) => {
  const { url, params, dataType, showLoading = true, responseType = responseTypeJson, showError } = config;
  let contentType = contentTypeForm;
  let formData = new FormData();// 创建form对象
  for (let key in params) {
    formData.append(key, params[key] == undefined ? "" : params[key]);
  }
  if (dataType != null && dataType == 'json') {
    contentType = contentTypeJson;
  }
  const token = localStorage.getItem('token')
  let headers = {
    'Content-Type': contentType,
    'X-Requested-With': 'XMLHttpRequest',
    "token": token
  }
  return instance.post(url, formData, {
    headers: headers,
    showLoading: showLoading,
    errorCallback: config.errorCallback,
    showError: showError,
    responseType: responseType
  }).catch(error => {
    if (error.showError) {
      Message.error(error.msg);
    }
    return null;
  });
};

export default request;