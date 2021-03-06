package com.github.lyd.base.provider.service.impl;

import com.github.lyd.base.client.constants.BaseConstants;
import com.github.lyd.base.client.entity.*;
import com.github.lyd.base.provider.mapper.*;
import com.github.lyd.base.provider.service.SystemGrantAccessService;
import com.github.lyd.base.provider.service.SystemRoleService;
import com.github.lyd.common.exception.OpenMessageException;
import com.github.lyd.common.http.OpenRestTemplate;
import com.github.lyd.common.mapper.CrudMapper;
import com.github.lyd.common.mapper.ExampleBuilder;
import com.github.lyd.common.model.PageList;
import com.github.lyd.common.model.PageParams;
import com.github.lyd.common.utils.StringUtils;
import com.github.pagehelper.PageHelper;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;


/**
 * 访问授权
 * 对菜单、操作、API等进行权限分配操作
 *
 * @author liuyadu
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SystemGrantAccessServiceImpl implements SystemGrantAccessService {
    @Autowired
    private SystemGrantAccessMapper systemAccessMapper;
    @Autowired
    private SystemMenuMapper systemMenuMapper;
    @Autowired
    private SystemActionMapper systemActionMapper;
    @Autowired
    private SystemApiMapper systemApiMapper;
    @Autowired
    private SystemAppMapper systemAppMapper;
    @Autowired
    private SystemRoleService systemRoleService;
    @Autowired
    private OpenRestTemplate openRestTemplate;
    @Value("${spring.application.name}")
    private String DEFAULT_SERVICE_ID;
    private String DEFAULT_PREFIX = "/";

    private final List<String> AUTH_PREFIX_LIST = Lists.newArrayList(new String[]{
            BaseConstants.AUTHORITY_PREFIX_ROLE,
            BaseConstants.AUTHORITY_PREFIX_USER,
            BaseConstants.AUTHORITY_PREFIX_APP});

    /**
     * 获取mapper
     *
     * @param resourceType
     * @return
     */
    private CrudMapper getMapper(String resourceType) {
        // 判断资源类型
        if (BaseConstants.RESOURCE_TYPE_MENU.equals(resourceType)) {
            return systemMenuMapper;
        }
        if (BaseConstants.RESOURCE_TYPE_ACTION.equals(resourceType)) {
            return systemActionMapper;
        }
        if (BaseConstants.RESOURCE_TYPE_API.equals(resourceType)) {
            return systemApiMapper;
        }
        return null;
    }

    /**
     * 获取已授权列表
     *
     * @param pageParams
     * @param keyword
     * @return
     */
    @Override
    public PageList<SystemGrantAccess> findListPage(PageParams pageParams, String keyword) {
        PageHelper.startPage(pageParams.getPage(), pageParams.getLimit(), pageParams.getOrderBy());
        ExampleBuilder builder = new ExampleBuilder(SystemGrantAccess.class);
        Example example = builder.build();
        List<SystemGrantAccess> list = systemAccessMapper.selectByExample(example);
        return new PageList(list);
    }

    /**
     * 获取已授权列表
     *
     * @param authorityOwner
     * @param authorityPrefix
     * @param resourceType
     * @return
     */
    @Override
    public PageList<SystemGrantAccess> findList(String authorityOwner, String authorityPrefix, String resourceType) {
        ExampleBuilder builder = new ExampleBuilder(SystemGrantAccess.class);
        Example example = builder.criteria()
                .andEqualTo("authorityPrefix", authorityPrefix)
                .andEqualTo("resourceType", resourceType)
                .andEqualTo("authorityOwner", authorityOwner)
                .end().build();
        List<SystemGrantAccess> list = systemAccessMapper.selectByExample(example);
        return new PageList(list);
    }

    /**
     * 获取系统用户已授权列表(包含个人特殊权限和所拥有角色的所以权限)
     *
     * @param userId       系统用户ID
     * @param resourceType 资源类型
     * @return
     */
    @Override
    public List<SystemGrantAccess> getUserGrantAccessList(Long userId, String resourceType) {
        List<SystemRole> roles = systemRoleService.getUserRoles(userId);
        List<Long> roleIds = Lists.newArrayList();
        List<SystemGrantAccess> permissions = Lists.newArrayList();
        // 系统用户私有权限
        ExampleBuilder builder = new ExampleBuilder(SystemGrantAccess.class);
        Example example = builder.criteria()
                .andEqualTo("authorityPrefix", BaseConstants.AUTHORITY_PREFIX_USER)
                .andEqualTo("resourceType", resourceType)
                .andEqualTo("authorityOwner", userId)
                .andEqualTo("status", BaseConstants.ENABLED)
                .end().build();
        List<SystemGrantAccess> userAccesss = systemAccessMapper.selectByExample(example);
        if (userAccesss != null) {
            permissions.addAll(userAccesss);
        }
        //系统用户角色权限
        if (roles != null) {
            roles.forEach(rbacRole -> {
                roleIds.add(rbacRole.getRoleId());
            });
            if (!roleIds.isEmpty()) {
                //强制清空查询
                example.clear();
                example = builder.criteria()
                        .andEqualTo("authorityPrefix", BaseConstants.AUTHORITY_PREFIX_ROLE)
                        .andEqualTo("resourceType", resourceType)
                        .andIn("authorityOwner", roleIds)
                        .andEqualTo("status", BaseConstants.ENABLED)
                        .end().build();
                List<SystemGrantAccess> roleAccesss = systemAccessMapper.selectByExample(example);
                if (roleAccesss != null) {
                    permissions.addAll(roleAccesss);
                }
            }
        }
        // 去重
        permissions = permissions.stream()
                .collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(f -> f.getResourceId() + f.getResourceType()))), ArrayList::new));
        // 排序
        permissions.sort((o1, o2) -> o1.getId().compareTo(o2.getId()));
        return permissions;
    }

    /**
     * 获取已授权列表
     *
     * @param resourceType
     * @return
     */
    @Override
    public List<SystemGrantAccess> getGrantAccessList(String resourceType) {
        ExampleBuilder builder = new ExampleBuilder(SystemGrantAccess.class);
        Example example = builder.criteria()
                .andEqualTo("resourceType", resourceType)
                .andEqualTo("status", BaseConstants.ENABLED)
                .end().build();
        List<SystemGrantAccess> accessList = systemAccessMapper.selectByExample(example);
        return accessList;
    }


    /**
     * 获取系统用户已授权私有列表(不包含角色权限)
     *
     * @param userId 系统用户ID
     * @return
     */
    @Override
    public List<SystemGrantAccess> getUserPrivateGrantAccessList(Long userId) {
        List<SystemGrantAccess> permissions = Lists.newArrayList();
        // 系统用户私有权限
        ExampleBuilder builder = new ExampleBuilder(SystemGrantAccess.class);
        Example example = builder.criteria()
                .andEqualTo("authorityPrefix", BaseConstants.AUTHORITY_PREFIX_USER)
                .andEqualTo("authorityOwner", userId)
                .andEqualTo("status", BaseConstants.ENABLED)
                .end().build();
        List<SystemGrantAccess> userAccesss = systemAccessMapper.selectByExample(example);
        if (userAccesss != null) {
            permissions.addAll(userAccesss);
        }
        return permissions;
    }

    /**
     * 获取所有已授权访问列表
     *
     * @return
     */
    @Override
    public List<SystemGrantAccess> getGrantAccessList() {
        ExampleBuilder builder = new ExampleBuilder(SystemGrantAccess.class);
        Example example = builder.criteria()
                .andEqualTo("status", BaseConstants.ENABLED)
                .end().build();
        return systemAccessMapper.selectByExample(example);
    }

    /**
     * 添加授权
     *
     * @param authorityOwner  权限拥有者ID
     * @param authorityPrefix 拥有者类型
     * @param resourceType    资源类型
     * @param resourceIds     资源ID
     * @return authorities
     */
    @Override
    public String addGrantAccess(String authorityOwner, String authorityPrefix, String resourceType, String... resourceIds) {
        if (!AUTH_PREFIX_LIST.contains(authorityPrefix)) {
            throw new OpenMessageException(String.format("%s授权类型暂不支持!", authorityPrefix));
        }
        CrudMapper crudMapper = getMapper(resourceType);
        if (crudMapper == null) {
            throw new OpenMessageException(String.format("%s资源类型暂不支持!", resourceType));
        }
        List<SystemGrantAccess> accessList = Lists.newArrayList();
        List<String> authorities = Lists.newArrayList();
        if (resourceIds != null) {
            for (String resource : resourceIds) {
                Object object = crudMapper.selectByPrimaryKey(resource);
                SystemGrantAccess grantAccess = buildGrantAccess(resourceType, authorityPrefix, authorityOwner, object);
                if (grantAccess != null) {
                    accessList.add(grantAccess);
                    authorities.add(grantAccess.getAuthority());
                }
            }
        }
        //先清空拥有者的权限
        removeGrantAccess(authorityOwner, authorityPrefix, resourceType);
        if (accessList.size() > 0) {
            // 再重新批量授权
            systemAccessMapper.insertList(accessList);
        }
        // 刷新网关
        openRestTemplate.refreshGateway();
        return org.springframework.util.StringUtils.arrayToDelimitedString(authorities.toArray(new String[accessList.size()]), ",");
    }

    /**
     * 移除授权
     *
     * @param authorityOwner
     * @param authorityPrefix
     * @return
     */
    @Override
    public void removeGrantAccess(String authorityOwner, String authorityPrefix, String resourceType) {
        //先清空拥有者的权限
        ExampleBuilder builder = new ExampleBuilder(SystemGrantAccess.class);
        Example example = builder.criteria()
                .andEqualTo("authorityPrefix", authorityPrefix)
                .andEqualTo("authorityOwner", authorityOwner)
                .andEqualTo("resourceType", resourceType)
                .end().build();
        systemAccessMapper.deleteByExample(example);
        // 刷新网关
        openRestTemplate.refreshGateway();
    }


    /**
     * 更新授权信息
     *
     * @param resourceType
     * @param resourceId
     * @return
     */
    @Override
    public void updateGrantAccess(String resourceType, Long resourceId) {
        // 判断资源类型
        CrudMapper crudMapper = getMapper(resourceType);
        if (crudMapper == null) {
            return;
        }
        Object object = crudMapper.selectByPrimaryKey(resourceId);
        if (object != null) {
            SystemGrantAccess grantAccess = buildGrantAccess(resourceType, null, null, object);
            if (grantAccess != null) {
                ExampleBuilder builder = new ExampleBuilder(SystemGrantAccess.class);
                Example example = builder.criteria().andEqualTo("resourceId", resourceId)
                        .andEqualTo("resourceType", resourceType).end().build();
                SystemGrantAccess updateObj = new SystemGrantAccess();
                updateObj.setStatus(grantAccess.getStatus());
                updateObj.setServiceId(grantAccess.getServiceId());
                updateObj.setResourcePid(grantAccess.getResourcePid());
                updateObj.setPath(grantAccess.getPath());
                updateObj.setResourceInfo(grantAccess.getResourceInfo());
                int count = systemAccessMapper.updateByExampleSelective(updateObj, example);
                return;
            }

        }
        // 刷新网关
        openRestTemplate.refreshGateway();
        return;
    }

    /**
     * 检查资源是否已授权
     *
     * @param resourceId
     * @param resourceType
     * @return
     */
    @Override
    public Boolean isExist(Long resourceId, String resourceType) {
        ExampleBuilder builder = new ExampleBuilder(SystemGrantAccess.class);
        Example example = builder.criteria()
                .andEqualTo("resourceId", resourceId)
                .andEqualTo("resourceType", resourceType)
                .end().build();
        int count = systemAccessMapper.selectCountByExample(example);
        return count > 0;
    }

    /**
     * 构建授权对象
     *
     * @param resourceType    资源类型
     * @param authorityPrefix 授权拥有者类型
     * @param authorityOwner  授权权限拥有者ID
     * @param object          资源对象
     * @return
     */
    protected SystemGrantAccess buildGrantAccess(String resourceType, String authorityPrefix, String authorityOwner, Object object) {
        if (object == null) {
            return null;
        }
        String path = null;
        String code = "";
        Long resourceId = null;
        Long resourcePid = null;
        String serviceId = "";
        String authority = "";
        Integer status = 0;
        SystemGrantAccess grantAccess = null;
        if (object instanceof SystemMenu) {
            SystemMenu menu = (SystemMenu) object;
            path = menu.getPath();
            resourceId = menu.getMenuId();
            resourcePid = menu.getParentId();
            serviceId = DEFAULT_SERVICE_ID;
            status = menu.getStatus();
            code = menu.getMenuCode();
        }
        if (object instanceof SystemAction) {
            SystemAction action = (SystemAction) object;
            path = action.getPath();
            resourceId = action.getActionId();
            resourcePid = action.getMenuId();
            serviceId = DEFAULT_SERVICE_ID;
            status = action.getStatus();
            code = action.getActionCode();
        }
        if (object instanceof SystemApi) {
            SystemApi api = (SystemApi) object;
            path = api.getPath();
            resourceId = api.getApiId();
            resourcePid = 0L;
            serviceId = api.getServiceId();
            status = api.getStatus();
            code = api.getApiCode();
        }
        if (object != null) {
            if (authorityPrefix != null) {
                if (BaseConstants.AUTHORITY_PREFIX_ROLE.equals(authorityPrefix)) {
                    SystemRole role = systemRoleService.getRole(Long.parseLong(authorityOwner));
                    // 角色授权标识=ROLE_角色编码
                    authority = authorityPrefix + role.getRoleCode();
                } else {
                    // APP授权状态
                    if (BaseConstants.AUTHORITY_PREFIX_APP.equals(authorityPrefix)) {
                        SystemApp systemApp = systemAppMapper.selectByPrimaryKey(authorityOwner);
                        if (systemApp != null) {
                            // 根据APP的状态强制覆盖
                            status = systemApp.getStatus();
                        }
                    }
                    if ("all".equals(code)) {
                        authority = authorityPrefix + code;
                    } else {
                        authority = authorityPrefix + resourceType + BaseConstants.AUTHORITY_SEPARATOR + code;
                    }
                }
            }
            grantAccess = new SystemGrantAccess();
            grantAccess.setServiceId(serviceId);
            grantAccess.setResourceId(resourceId);
            grantAccess.setResourcePid(resourcePid);
            if (StringUtils.isNotBlank(path)) {
                // 去掉/
                if (path.startsWith(DEFAULT_PREFIX)) {
                    path = path.substring(1);
                }
            }
            grantAccess.setResourceType(resourceType);
            grantAccess.setPath(path);
            grantAccess.setStatus(status);
            grantAccess.setAuthority(authority);
            grantAccess.setAuthorityOwner(authorityOwner);
            grantAccess.setAuthorityPrefix(authorityPrefix);
            grantAccess.setResourceInfo(object);
        }
        return grantAccess;
    }
}
