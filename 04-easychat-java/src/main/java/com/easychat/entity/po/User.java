package com.easychat.entity.po;

import java.util.Date;

import com.easychat.entity.constants.Constants;
import com.easychat.entity.enums.DateTimePatternEnum;
import com.easychat.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * 用户信息表
 */
@Data
public class User implements Serializable {


	/**
	 * 用户id
	 */
	private String userId;

	/**
	 * 昵称
	 */
	private String nickName;

	/**
	 * 邮箱
	 */
	private String email;

	/**
	 * 密码
	 */
	private String password;

	/**
	 * 0:直接加入，1:同意后加入
	 */
	private Integer joinType;

	/**
	 * 性别 0:女，1:男
	 */
	private Integer sex;

	/**
	 * 个性签名
	 */
	private String personalSignature;

	/**
	 * 0:禁用，1:启用
	 */
	private Integer status;

	/**
	 * 头像
	 */
	private String avatar;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 最后登录时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date lastLoginTime;

	/**
	 * 地区名称
	 */
	private String areaName;

	/**
	 * 地区编码
	 */
	private String areaCode;

	/**
	 * 最后下线时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Long lastOffTime;

	private Integer onlineType;

   public Integer getOnlineType(){
	   if(lastLoginTime!=null&&lastLoginTime.getTime()>lastOffTime){
		  return Constants.ONE;
	   }else{
		   return Constants.ZERO;
	   }
   }


	@Override
	public String toString (){
		return "用户id:"+(userId == null ? "空" : userId)+"，昵称:"+(nickName == null ? "空" : nickName)+"，邮箱:"+(email == null ? "空" : email)+"，密码:"+(password == null ? "空" : password)+"，0:直接加入，1:同意后加入:"+(joinType == null ? "空" : joinType)+"，性别 0:女，1:男:"+(sex == null ? "空" : sex)+"，个性签名:"+(personalSignature == null ? "空" : personalSignature)+"，0:正常，1:封禁:"+(status == null ? "空" : status)+"，头像:"+(avatar == null ? "空" : avatar)+"，创建时间:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，最后登录时间:"+(lastLoginTime == null ? "空" : DateUtil.format(lastLoginTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，地区名称:"+(areaName == null ? "空" : areaName)+"，地区编码:"+(areaCode == null ? "空" : areaCode)+"，最后下线时间:"+(lastOffTime == null ? "空" : lastOffTime);
	}
}
