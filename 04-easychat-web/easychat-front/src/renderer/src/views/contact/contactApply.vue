<template>
    <ContentPanel :showTopBorder="true" :infinite-scroll-immediate="false" v-infinite-scroll="loadApply">
        <div>
            <div class="apply-item" v-for="item in applyList">
                <div :class="['contact-type', item.contactType == 0 ? 'user-contact' : '']">
                    {{ item.contactType == 0 ? '好友' : '群聊' }}
                </div>
                <Avatar :width="50" :userId="item.applyUserId"></Avatar>

                <div class="contact-info">
                    <div class="nick-name">{{ item.contactName }}</div>
                    <div class="apply-info">{{ item.applyInfo }}</div>
                </div>

                <div class="op-btn">
                    <div v-if="item.status == 0" class="btn-group">
                        <el-button 
                            type="primary" 
                            size="small"
                            @click="handleAccept(item)"
                            class="accept-btn">
                            同意
                        </el-button>
                        <el-dropdown placement="bottom-end" trigger="click">
                            <el-button 
                                type="info" 
                                size="small"
                                plain
                                class="more-btn">
                                更多 ▼
                            </el-button>
                            <template #dropdown>
                                <el-dropdown-item
                                    @click="dealWithApply(item.applyId, item.contactType, 2)">
                                    拒绝
                                </el-dropdown-item>
                                <el-dropdown-item
                                    @click="dealWithApply(item.applyId, item.contactType, 3)">
                                    拉黑
                                </el-dropdown-item>
                            </template>
                        </el-dropdown>
                    </div>

                    <div v-else class="result-name">{{ item.statusName }}</div>
                </div>
            </div>
        </div>

        <div v-if="applyList.length == 0" class="no-data">暂无申请</div>
    </ContentPanel>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick } from "vue"
const { proxy } = getCurrentInstance();
import { useRoute, useRouter } from 'vue-router'
const route = useRoute()
const router = useRouter()

import {useContactStateStore} from "@/stores/contactStateStore.js"
const contactStateStore = useContactStateStore();

const applyList = ref([])

let pageNo = 0;
let pageTotal = 1;
const loadApply = async () => {

    pageNo++;
    if (pageNo > pageTotal) {
        return;
    }
    let result = await proxy.Request({
        url: proxy.Api.loadApply,
        params: {}
    })
    if (!result) {
        return;
    }

    pageTotal = result.data.pageTotal;
    if (result.data.pageNo == 1) {
        applyList.value = [];
    }

    applyList.value = applyList.value.concat(result.data.list);
    pageNo = result.data.pageNo;
}

loadApply()

const dealWithApply = async (applyId, contactType, status) => {
    contactStateStore.setContactReload(null)
    proxy.Confirm({
        message: status == 1 ? "确定要接受申请吗？" : status == 2 ? "确定要拒绝申请吗？" : "确定要拉黑申请用户吗？",
        okfun: async () => {
            let result = await proxy.Request({
                url: proxy.Api.dealWithApply,
                params: {
                    applyId,
                    status
                }
            })
            if (!result) {
                return;
            }
            pageNo = 0;
            loadApply()
            if(contactType == 0 && status == 1){
                contactStateStore.setContactReload("USER")
            }else if(contactType == 1 && status == 1){
                contactStateStore.setContactReload("GROUP")
            }

        }
    })
}

// 快捷同意处理
const handleAccept = (item) => {
    dealWithApply(item.applyId, item.contactType, 1)
}

// TODO 监听新朋友数量改变
</script>

<style lang="scss" scoped>
.apply-item {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #ddd;
    padding: 12px 10px;

    .contact-type {
        display: flex;
        justify-content: center;
        writing-mode: vertical-rl;
        vertical-align: middle;
        background: #2cb6fe;
        color: #fff;
        border-radius: 5px 0px 0px 5px;
        height: 50px;
        font-size: 13px;
        font-weight: 500;
    }

    .user-contact {
        background: #08bf61;
    }

    .contact-info {
        flex: 1;
        margin-left: 12px;
        min-width: 0;

        .nick-name {
            color: #000000;
            font-size: 15px;
            font-weight: 500;
        }

        .apply-info {
            color: #999999;
            font-size: 12px;
            margin-top: 4px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
    }

    .op-btn {
        margin-left: 10px;
        display: flex;
        align-items: center;

        .btn-group {
            display: flex;
            gap: 8px;
            align-items: center;
        }

        .accept-btn {
            background: #07c160 !important;
            border-color: #07c160 !important;
            color: white !important;
            font-weight: 500;
            padding: 6px 16px;
            border-radius: 4px;
            transition: all 0.3s ease;

            &:hover {
                background: #06ae56 !important;
                border-color: #06ae56 !important;
                transform: translateY(-1px);
                box-shadow: 0 2px 6px rgba(7, 193, 96, 0.3);
            }
        }

        .more-btn {
            padding: 6px 10px;
            border-radius: 4px;
            font-size: 12px;
            color: #666 !important;

            &:hover {
                color: #409eff !important;
                border-color: #409eff !important;
            }
        }

        .result-name {
            color: #999999;
            font-size: 13px;
            padding: 4px 8px;
            background: #f5f5f5;
            border-radius: 4px;
        }
    }
}
</style>
