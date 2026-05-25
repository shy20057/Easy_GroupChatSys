<template>
  <div>
    <Dialog
      :show="dialogConfig.show"
      :title="dialogConfig.title"
      :buttons="dialogConfig.buttons"
      width="400px"
      :showCancel="false"
      @close="dialogConfig.show = false">
      
      <el-form
        :model="formData"
        :rules="rules"
        ref="formDataRef" 
        @submit.prevent
      >
    
      <!--input输入-->
        <el-form-item label="" prop="" >
          <el-input 
          type="textarea"
          :rows="5"
          clearable
          resize="none"
          show-word-limit
          maxlength="100"
          v-model="formData.applyInfo"
          ></el-input>

        </el-form-item>
      </el-form>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from "vue"
const { proxy } = getCurrentInstance();
import { useUserInfoStore } from "@/stores/UserInfoStore.js"
const userInfoStore = useUserInfoStore();
import { useContactStateStore} from "@/stores/ContactStateStore.js"
const contactStateStore = useContactStateStore();

const dialogConfig = ref({
    show: false,
    title: "提交申请",
    buttons: [
        {
            type: "primary",
            text: "确定",
            click: (e) => {
                submitApply();
            }
        }
    ]
})

const formData = ref({}); // 初始化为空对象
const formDataRef = ref();
const rules = {
  title: [{ required: true, message: "请输入内容" }],
};

const emit = defineEmits(["reload"]);
const submitApply = async() => {
    const {contactId, contactType ,applyInfo}= formData.value;
    let result = await proxy.Request({
      url: proxy.Api.applyAdd,
      params:{
        contactId,
        applyInfo,
        contactType
      }
    })
    if(!result){
      return;
    }
    if(result.data == 0){
      proxy.Message.success('加入成功');
    }else{
      proxy.Message.success('申请成功，等待对方同意');
    }

    dialogConfig.value.show = false;
    emit("reload");

    
    if(result.data == 0){ // 意味着joinType==0 可以直接加
      
      contactStateStore.setContactReload(contactType)
    }
}

// show方法接收外部传入的数据并填充到formData中
const show = (data) => {
    dialogConfig.value.show = true;
    nextTick(() => {
      formDataRef.value.resetFields(); // 清除表单验证状态
      formData.value = Object.assign({}, data); // 将外部数据赋值给formData
      formData.value.applyInfo = '我是' + userInfoStore.getInfo().nickName; // 填充默认申请信息
    })
}

// 下面在这段代码的作用是将show方法暴露给父组件，父组件可以通过ref获取到这个方法并调用
defineExpose({
    show
})
</script>

<style lang="scss" scoped>
</style>