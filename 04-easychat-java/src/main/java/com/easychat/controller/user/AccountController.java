package com.easychat.controller.user;


import com.easychat.annotation.GlobalInterceptor;
import com.easychat.controller.ABaseController;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.LoginDTO;
import com.easychat.entity.dto.MessageSendDTO;
import com.easychat.entity.dto.RegisterDTO;
import com.easychat.entity.vo.ResponseVO;
import com.easychat.entity.vo.UserInfoVO;
import com.easychat.exception.BusinessException;
import com.easychat.redis.RedisComponent;
import com.easychat.redis.RedisUtils;
import com.easychat.service.UserService;
import com.easychat.websocket.MessageHandler;
import com.wf.captcha.ArithmeticCaptcha;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController("accountController")
@RequestMapping("/account")
@Api(tags = "用户账号接口")
@Validated // 开启数据验证
public class AccountController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
   @Resource
   private RedisUtils redisUtils;
   @Resource
   private UserService userService;
   @Resource
   private RedisComponent redisComponent;
   @Resource
   private MessageHandler messageHandler;


   /**
     * 获取图片验证码
     * @return
     */
    @ApiOperation(value = "获取图片验证码")
    @RequestMapping("/checkCode")
    public ResponseVO checkCode() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(100, 42);
        String code = captcha.text();
        String checkCodeKey = UUID.randomUUID().toString();
        logger.info("验证码：{}",code); // code是展示图片验证码的结果
        logger.info("验证码key：{}",checkCodeKey);
        String checkCodeBase64 = captcha.toBase64(); // base64编码的图片验证码

        // 将获取的验证码存储到redis中
        redisUtils.set(Constants.REDIS_KEY_CHECK_CODE+checkCodeKey, code, Constants.REDIS_TIME_1MIN * 10); // 设置验证码10分钟有效

        // 将获取的验证码打包到服务端
        Map<String,String> result = new HashMap<>();
        result.put("checkCode",checkCodeBase64); // base64编码的图片验证码
        result.put("checkCodeKey",checkCodeKey); // 验证码key
        return getSuccessResponseVO(result);
    }

    /**
     * 注册
     * @param registerDTO
     * @return
     */
    @ApiOperation(value = "注册")
    @RequestMapping("/register")
    public ResponseVO register(RegisterDTO registerDTO) {
        String checkCode = registerDTO.getCheckCode();
        String checkCodeKey = registerDTO.getCheckCodeKey();
        try{                 //比较是否和传进来的值是一样的              // 获取指定键的值（当前有效期内存在Redis中的值）
            String checkCodeRedis = (String)redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
            if(!checkCode.equalsIgnoreCase( (String)redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey))){ // get(easychat:checkcode:UUID生成值)
                throw new BusinessException("验证码错误");
            }

            userService.register(registerDTO);
            return getSuccessResponseVO(null);
        }finally {
            redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey); // 重点注册成功后删掉这个值
        }

    }


    /**
     * 登录
     * @param loginDTO
     * @return
     */
    @ApiOperation(value = "登录")
    @RequestMapping("/login")
    public ResponseVO login(LoginDTO loginDTO) {
        String checkCode = loginDTO.getCheckCode();
        String checkCodeKey = loginDTO.getCheckCodeKey();
        try{
            String checkCodeRedis = (String)redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
            if(!checkCode.equalsIgnoreCase((String)redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey))){
                throw new BusinessException("验证码错误");
            }

            UserInfoVO userInfoVO = userService.login(loginDTO);



            return getSuccessResponseVO(userInfoVO);
        }finally {
            redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
        }

    }


    @GlobalInterceptor
    @RequestMapping("/getSysSetting")
    public ResponseVO getSysSetting() {
       return getSuccessResponseVO(redisComponent.getSysSetting());

    }


    @RequestMapping("/test")
    public ResponseVO getUserInfo() {
        MessageSendDTO messageSendDTO = new MessageSendDTO();
        messageSendDTO.setMessageContent("测试消息"+System.currentTimeMillis());
        messageHandler.sendMessage(messageSendDTO);
        return getSuccessResponseVO(null);
    }

}
