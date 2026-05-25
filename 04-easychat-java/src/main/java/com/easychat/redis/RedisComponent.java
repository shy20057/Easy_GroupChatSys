package com.easychat.redis;


import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.SysSettingDTO;
import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.utils.StringTools;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;


@Component("redisComponent")
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;


    // 只封装了存储token的逻辑
    public void saveTokenUserInfoDTO(TokenUserInfoDTO tokenUserInfoDTO){

        redisUtils.set(Constants.REDIS_KEY_WS_TOKEN + tokenUserInfoDTO.getToken(),
                            tokenUserInfoDTO, // 这里就是要存的数据 存了一个
                            Constants.REDIS_KEY_EXPIRES_DAY*2); // 三个参数，key，value，过期时间


        redisUtils.set(Constants.REDIS_KEY_WS_TOKEN_USERID + tokenUserInfoDTO.getUserId(),
                            tokenUserInfoDTO.getToken(), // token
                            Constants.REDIS_KEY_EXPIRES_DAY*2); // 这里存储两个token,dto用于业务，后面用来状态管理，（强制下线，心跳检测等）
    }

    // 获取token对应的用户信息
    public TokenUserInfoDTO getTokenUserInfoDTO(String token){

        return (TokenUserInfoDTO) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN + token);
    }

    public TokenUserInfoDTO getTokenUserInfoDTOByUserId(String userId){
        String token = (String) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN_USERID + userId);
        return getTokenUserInfoDTO(token);
    }


    public void cleanUserTokenByUserId(String userId){
        String token = (String) redisUtils.get(Constants.REDIS_KEY_WS_TOKEN_USERID + userId);
        if(StringTools.isEmpty(token)){
            return;
        }
        redisUtils.delete(Constants.REDIS_KEY_WS_TOKEN_USERID + token);
    }

    // 获取系统设置
    public SysSettingDTO getSysSetting(){
        SysSettingDTO sysSettingDTO = (SysSettingDTO) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        sysSettingDTO = sysSettingDTO == null ? new SysSettingDTO() : sysSettingDTO;
        return sysSettingDTO;

    }

    // 保存系统设置
    public void saveSysSetting(SysSettingDTO sysSettingDTO){
        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING,
                       sysSettingDTO,
                       Constants.REDIS_KEY_EXPIRES_DAY*365); // 按理说不应该设置时间
    }

    /**
     * 获取用户心跳
     * @param userId
     * @return
     */
    public Long getUserHeartBeat(String userId){
        return (Long) redisUtils.get(Constants.REDIS_KEY_WS_USER_HEART_BEAT + userId);
    }

    /**
     * 保存用户心跳
     * @param userId
     */
    public void saveHeartBeat(String userId) {
        redisUtils.set(Constants.REDIS_KEY_WS_USER_HEART_BEAT + userId,
                           System.currentTimeMillis(), // 保存的数据 就是用户心跳的时间
                           Constants.REDIS_KEY_EXPIRES_BEAT);
    }

    /**
     * 删除用户心跳
     * @param userId
     */
    public void removeUserHeartBeat(String userId){
        redisUtils.delete(Constants.REDIS_KEY_WS_USER_HEART_BEAT + userId);
    }


    // 清空联系人
    public void cleanUserContact(String userId) {
        redisUtils.delete(Constants.REDIS_KEY_USER_CONTACT + userId);
    }
    // 批量添加联系人
    public void addUserContactBatch(String userId, List<String> contactIdList) {
        redisUtils.lpushAll(Constants.REDIS_KEY_USER_CONTACT + userId, contactIdList, Constants.REDIS_KEY_TOKEN_EXPIRES);
    }

    // 单独添加联系人 往redis中添加联系人
    public void addUserContact(String userId, String contactId){
        List<String> contactIdList = getContactList(userId);
        if(contactIdList.contains(contactId)){
            return;
        }
        redisUtils.lpush(Constants.REDIS_KEY_USER_CONTACT + userId, contactId, Constants.REDIS_KEY_TOKEN_EXPIRES);
    }

    /**
     * 获取联系人列表
     * @param userId
     * @return
     */
    public List<String> getContactList(String userId) {
//        String key = Constants.REDIS_KEY_USER_CONTACT + userId;
//        List<String> contactList =(List<String>) redisUtils.getList(key);
        return (List<String>) redisUtils.getList(Constants.REDIS_KEY_USER_CONTACT + userId);
    }

    /**
     * 删除联系人
     * @param userId
     * @param contactId
     * */
     public void removeUserContact(String userId, String contactId){
       redisUtils.remove(Constants.REDIS_KEY_USER_CONTACT + userId, contactId);
    }



}
