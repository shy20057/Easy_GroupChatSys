package com.easychat.entity.enums;

import com.easychat.utils.StringTools;

public enum UserContactApplyStatusEnum {
    INIT(0,"待处理"),
    PASS(1,"已同意"),
    REJECT(2,"已拒绝"),
    BLACKLIST(3,"已拉黑");

    private Integer status;
    private String desc;

    UserContactApplyStatusEnum(Integer status, String desc){
        this.status = status;
        this.desc = desc;
    }

    public static UserContactApplyStatusEnum getByName(String name){
        try{
            if(StringTools.isEmpty( name)){
                return null;
            }

            return UserContactApplyStatusEnum.valueOf(name.toUpperCase());
        }catch (Exception e){
            return null;
        }
    }

    public static UserContactApplyStatusEnum getByStatus(Integer status){
        for(UserContactApplyStatusEnum userContactApplyStatusEnum : UserContactApplyStatusEnum.values()){
            if(userContactApplyStatusEnum.getStatus().equals(status)){
                return userContactApplyStatusEnum;
            }
        }
        return null;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
