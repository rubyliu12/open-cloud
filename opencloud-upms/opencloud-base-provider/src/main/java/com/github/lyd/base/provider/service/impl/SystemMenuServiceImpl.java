package com.github.lyd.base.provider.service.impl;

import com.github.lyd.base.client.constants.BaseConstants;
import com.github.lyd.base.client.dto.SystemMenuDto;
import com.github.lyd.base.client.entity.SystemMenu;
import com.github.lyd.base.provider.mapper.SystemMenuMapper;
import com.github.lyd.base.provider.service.SystemGrantAccessService;
import com.github.lyd.base.provider.service.SystemMenuService;
import com.github.lyd.common.exception.OpenMessageException;
import com.github.lyd.common.mapper.ExampleBuilder;
import com.github.lyd.common.model.PageList;
import com.github.lyd.common.model.PageParams;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

/**
 * @author liuyadu
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class SystemMenuServiceImpl implements SystemMenuService {
    @Autowired
    private SystemMenuMapper systemMenuMapper;
    @Autowired
    private SystemGrantAccessService systemAccessService;

    /**
     * 分页查询
     *
     * @param pageParams
     * @param keyword
     * @return
     */
    @Override
    public PageList<SystemMenu> findListPage(PageParams pageParams, String keyword) {
        PageHelper.startPage(pageParams.getPage(), pageParams.getLimit(), pageParams.getOrderBy());
        return findList(keyword);
    }

    /**
     * 查询列表
     *
     * @param keyword
     * @return
     */
    @Override
    public PageList<SystemMenu> findList(String keyword) {
        ExampleBuilder builder = new ExampleBuilder(SystemMenu.class);
        Example example = builder.criteria()
                .orLike("menuCode", keyword)
                .orLike("menuName", keyword).end().build();
        example.orderBy("menuId").asc().orderBy("priority").asc();
        List<SystemMenu> list = systemMenuMapper.selectByExample(example);
        return new PageList(list);
    }

    /**
     * 获取菜单和操作列表
     *
     * @param keyword
     * @return
     */
    @Override
    public PageList<SystemMenuDto> findWithActionList(String keyword) {
        List<SystemMenuDto> list = systemMenuMapper.selectWithActionList();
        return new PageList(list);
    }

    /**
     * 根据主键获取菜单
     *
     * @param menuId
     * @return
     */
    @Override
    public SystemMenu getMenu(Long menuId) {
        return systemMenuMapper.selectByPrimaryKey(menuId);
    }

    /**
     * 检查菜单编码是否存在
     *
     * @param menuCode
     * @return
     */
    @Override
    public Boolean isExist(String menuCode) {
        ExampleBuilder builder = new ExampleBuilder(SystemMenu.class);
        Example example = builder.criteria()
                .andEqualTo("menuCode", menuCode)
                .end().build();
        int count = systemMenuMapper.selectCountByExample(example);
        return count > 0 ? true : false;
    }

    /**
     * 添加菜单资源
     *
     * @param menu
     * @return
     */
    @Override
    public Long addMenu(SystemMenu menu) {
        if (isExist(menu.getMenuCode())) {
            throw new OpenMessageException(String.format("%s菜单编码已存在,不允许重复添加", menu.getMenuCode()));
        }
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (menu.getPriority() == null) {
            menu.setPriority(0);
        }
        if (menu.getStatus() == null) {
            menu.setStatus(BaseConstants.ENABLED);
        }
        if (menu.getIsPersist() == null) {
            menu.setIsPersist(BaseConstants.DISABLED);
        }
        menu.setCreateTime(new Date());
        menu.setUpdateTime(menu.getCreateTime());
        systemMenuMapper.insertSelective(menu);
        return menu.getMenuId();
    }

    /**
     * 修改菜单资源
     *
     * @param menu
     * @return
     */
    @Override
    public void updateMenu(SystemMenu menu) {
        if (menu.getMenuId() == null) {
            throw new OpenMessageException("ID不能为空");
        }
        SystemMenu savedMenu = getMenu(menu.getMenuId());
        if (savedMenu == null) {
            throw new OpenMessageException(String.format("%s菜单不存在", menu.getMenuId()));
        }
        if (!savedMenu.getMenuCode().equals(menu.getMenuCode())) {
            // 和原来不一致重新检查唯一性
            if (isExist(menu.getMenuCode())) {
                throw new OpenMessageException(String.format("%s菜单编码已存在,不允许重复添加", menu.getMenuCode()));
            }
        }
        if (menu.getParentId() == null) {
            menu.setParentId(0l);
        }
        if (menu.getPriority() == null) {
            menu.setPriority(0);
        }
        menu.setUpdateTime(new Date());
        systemMenuMapper.updateByPrimaryKeySelective(menu);
        // 同步授权表里的信息
        systemAccessService.updateGrantAccess(BaseConstants.RESOURCE_TYPE_MENU, menu.getMenuId());
    }

    /**
     * 更新启用禁用
     *
     * @param menuId
     * @param status
     * @return
     */
    @Override
    public void updateStatus(Long menuId, Integer status) {
        SystemMenu menu = new SystemMenu();
        menu.setMenuId(menuId);
        menu.setStatus(status);
        menu.setUpdateTime(new Date());
        systemMenuMapper.updateByPrimaryKeySelective(menu);
        // 同步授权表里的信息
        systemAccessService.updateGrantAccess(BaseConstants.RESOURCE_TYPE_ACTION, menu.getMenuId());
    }

    /**
     * 移除菜单
     *
     * @param menuId
     * @return
     */
    @Override
    public void removeMenu(Long menuId) {
        SystemMenu menu = getMenu(menuId);
        if (menu != null && menu.getIsPersist().equals(BaseConstants.ENABLED)) {
            throw new OpenMessageException(String.format("保留数据,不允许删除"));
        }
        if (systemAccessService.isExist(menuId, BaseConstants.RESOURCE_TYPE_MENU)) {
            throw new OpenMessageException(String.format("资源已被授权,不允许删除,请取消授权后,再次尝试!", menuId));
        }
        systemMenuMapper.deleteByPrimaryKey(menuId);
    }


}
