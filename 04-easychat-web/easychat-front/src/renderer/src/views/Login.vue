<template>
  <div class="login-panel">
     <div class="title drag">EasyChat</div>
     <div v-if="showLoading" class="loading-panel">
        <img src="../assets/img/loading.gif" />
     </div>
     <div class="login-form">
        <div class="error-msg">{{ errorMsg }}</div>
       
        <el-form
          :model="formData"  
          :rules="rules"
          ref="formDataRef"
          label-width="0px"
          @submit.prevent
        >
        <!--input输入-->
         <el-form-item prop="email">
          <el-input @focus="cleanVerify" maxLength="30" size="large" clearable placeholder="请输入邮箱"
            v-model.trim="formData.email">
            <template #prefix>
              <span class="iconfont icon-email"></span>
            </template>
          </el-input>
        </el-form-item>

          <el-form-item  prop="nickName" v-if="!isLogin">
            <el-input @focus="cleanVerify" maxLength="15" size="large" clearable placeholder="请输入昵称" v-model.trim="formData.nickName">
                <template #prefix>
                    <span class="iconfont icon-user"></span>
                </template>
            </el-input>
          </el-form-item>

          <el-form-item  prop="password" >
            <el-input @focus="cleanVerify" show-password size="large" clearable placeholder="请输入密码" v-model.trim="formData.password">
                <template #prefix>
                    <span class="iconfont icon-password"></span>
                </template>
            </el-input>
          </el-form-item>

          <el-form-item  prop="rePassword" v-if="!isLogin" >
            <el-input @focus="cleanVerify" show-password size="large" clearable placeholder="请重新输入密码" v-model.trim="formData.rePassword">
                <template #prefix>
                    <span class="iconfont icon-password"></span>
                </template>
            </el-input>
          </el-form-item>

          <el-form-item  prop="checkCode">
           <div class="check-code-panel">
              <el-input @focus="cleanVerify" size="large" clearable placeholder="请输入验证码" v-model.trim="formData.checkCode">
                  <template #prefix>
                      <span class="iconfont icon-checkcode"></span>
                  </template>
              </el-input>
  
              <img :src="checkCodeUrl" class="check-code" @click="changeCheckCode">
           </div>
          </el-form-item>

          <el-form-item>
           <el-button type="primary"class="login-btn" @click="submit">{{ isLogin? '登录':'注册'}}</el-button>
          </el-form-item>

          <div class="bottom-link">
             <span class="a-link" @click="changeOpType">{{ isLogin? '没有账号？':'已有账号？'}}</span>
          </div>
        </el-form>
     </div>
     
  </div>
<!-- WinOp 组件放在 login-panel 外部 -->
<WinOp :showSetTop="false" :showMin="false" :showMax="false" :closeType="0"></WinOp>
</template>


<script setup>
    import md5 from 'js-md5';
    import { useUserInfoStore } from '@/stores/UserInfoStore';
    const userInfoStore = useUserInfoStore();
    import {ref,reactive,getCurrentInstance,nextTick,onMounted} from 'vue'
    import { useRouter } from 'vue-router';
    import WinOp from '../components/WinOp.vue';
    const router = useRouter();
    const {proxy} = getCurrentInstance()
    
  
    const formData = ref({});
    // formDataRef(模板引用) 提供了一种直接访问和操作 el-form 组件的方法，使得我们可以调用组件提供的各种 API，如验证、重置等
    const formDataRef = ref();
    const rules = {
      email: [{ required: true, message: "请输入邮箱" }],
      password: [{ required: true, message: "请输入密码" }],
      checkCode: [{ required: true, message: "请输入验证码" }],
      nickName: [{ required: true, message: "请输入昵称" }],
      rePassword: [{ required: true, message: "请再次输入密码" }],

    };

    const isLogin = ref(true);

    const changeOpType = () => {
        window.ipcRenderer.send('loginOrRegister', !isLogin.value);
        isLogin.value = !isLogin.value;
        // 清空表单数据
        formData.value = {};
        // 清除表单验证状态
        nextTick(() => {
          formDataRef.value.clearValidate();
          formDataRef.value.resetFields();
          cleanVerify();
        });
        changeCheckCode();
    }


    // 获取验证码
    const checkCodeUrl = ref(null);
    const changeCheckCode = async() => {
       let result = await proxy.Request({
         url: proxy.Api.checkCode,
       })
       if(!result){
          return;
       }
       checkCodeUrl.value = result.data.checkCode;
       localStorage.setItem("checkCodeKey",result.data.checkCodeKey)
    }
     changeCheckCode();

     // 验证信息的规则
     const checkValue = (type,value,msg) => {
        // 判空校验
        if(proxy.Utils.isEmpty(value)){
            errorMsg.value =msg;
            return false;
        }
        // 正则校验
        if(type && !proxy.Verify[type](value)){
          errorMsg.value =msg;
          return false;
        }

        return true;
     }

     const cleanVerify = () => {
      errorMsg.value = null;
     }


     const errorMsg = ref(null);

     const showLoading = ref(false);
    const submit = async () => {
      
        cleanVerify();

        // 等待表单验证完成
        const valid = await new Promise((resolve) => {
            formDataRef.value.validate((valid) => {
                resolve(valid);
            });
        });

        if (!valid) {
            return;
        }

        if(!checkValue("checkEmail",formData.value.email,"请输入正确的邮箱")){
         
            return false;
        }
        if(!isLogin.value && !checkValue(null,formData.value.nickName,"请输入正确的昵称")){
         
            return false;
        }
        if(!checkValue("checkPassword",formData.value.password,"密码只能是数字，字母，特殊字符8-18位")){
         
            return false;
        }
        if(!isLogin.value && !checkValue(null,formData.value.rePassword,"两次输入的密码不一致")){
      
          return false;
        }
        if(!checkValue(null,formData.value.checkCode,"请输入正确的验证码")){
           
            return false;
        }
        
        if(isLogin.value){
          showLoading.value = true;
        }

        // 校验成功后，发送后端请求
        let result = await proxy.Request({
          
           url: isLogin.value ? proxy.Api.login : proxy.Api.register,
           showError: false,
           showLoading: isLogin.value?false:true,
           params:{
            email: formData.value.email,
            // password: isLogin.value ? md5(formData.value.password) : formData.value.password,
            password: formData.value.password,
            checkCode: formData.value.checkCode,
            nickName: formData.value.nickName,
            checkCodeKey: localStorage.getItem("checkCodeKey")
           },
           errorCallback: (response) => {
            showLoading.value = false;
            changeCheckCode();
            errorMsg.value = response.info
           
           }
        })
        
        if(!result){
          return;
        }
        
        if(isLogin.value){
          userInfoStore.setInfo(result.data)
          localStorage.setItem("token", result.data.token)
          // 页面跳转如此简单
          router.push('/main')

          const screenWidth = window.screen.width
          const screenHeight = window.screen.height
          
          
          // 发送主进程 
          window.ipcRenderer.send('openChat', {
            email: formData.value.email,
            token: result.data.token,
            userId: result.data.userId,
            nickName: result.data.nickName,
            admin: result.data.admin,
            screenWidth: screenWidth,
            screenHeight: screenHeight
          })

          window.ipcRenderer.send('setLocalStore', {key: "devWsDomain",value: proxy.Api.devWsDomain })
          window.ipcRenderer.send('getLocalStore', "devWsDomain")

        }else{
          proxy.Message.success("注册成功");
          changeOpType()
        }
     }

     const init = () => {
         window.ipcRenderer.send('setLocalStore', {key: "prodDomain",value: proxy.Api.prodDomain})
         window.ipcRenderer.send('setLocalStore', {key: "devDomain",value: proxy.Api.devDomain})
         window.ipcRenderer.send('setLocalStore', {key: "prodWsDomain",value: proxy.Api.prodWsDomain})
         window.ipcRenderer.send('setLocalStore', {key: "devWsDomain",value: proxy.Api.devWsDomain})
     }

     onMounted(() => {
       init();
     })


    
</script>

<style lang="scss" scoped>
.email-select {
  width: 250px;
}

.loading-panel {
  height: calc(100vh - 32px);
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  img {
    width: 300px;
  }
}

.login-panel {
  background: #fff;
  border-radius: 3px;
  border: 1px solid #ddd;
  .title {
    height: 30px;
    padding: 5px 0px 0px 10px;
  }
  .login-form {
    padding: 0px 15px 29px 15px;
    :deep(.el-input__wrapper) {
      box-shadow: none;
      border-radius: none;
    }
    .el-form-item {
      border-bottom: 1px solid #ddd;
    }
    .email-panel {
      align-items: center;
      width: 100%;
      display: flex;
      .input {
        flex: 1;
      }
      .icon-down {
        margin-left: 3px;
        width: 16px;
        cursor: pointer;
        border: none;
      }
    }
    .error-msg {
      line-height: 30px;
      height: 30px;
      color: #fb7373;
    }
    .check-code-panel {
      display: flex;
      .check-code {
        cursor: pointer;
        width: 120px;
        margin-left: 5px;
      }
    }
    .login-btn {
      margin-top: 20px;
      width: 100%;
      background: #07c160;
      height: 36px;
      font-size: 16px;
    }
    .bottom-link {
      text-align: right;
    }
  }
}

/* 添加新的样式 */
.win-op-container {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 1000; /* 确保它在其他元素之上 */
}
</style>