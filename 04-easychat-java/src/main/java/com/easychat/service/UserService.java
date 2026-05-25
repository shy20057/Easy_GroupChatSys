package com.easychat.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.easychat.entity.dto.LoginDTO;
import com.easychat.entity.dto.RegisterDTO;
import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.entity.query.UserQuery;
import com.easychat.entity.po.User;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.vo.UserInfoVO;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotEmpty;


/**
 * 用户信息表 业务接口
 */
public interface UserService {

	/**
	 * 根据条件查询列表
	 */
	List<User> findListByParam(UserQuery param);

	/**
	 * 根据条件查询数量
	 */
	Integer findCountByParam(UserQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<User> findListByPage(UserQuery param);

	/**
	 * 新增
	 */
	Integer add(User bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<User> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<User> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(User bean, UserQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(UserQuery param);

	/**
	 * 根据UserId查询对象 （单条件 主键+索引）
	 */
	User getUserByUserId(String userId);


	/**
	 * 根据UserId修改
	 */
	Integer updateUserByUserId(User bean, String userId);


	/**
	 * 根据UserId删除
	 */
	Integer deleteUserByUserId(String userId);


	/**
	 * 根据Email查询对象
	 */
	User getUserByEmail(String email);


	/**
	 * 根据Email修改
	 */
	Integer updateUserByEmail(User bean, String email);


	/**
	 * 根据Email删除
	 */
	Integer deleteUserByEmail(String email);

	/**
	 * 注册
	 */
	void register(RegisterDTO registerDTO);

	/**
	 * 登录
	 */
	UserInfoVO login(LoginDTO loginDTO);

	/**
	 * 修改用户信息
	 */
    void updateUserInfo(User user, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException;

	void updateUserStatus(String userId, Integer status);

	void forceOffLine(@NotEmpty String userId);
}