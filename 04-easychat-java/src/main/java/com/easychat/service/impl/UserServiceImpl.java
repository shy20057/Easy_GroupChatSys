package com.easychat.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.easychat.entity.config.Appconfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.dto.LoginDTO;
import com.easychat.entity.dto.MessageSendDTO;
import com.easychat.entity.dto.RegisterDTO;
import com.easychat.entity.dto.TokenUserInfoDTO;
import com.easychat.entity.enums.*;
import com.easychat.entity.po.UserContact;
import com.easychat.entity.po.UserInfoBeauty;
import com.easychat.entity.query.UserContactQuery;
import com.easychat.entity.vo.UserInfoVO;
import com.easychat.exception.BusinessException;
import com.easychat.mappers.UserContactMapper;
import com.easychat.mappers.UserInfoBeautyMapper;
import com.easychat.redis.RedisComponent;
import com.easychat.service.ChatSessionUserService;
import com.easychat.service.UserContactService;
import com.easychat.utils.CopyTools;
import com.easychat.websocket.MessageHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easychat.entity.query.UserQuery;
import com.easychat.entity.po.User;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.query.SimplePage;
import com.easychat.mappers.UserMapper;
import com.easychat.service.UserService;
import com.easychat.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


/**
 * 用户信息表 业务接口实现
 */
@Service("userService")
public class UserServiceImpl implements UserService {

	@Resource
	private UserMapper<User, UserQuery> userMapper;
    @Resource
	private UserInfoBeautyMapper<UserInfoBeauty, UserQuery> userInfoBeautyMapper;
	@Resource
	private Appconfig appconfig;
	@Resource
	private RedisComponent redisComponent;
	@Resource
	private UserContactMapper userContactMapper;
	@Resource
	private UserContactService userContactService;
	@Resource
	private ChatSessionUserService chatSessionUserService;
    @Autowired
    private MessageHandler messageHandler;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<User> findListByParam(UserQuery param) {
		return this.userMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserQuery param) {
		return this.userMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	// 因此这里我们可以看出 BaseParam 是暴露在外面的分页参数 SimplePage是藏在里面的
	// 前端是通过给BaseParam中分页有关的属性传参数 在service业务内封装成SimplePage
	@Override
	public PaginationResultVO<User> findListByPage(UserQuery param) {
		int count = this.findCountByParam(param); // 获取总记录数 这里查询的参数一个不传就可以查询到所有的记录数
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize(); // 确定每页显示的记录数，如果未指定则默认为15条

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
//		pageNo; // 当前页码 √
//		countTotal; // 总记录数 √
//		pageSize; // 每页记录数 √
//		pageTotal; // 总页数 一般不规定
//		start; // 起始记录
//		end; // 结束记录
		param.setSimplePage(page);
		List<User> list = this.findListByParam(param);// 关键还是在于这个list 其余的传到前端是为了设置分页控件的   // 总记录数  每页记录数  第几页  总页数  当前页展示的数据
		PaginationResultVO<User> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(User bean) {
		return this.userMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<User> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<User> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(User bean, UserQuery param) {
		StringTools.checkParam(param); //多条件更新 也就是检验必须要有一个条件是存在的 可以支持多条件查询
		return this.userMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserQuery param) {
		StringTools.checkParam(param);
		return this.userMapper.deleteByParam(param);
	}

	/**
	 * 根据UserId获取对象
	 */
	@Override
	public User getUserByUserId(String userId) {
		return this.userMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId修改
	 */
	@Override
	public Integer updateUserByUserId(User bean, String userId) {
		return this.userMapper.updateByUserId(bean, userId);
	}

	/**
	 * 根据UserId删除
	 */
	@Override
	public Integer deleteUserByUserId(String userId) {
		return this.userMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email获取对象
	 */
	@Override
	public User getUserByEmail(String email) {
		return this.userMapper.selectByEmail(email);
	}

	/**
	 * 根据Email修改
	 */
	@Override
	public Integer updateUserByEmail(User bean, String email) {
		return this.userMapper.updateByEmail(bean, email);
	}

	/**
	 * 根据Email删除
	 */
	@Override
	public Integer deleteUserByEmail(String email) {
		return this.userMapper.deleteByEmail(email);
	}

	/**
	 * 注册
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void register(RegisterDTO registerDTO) {
		Map<String,Object> result = new HashMap<>();
		// 1 先判断邮箱是否存在
		User user = this.userMapper.selectByEmail(registerDTO.getEmail());

		if(null!=user){
			throw new RuntimeException("邮箱已存在");
		}

		// 2 邮箱不存在 则查看这个邮箱是否绑定了靓号 有的话将该用户存储再用户表后，继续存储在靓号用户表，并关联
			String userId = StringTools.getUserId(); // 生成用户ID(随机)
            // 检查邮箱是否绑定靓号
			UserInfoBeauty beautyAccount = this.userInfoBeautyMapper.selectByEmail(registerDTO.getEmail());
			Boolean useBeautyAccount = beautyAccount != null && BeautyAccountStatusEnum.NO_USED
					                      .getStatus().equals(beautyAccount.getStatus());  // 靓号有并且没有使用 才使用
			if (useBeautyAccount) { // 使用靓号生成用户ID
				userId =UserContactTypeEnum.USER.getPrefix() + beautyAccount.getUserId(); // "U123"
			}

			Date curDate = new Date();
			user = new User();
			BeanUtils.copyProperties(registerDTO, user);
			user.setUserId(userId); // 设置UserId
			user.setPassword(StringTools.encodeMd5(registerDTO.getPassword())); // 按密码的规则进行校验，可以放在前端
			user.setCreateTime(curDate);
			user.setStatus(UserStatusEnum.ENABLED.getStatus());
//			user.setLastOffTime(curDate.getTime());
			user.setJoinType(JoinTypeEnum.APPLY.getType());
			this.userMapper.insert(user);

			// 用户注册成功后，将对应的靓号状态更新为 USED，防止重复使用
            if(useBeautyAccount){
				UserInfoBeauty updateBeauty = new UserInfoBeauty();
				updateBeauty.setStatus(BeautyAccountStatusEnum.USED.getStatus()); // 我不管了 0就是低电平 就是没用
				this.userInfoBeautyMapper.updateById(updateBeauty, beautyAccount.getId());
			}

		    userContactService.addContact4Robot(userId);


	}

	@Override
	public UserInfoVO login(LoginDTO loginDTO) {

		User user = this.userMapper.selectByEmail(loginDTO.getEmail());

		if(user == null || !user.getPassword().equals(StringTools.encodeMd5(loginDTO.getPassword()))){
			throw new BusinessException("账号或密码不存在");
		}

		if(UserStatusEnum.DISABLED.getStatus().equals(user.getStatus())){
			throw new BusinessException("账号被禁用");
		}

		// 查询联系人
		UserContactQuery contactQuery = new UserContactQuery();
		contactQuery.setUserId(user.getUserId());
		contactQuery.setStatus(UserContactStatusEnum.FRIEND.getCode());
		List<UserContact> contactList =  userContactMapper.selectList(contactQuery);
		List<String> contactIdList = contactList.stream().map(item->item.getContactId()).collect(Collectors.toList());

		redisComponent.cleanUserContact(user.getUserId());

		if(!contactList.isEmpty()){
			redisComponent.addUserContactBatch(user.getUserId(), contactIdList);
		}



        // 这行代码是用查到的user对象生成一个TokenUserInfoDTO
		TokenUserInfoDTO tokenUserInfoDTO = this.getTokenUserInfoDTO(user);


		Long lastHeartBeat = redisComponent.getUserHeartBeat(user.getUserId());
		if (lastHeartBeat != null) {
			throw new BusinessException("此账号已在别处登录，请退出后重新登陆");
		}

		// 保存登录信息到Redis中
		String token = StringTools.encodeMd5(tokenUserInfoDTO.getUserId() + StringTools.getRandomString(Constants.LENGTH_20));
		//生成用户访问令牌(token)，通过MD5加密用户ID和随机字符串的组合 自己写令牌 保证唯一性
		tokenUserInfoDTO.setToken(token);

		// ***** 保存用户信息到Redis中 保存了一个tokenUserInfoDTO *****
		redisComponent.saveTokenUserInfoDTO(tokenUserInfoDTO);


		// 封装VO
		UserInfoVO userInfoVO = CopyTools.copy(user, UserInfoVO.class);
		userInfoVO.setToken(tokenUserInfoDTO.getToken());
		userInfoVO.setAdmin(tokenUserInfoDTO.getAdmin());


		return userInfoVO;
	}

	@Override
	@Transactional(rollbackFor = Exception.class) // 启用事务的原因是该方法进行了多个业务逻辑操作（显式效果） 为了防止一部分效果实现 一部分效果又没实现的问题 开启事务
	public void updateUserInfo(User user, MultipartFile avatarFile, MultipartFile avatarCover) throws IOException {
		// 1 处理头像文件上传
		if(avatarFile != null){
			// 创建保存目录
			String basePath = appconfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
			File targetFileFolder = new File(basePath + Constants.FILE_FOLDER_AVATAR_NAME);
			if(!targetFileFolder.exists()){
				targetFileFolder.mkdirs(); // 目录不存在手动创建
			}
			// 保存原图和压缩图
			String filePath = targetFileFolder.getPath()+ "/"+user.getUserId() + Constants.IMAGE_SUFFIX; // .png 原图
			avatarFile.transferTo(new File(filePath)); // 使用 MultipartFile 的 transferTo() 方法将上传的头像文件保存到指定路径
			avatarCover.transferTo(new File(filePath.replace(Constants.IMAGE_SUFFIX, Constants.COVER_IMAGE_SUFFIX))); // 通过 replace() 方法将原文件路径的后缀替换为压缩图片后缀
		}

		// 2 更新用户信息到数据库 dbInfo是为了得到数据库中的昵称 用来检查昵称变化 和 改变会话中的昵称的
		User dbInfo = this.userMapper.selectByUserId(user.getUserId()); // 这个查数据库的操作比较耗时 会超过事务提交的时间，就会让事务失效
		this.userMapper.updateByUserId(user, user.getUserId()); // 查询不开启事务 更新会开启事务，更新在查询前 查询时间长 事务超时失效

		// 3 检查昵称变化
		String contactNameUpdate = null; // 昵称变化
		if(!dbInfo.getNickName().equals(user.getNickName())){
			contactNameUpdate = user.getNickName();
		}
		if(contactNameUpdate == null){
			return;
		}
		// 更新会话信息中的昵称信息 P32
		// 更新token中的昵称
		TokenUserInfoDTO tokenUserInfoDTO = redisComponent.getTokenUserInfoDTOByUserId(user.getUserId());
		tokenUserInfoDTO.setNickName(contactNameUpdate);
		redisComponent.saveTokenUserInfoDTO(tokenUserInfoDTO);

		chatSessionUserService.updateRedundantIfo(contactNameUpdate,user.getUserId());



	}

	@Override
	public void updateUserStatus(String userId, Integer status) {


		UserStatusEnum userStatusEnum = UserStatusEnum.getByStatus(status);
		if(userStatusEnum == null){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		User user = new User();
		user.setStatus(userStatusEnum.getStatus());
		this.userMapper.updateByUserId(user, userId);
	}

	@Override
	public void forceOffLine(String userId) {
		//  强制下线
		MessageSendDTO messageSendDTO = new MessageSendDTO();
		messageSendDTO.setContactType(UserContactTypeEnum.USER.getType());
		messageSendDTO.setMessageType(MessageTypeEnum.FORCE_OFF_LINE.getType());
		messageSendDTO.setContactId(userId);
		messageHandler.sendMessage(messageSendDTO);
	}

	//  这里是封装令牌 controller那里是从缓存中 取令牌 和 管理端的关键啊
	private TokenUserInfoDTO getTokenUserInfoDTO(User user){
		TokenUserInfoDTO tokenUserInfoDTO = new TokenUserInfoDTO();
		tokenUserInfoDTO.setUserId(user.getUserId());
		tokenUserInfoDTO.setNickName(user.getNickName());

		String adminEmails = appconfig.getAdminEmails();
		if(!StringTools.isEmpty(adminEmails) && ArrayUtils.contains(adminEmails.split(","),user.getEmail())){
			tokenUserInfoDTO.setAdmin(true);
		}else{
			tokenUserInfoDTO.setAdmin(false);
		}
		return tokenUserInfoDTO;
 	}
}