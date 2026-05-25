import { defineStore } from "pinia";
export const useContactStateStore = defineStore("ContactStateInfo", {

    state: () => {
        return{
            contactReload:null, // 用于控制联系人列表的重新加载，初始值为null
        }
    },
    actions: {
       setContactReload(state){ // 接收一个state参数，用于更新contactReload状态
        this.contactReload = state 
       }
    }
})