package com.easychat.controller;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.entity.enums.ResponseCodeEnum;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.exception.BusinessException;
import com.easychat.redis.RedisUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


public class ABaseController {

    protected static final String STATUC_SUCCESS = "success";

    protected static final String STATUC_ERROR = "error";

    @Resource
    private RedisUtils redisUtils;

    // 获取成功响应
    protected <T> ResponseVO getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>(); // 创建响应对象
        responseVO.setStatus(STATUC_SUCCESS); // 成功
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode()); // 响应的状态码
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg()); // 响应的信息成功
        responseVO.setData(t); // 响应的数据
        return responseVO;
    }

    protected <T> ResponseVO getBusinessErrorResponseVO(BusinessException e, T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUC_ERROR);
        if (e.getCode() == null) {
            vo.setCode(ResponseCodeEnum.CODE_600.getCode()); // 参数错误
        } else {
            vo.setCode(e.getCode()); // 业务错误的状态码
        }
        vo.setInfo(e.getMessage()); // 错误信息
        vo.setData(t); // 响应的数据
        return vo;
    }

    protected <T> ResponseVO getServerErrorResponseVO(T t) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus(STATUC_ERROR);
        vo.setCode(ResponseCodeEnum.CODE_500.getCode()); // 服务器错误的状态码
        vo.setInfo(ResponseCodeEnum.CODE_500.getMsg()); // 错误信息
        vo.setData(t); // 响应的数据
        return vo;
    }

    // 定义一个公共方法，所有的controller都会使用
    protected TokenUserInfoDTO getTokenUserInfoDTO(HttpServletRequest request) {

        String token = request.getHeader("token");
        TokenUserInfoDTO tokenUserInfoDTO = (TokenUserInfoDTO) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN + token); // !!!我当时存的就是一个对象，所以这里直接返回
        return tokenUserInfoDTO;
    }
}
