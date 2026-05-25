package com.easychat.entity.po;

import com.easychat.utils.StringTools;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.easychat.entity.enums.DateTimePatternEnum;
import com.easychat.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * app发布
 */
@Data
public class AppUpdate implements Serializable {


	/**
	 * 自增ID
	 */
	private Integer id;

	/**
	 * 版本号
	 */
	private String version;

	/**
	 * 更新描述
	 */
	private String updateDesc;
	private String[] updateDescArray;

	public String[] getUpdateDescArray() {
		if(!StringTools.isEmpty(updateDesc)){
			return updateDesc.split("\\|");
		}
		return updateDescArray;
	}
	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 0:未发布1:灰度发布2:全网发布
	 */
	private Integer status;

	/**
	 * 灰度uid
	 */
	private String grayscaleUid;

	/**
	 * 文件类型0:本地文件1:外链
	 */
	private Integer fileType;

	/**
	 * 外链地址
	 */
	private String outerLink;


}
