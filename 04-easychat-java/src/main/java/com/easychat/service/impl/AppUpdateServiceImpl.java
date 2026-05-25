package com.easychat.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.easychat.entity.config.Appconfig;
import com.easychat.entity.constants.Constants;
import com.easychat.entity.enums.AppUpdateFileTypeEnum;
import com.easychat.entity.enums.AppUpdateStatusEnum;
import com.easychat.entity.enums.ResponseCodeEnum;
import com.easychat.exception.BusinessException;
import org.springframework.stereotype.Service;

import com.easychat.entity.enums.PageSize;
import com.easychat.entity.query.AppUpdateQuery;
import com.easychat.entity.po.AppUpdate;
import com.easychat.entity.vo.PaginationResultVO;
import com.easychat.entity.query.SimplePage;
import com.easychat.mappers.AppUpdateMapper;
import com.easychat.service.AppUpdateService;
import com.easychat.utils.StringTools;
import org.springframework.web.multipart.MultipartFile;


/**
 * app发布 业务接口实现
 */
@Service("appUpdateService")
public class AppUpdateServiceImpl implements AppUpdateService {

	@Resource
	private AppUpdateMapper<AppUpdate, AppUpdateQuery> appUpdateMapper;
    @Resource
    private Appconfig appConfig;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<AppUpdate> findListByParam(AppUpdateQuery param) {
		return this.appUpdateMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(AppUpdateQuery param) {
		return this.appUpdateMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<AppUpdate> findListByPage(AppUpdateQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<AppUpdate> list = this.findListByParam(param);
		PaginationResultVO<AppUpdate> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(AppUpdate bean) {
		return this.appUpdateMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<AppUpdate> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.appUpdateMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<AppUpdate> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.appUpdateMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(AppUpdate bean, AppUpdateQuery param) {
		StringTools.checkParam(param);
		return this.appUpdateMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(AppUpdateQuery param) {
		StringTools.checkParam(param);
		return this.appUpdateMapper.deleteByParam(param);
	}

	/**
	 * 根据Id获取对象
	 */
	@Override
	public AppUpdate getAppUpdateById(Integer id) {
		return this.appUpdateMapper.selectById(id);
	}

	/**
	 * 根据Id修改
	 */
	@Override
	public Integer updateAppUpdateById(AppUpdate bean, Integer id) {
		return this.appUpdateMapper.updateById(bean, id);
	}

	/**
	 * 根据Id删除
	 */
	@Override
	public Integer deleteAppUpdateById(Integer id) {


		AppUpdate dbInfo = this.getAppUpdateById(id);
		if(AppUpdateStatusEnum.INIT.getCode().equals(dbInfo.getStatus())){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		return this.appUpdateMapper.deleteById(id);
	}

	/**
	 * 根据Version获取对象
	 */
	@Override
	public AppUpdate getAppUpdateByVersion(String version) {
		return this.appUpdateMapper.selectByVersion(version);
	}

	/**
	 * 根据Version修改
	 */
	@Override
	public Integer updateAppUpdateByVersion(AppUpdate bean, String version) {
		return this.appUpdateMapper.updateByVersion(bean, version);
	}

	/**
	 * 根据Version删除
	 */
	@Override
	public Integer deleteAppUpdateByVersion(String version) {
		return this.appUpdateMapper.deleteByVersion(version);
	}

	@Override
	public void saveUpdate(AppUpdate appUpdate, MultipartFile file) throws IOException {
		AppUpdateFileTypeEnum fileTypeEnum = AppUpdateFileTypeEnum.getByType(appUpdate.getFileType());
		if(null == fileTypeEnum){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		if(appUpdate.getId() != null){
			AppUpdate dbInfo = this.getAppUpdateById(appUpdate.getId());
			if(AppUpdateStatusEnum.INIT.getCode().equals(dbInfo.getStatus())){
				throw new BusinessException(ResponseCodeEnum.CODE_600);
			}
		}



		AppUpdateQuery updateQuery = new AppUpdateQuery();
		updateQuery.setOrderBy("id desc");
		updateQuery.setSimplePage(new SimplePage(0, 1));
		List<AppUpdate> appUpdateList = this.findListByParam(updateQuery);
		if(!appUpdateList.isEmpty()){
			AppUpdate lastUpdate = appUpdateList.get(0);
			Long dbVersion = Long.parseLong(lastUpdate.getVersion().replace(".", ""));
			// 传过来的当前版本
			Long currentVersion = Long.parseLong(appUpdate.getVersion().replace(".", ""));
			// 新增的时候判断版本号
			if(appUpdate.getId() == null && currentVersion <= dbVersion){
				throw new BusinessException("当前版本必须大于历史版本");
			}
			// 修改的时候判断版本号
			if(appUpdate.getId()!= null && currentVersion >= dbVersion && !appUpdate.getId().equals(lastUpdate.getId())){
				throw new BusinessException("当前版本必须大于历史版本");
			}

            AppUpdate versionDb = appUpdateMapper.selectByVersion(appUpdate.getVersion());
			if(appUpdate.getId()!=null && versionDb != null && !versionDb.getId().equals(appUpdate.getId())){
				throw new BusinessException("当前版本已经存在");
			}

		}



		if(appUpdate.getId() == null){
			appUpdate.setCreateTime(new Date());
			appUpdate.setStatus(AppUpdateStatusEnum.INIT.getCode());
			appUpdateMapper.insert(appUpdate);
		}else{
//			appUpdate.setStatus(null);
//			appUpdate.setGrayscaleUid( null);
			// 置null的原因： 避免用户修改的时候，把状态置为初始化 走接口攻击
			// 而这里可以不用置null是因为我的controller层接受参数时 没有暴露AppUpdate这个对象
			appUpdateMapper.updateById(appUpdate, appUpdate.getId());
		}
		if(file != null){
		     File folder = new File(appConfig.getProjectFolder() + Constants.APP_UPDATE_FOLDER);
			 if(!folder.exists()){
				 folder.mkdirs();
			 }
			 file.transferTo(new File(folder.getAbsolutePath() + "/" + appUpdate.getId() + Constants.APP_EXE_SUFFIX));

		}

	}

	 /**
	  * 发布更新
	  * @param id
	  * @param status
	  * @param grayscaleUid
	  */
	@Override
	public void postUpdate(Integer id, Integer status, String grayscaleUid) {




		AppUpdateStatusEnum statusEnum = AppUpdateStatusEnum.getByCode(status);
		if(null == statusEnum){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		if(AppUpdateStatusEnum.GRAYSCALE == statusEnum && StringTools.isEmpty(grayscaleUid)){
			throw new BusinessException(ResponseCodeEnum.CODE_600);
		}

		if(AppUpdateStatusEnum.GRAYSCALE != statusEnum){
			grayscaleUid = "";
		}

		AppUpdate appUpdate = new AppUpdate();
		appUpdate.setStatus(status);
		appUpdate.setGrayscaleUid(grayscaleUid);
		appUpdateMapper.updateById(appUpdate, id);


	}

	@Override
	public AppUpdate getLastUpdate(String appVersion, String uid) {
		return appUpdateMapper.selectLatestUpdate(appVersion, uid);
	}
}