package com.easychat.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 全局拦截器
@Target(ElementType.METHOD) // 注解作用在方法上
@Retention(RetentionPolicy.RUNTIME) // 注解保留在运行时
public @interface GlobalInterceptor {

    // 校验登录
    boolean checkLogin() default true;
    // 校验管理员
    boolean checkAdmin() default false;
}
