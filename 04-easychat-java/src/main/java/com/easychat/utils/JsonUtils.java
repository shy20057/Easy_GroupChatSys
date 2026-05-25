package com.easychat.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.easychat.entity.enums.ResponseCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.easychat.exception.BusinessException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    // 配置 JSON 序列化特性，这里设置了 WriteMapNullValue，即在序列化时会输出值为 null 的字段。
    public static SerializerFeature[] FEATURES = new SerializerFeature[]{SerializerFeature.WriteMapNullValue};

    // java对象 转 字符串
    public static String convertObj2Json(Object obj) {
        return JSON.toJSONString(obj, FEATURES); // 使用预定义的序列化特性（包含空值字段）
    }

    // json字符串 转 java对象
    public static <T> T convertJson2Obj(String json, Class<T> classz) {
        try {
            return JSONObject.parseObject(json, classz);
        } catch (Exception e) {
            logger.error("convertJson2Obj异常,json:{}", json);
            throw new BusinessException(ResponseCodeEnum.CODE_601);
        }
    }

    // json数组 转 List集合
    public static <T> List<T> convertJsonArray2List(String json, Class<T> classz) {
        try {
            return JSONArray.parseArray(json, classz);
        } catch (Exception e) {
            logger.error("convertJsonArray2List,json:{}", json, e);
            throw new BusinessException(ResponseCodeEnum.CODE_601);
        }
    }

    public static List<String> convertArray2List(List<String> contactIdList) {
        // 确保 contactIdList 不为空
        if (contactIdList != null && !contactIdList.isEmpty()) {
            String jsonString = contactIdList.get(0);

            // 处理字符串，去除不需要的字符并分割
            String cleanedString = jsonString.replaceAll("[\\[\\]\"]", "").trim();
            String[] idArray = cleanedString.split(",");

            // 转换为 List<String>
            List<String> parsedList = Arrays.stream(idArray)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            // 重新赋值给 contactIdList
            contactIdList = parsedList;
        }
        return contactIdList;
    }

}