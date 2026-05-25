package com.easychat.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;


/**
 * 靓号信息表
 */
@Data
public class UserInfoBeauty implements Serializable {


	/**
	 * 主键自增id
	 */
	private Long id;

	/**
	 * 邮箱
	 */
	private String email;

	/**
	 * 用户id
	 */
	private String userId;

	/**
	 * 0:正常，1:封禁
	 */
	private Integer status;




	@Override
	public String toString (){
		return "主键自增id:"+(id == null ? "空" : id)+"，邮箱:"+(email == null ? "空" : email)+"，用户id:"+(userId == null ? "空" : userId)+"，0:正常，1:封禁:"+(status == null ? "空" : status);
	}
}
