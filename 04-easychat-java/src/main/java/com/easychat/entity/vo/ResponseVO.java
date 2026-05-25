package com.easychat.entity.vo;


public class ResponseVO<T> {
    private String status; // success, error
    private Integer code; // 200, 500, 600, 700, 800, 900
    private String info; // 错误信息
    private T data; // 数据

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
