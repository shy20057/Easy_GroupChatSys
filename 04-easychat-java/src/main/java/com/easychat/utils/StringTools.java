package com.easychat.utils;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.enums.UserContactTypeEnum;
import com.easychat.exception.BusinessException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;


public class StringTools {

    public static void checkParam(Object param) {
        try {
            Field[] fields = param.getClass().getDeclaredFields(); // getDeclaredFields()：获取指定类上的所有字段
            boolean notEmpty = false; // 判断是否有非空的字段
            for (Field field : fields) {
                String methodName = "get" + StringTools.upperCaseFirstLetter(field.getName());  // 为每个字段构造对应的 getter 方法名
                Method method = param.getClass().getMethod(methodName); // 通过反射调用 getter 方法获取字段值
                Object object = method.invoke(param); // 获取字段值
                // 判断字段值是否为空 ①字符串类型：不为 null 且不为空字符串 ②非字符串类型：不为 null
                if (object != null && object instanceof java.lang.String && !StringTools.isEmpty(object.toString())
                        || object != null && !(object instanceof java.lang.String)) {
                    notEmpty = true;
                    break;
                }
            }
            if (!notEmpty) {
                throw new BusinessException("多参数更新，删除，必须有非空条件");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("校验参数是否为空失败");
        }
    }

    public static String upperCaseFirstLetter(String field) {
        if (isEmpty(field)) {
            return field;
        }
        //如果第二个字母是大写，第一个字母不大写
        if (field.length() > 1 && Character.isUpperCase(field.charAt(1))) {
            return field;
        }
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    public static boolean isEmpty(String str) {
        if (null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if ("".equals(str.trim())) {
            return true;
        }
        return false;
    }

    public static String getUserId(){
       // return RandomStringUtils.random(11, true, true);
        return UserContactTypeEnum.USER.getPrefix() + getRandomNumber(Constants.LENGTH_11);
    }

    public static String getGroupId(){
       // return RandomStringUtils.random(11, true, true);
        return UserContactTypeEnum.GROUP.getPrefix() + getRandomNumber(Constants.LENGTH_11);
    }


    public static  String getRandomNumber(Integer count){
        return RandomStringUtils.random(count,false,true);
    }

    public static  String getRandomString(Integer count){
        return RandomStringUtils.random(count,true,true);
    }

    /*MD5加密工具*/
    public static final String encodeMd5(String originString){
        return StringTools.isEmpty(originString)?null: DigestUtils.md5Hex(originString);
    }

    public static String cleanHtmlTag(String content){
        if(isEmpty(content)){
            return content;
        }
        content = content.replace("<", "&lt;");
        content = content.replace("\r\n", "<br/>");
        content = content.replace("\n", "<br/>");
        content = content.replace(">", "&gt;");
        return content;

    }

    public static final String getChatSessionId4User(String[] userIds) {
        Arrays.sort(userIds);
        return encodeMd5(StringUtils.join(userIds, ""));
    }

    public static final String getChatSessionId4Group(String groupId){
        return encodeMd5(groupId);
    }

    public static String getFileSuffix(String fileName){

        if(isEmpty(fileName)){
            return null;
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    public static boolean isNumber(String str){
        String checkNumber = "^[0-9]+$";
        if(null == str){
            return false;
        }
        if(!str.matches(checkNumber)){
            return false;
        }
        return true;
    }
}
