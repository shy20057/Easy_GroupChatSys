import { createApp } from 'vue'
import App from './App.vue'

import * as Pinia from 'pinia'
// element-plus样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css' 
import '@/assets/cust-elementplus.scss'
import '@/assets/icon/iconfont.css'
import '@/assets/base.scss'
import router from '@/router'

/* utils */
import Utils from '@/utils/Utils.js'
import Verify from '@/utils/Verify.js'
import Request from '@/utils/Request.js'
import Message from '@/utils/Message.js'
import Api from '@/utils/Api.js'
import Confirm from '@/utils/Confirm.js'

/* components */
import WinOp from '@/components/WinOp.vue'
import Layout from '@/components/Layout.vue'
import ContentPanel from '@/components/ContentPanel.vue'
import ShowLocalImage from '@/components/ShowLocalImage.vue'
import UserBaseInfo from '@/components/UserBaseInfo.vue'
import Dialog from '@/components/Dialog.vue'
import Avatar from '@/components/Avatar.vue'
import AvatarUpload from './components/AvatarUpload.vue'

const app = createApp(App)


app.use(ElementPlus)
app.use(Pinia.createPinia());
app.use(router);

/* utils */
// proxy 是组件实例的代理对象 项目通过 app.config.globalProperties 向全局实例挂载了多个工具对象
// 在渲染组件中通过 “proxy.工具” 可以使用全局工具 但注意要是app.config.globalProperties注册的工具
app.config.globalProperties.Utils = Utils;
app.config.globalProperties.Verify = Verify;
app.config.globalProperties.Request = Request;
app.config.globalProperties.Message = Message;
app.config.globalProperties.Api = Api;
app.config.globalProperties.Confirm = Confirm;


/* components */
app.component('WinOp', WinOp)
app.component('Layout', Layout)
app.component('ContentPanel', ContentPanel)
app.component('ShowLocalImage', ShowLocalImage)
app.component('UserBaseInfo', UserBaseInfo)
app.component('Dialog', Dialog)
app.component('Avatar', Avatar)
app.component('AvatarUpload', AvatarUpload)

app.mount('#app')

