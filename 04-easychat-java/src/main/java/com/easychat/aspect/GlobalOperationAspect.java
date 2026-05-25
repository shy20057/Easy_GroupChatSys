package com.easychat.aspect;

import com.easychat.annotation.GlobalInterceptor;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.entity.enums.ResponseCodeEnum;
import com.easychat.exception.BusinessException;
import com.easychat.redis.RedisUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Aspect
@Component("globalOperationAspect")
public class GlobalOperationAspect {

    @Resource
    private RedisUtils redisUtils;

    private static final Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);

    @Before("@annotation(com.easychat.annotation.GlobalInterceptor)")
    public void interceptDo(JoinPoint joinPoint) { //JoinPoint joinPoint: 连接点对象，提供了对连接点的信息访问
        try{
            Method method = ((MethodSignature)joinPoint.getSignature()).getMethod(); // 获取连接点方法
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class); // 获取方法上的注解
            if(interceptor == null){ // 如果方法上没有注解，则不进行拦截
                return;
            }
            // 这里要检查checkAdmin 就一定要先检查登录 ！！！！重点逻辑
            if(interceptor.checkLogin() || interceptor.checkAdmin()){ // 如果需要登录验证 或者管理员验证
                checkLogin(interceptor.checkAdmin());
            }
        }catch (BusinessException e){
            logger.error("全局拦截异常",e);
            throw e;
        }
        catch (Exception e) {
            logger.error("全局拦截异常",e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }


    }

    private void checkLogin(Boolean checkAdmin){ // 检查是否登录
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        /*
        *  通过 RequestContextHolder 获取 当前线程绑定 的请求属性 ---> .getRequestAttributes()
        *  ServletRequestAttributes 并获取 HttpServletRequest 对象
        *  HttpServletRequest 代表客户端发送到服务端的HTTP请求  包含请求参数、请求头、Cookie、请求体等信息 token在请求头header中
         * */
        String token = request.getHeader("token");
        TokenUserInfoDTO tokenUserInfoDTO = (TokenUserInfoDTO) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN + token);
        if(tokenUserInfoDTO == null){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        // 检查是否是管理员
        if(checkAdmin && !tokenUserInfoDTO.getAdmin()){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }

    }
}
