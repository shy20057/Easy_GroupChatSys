package com.easychat.entity.query;

import lombok.Data;

@Data
public class BaseParam {
	private SimplePage simplePage; //分页参数
	private Integer pageNo; //当前页码
	private Integer pageSize; // 每页显示的记录数
	private String orderBy; // 排序字段



}
