package com.github.lyd.base.provider.controller;

import com.github.lyd.auth.client.dto.ClientDetailsDto;
import com.github.lyd.base.client.api.SystemAppRemoteService;
import com.github.lyd.base.client.dto.SystemAppDto;
import com.github.lyd.base.client.entity.SystemApp;
import com.github.lyd.base.provider.service.SystemAppService;
import com.github.lyd.common.model.PageList;
import com.github.lyd.common.model.PageParams;
import com.github.lyd.common.model.ResultBody;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户信息
 *
 * @author liuyadu
 */
@Api(tags = "应用管理")
@RestController
public class SystemAppController implements SystemAppRemoteService {
    @Autowired
    private SystemAppService appInfoService;


    /**
     * 获取应用分页列表
     *
     * @return
     */
    @ApiOperation(value = "获取应用分页列表", notes = "获取应用分页列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "当前页码", paramType = "form"),
            @ApiImplicitParam(name = "limit", value = "显示条数:最大999", paramType = "form"),
            @ApiImplicitParam(name = "keyword", value = "查询字段", paramType = "form"),
    })
    @PostMapping("/app")
    @Override
    public ResultBody<PageList<SystemApp>> app(
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit,
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        PageList<SystemApp> pageList = appInfoService.findListPage(new PageParams(page, limit), keyword);
        return ResultBody.success(pageList);
    }

    /**
     * 获取应用信息
     *
     * @param appId appId
     * @return 应用信息
     */
    @ApiOperation(value = "获取应用信息", notes = "获取应用信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", defaultValue = "1", required = true, paramType = "path"),
    })
    @GetMapping("/app/{appId}")
    @Override
    public ResultBody<SystemApp> getApp(
            @PathVariable("appId") String appId
    ) {
        SystemApp appInfo = appInfoService.getAppInfo(appId);
        return ResultBody.success(appInfo);
    }

    /**
     * 获取应用开发配置信息
     *
     * @param appId 应用Id
     * @return
     */
    @ApiOperation(value = "获取应用开发配置信息", notes = "获取应用开发配置信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用ID", defaultValue = "1", required = true, paramType = "path"),
    })
    @GetMapping("/app/dev/{appId}")
    @Override
    public ResultBody<ClientDetailsDto> getAppDevInfo(
            @PathVariable("appId") String appId
    ) {
        SystemAppDto appInfo = appInfoService.getAppWithClientInfo(appId);
        if (appInfo == null) {
            return ResultBody.success(null);
        }
        return ResultBody.success(appInfo.getClientInfo());
    }

    /**
     * 添加应用信息
     *
     * @param appName      应用名称
     * @param appNameEn    应用英文名称
     * @param appOs        手机应用操作系统:ios-苹果 android-安卓
     * @param appType      应用类型:server-应用服务 app-手机应用 pc-PC网页应用 wap-手机网页应用
     * @param appIcon      应用图标
     * @param appDesc      应用说明
     * @param status       状态
     * @param website      官网地址
     * @param redirectUrls 第三方应用授权回调地址(多个使用,号隔开)
     * @param userId       开发者
     * @param userType     开发者类型
     * @param scopes       用户授权范围(多个使用,号隔开)
     * @param authorities  功能权限.这里指的是接口标识(多个使用,号隔开)
     * @param grantTypes   授权类型(多个使用,号隔开)
     * @return
     */
    @ApiOperation(value = "添加应用信息", notes = "添加应用信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appName", value = "应用名称", required = true, paramType = "form"),
            @ApiImplicitParam(name = "appNameEn", value = "应用英文名称", required = true, paramType = "form"),
            @ApiImplicitParam(name = "appType", value = "应用类型(server-应用服务 app-手机应用 pc-PC网页应用 wap-手机网页应用)", allowableValues = "server,app,pc,wap", required = true, paramType = "form"),
            @ApiImplicitParam(name = "appIcon", value = "应用图标", paramType = "form"),
            @ApiImplicitParam(name = "appOs", value = "手机应用操作系统", allowableValues = "android,ios", required = false, paramType = "form"),
            @ApiImplicitParam(name = "appDesc", value = "应用说明", required = false, paramType = "form"),
            @ApiImplicitParam(name = "status", required = true, defaultValue = "1", allowableValues = "0,1", value = "是否启用", paramType = "form"),
            @ApiImplicitParam(name = "website", value = "官网地址", required = true, paramType = "form"),
            @ApiImplicitParam(name = "redirectUrls", value = "第三方应用授权回调地址", required = true, paramType = "form"),
            @ApiImplicitParam(name = "userId", value = "0-平台,其他填写真实Id", required = true, paramType = "form"),
            @ApiImplicitParam(name = "userType", value = "开发者类型", allowableValues = "platform,isp,dev", required = true, paramType = "form"),
            @ApiImplicitParam(name = "scopes", value = "用户授权范围(多个使用,号隔开)", required = true, paramType = "form"),
            @ApiImplicitParam(name = "authorities", value = "功能权限.这里指的是接口标识(多个使用,号隔开)", required = true, paramType = "form"),
            @ApiImplicitParam(name = "grantTypes", value = "授权类型(多个使用,号隔开)", required = true, paramType = "form")
    })
    @PostMapping("/app/add")
    @Override
    public ResultBody<String> addApp(
            @RequestParam(value = "appName") String appName,
            @RequestParam(value = "appNameEn") String appNameEn,
            @RequestParam(value = "appType") String appType,
            @RequestParam(value = "appIcon") String appIcon,
            @RequestParam(value = "appOs", required = false) String appOs,
            @RequestParam(value = "appDesc", required = false) String appDesc,
            @RequestParam(value = "status", defaultValue = "1") Integer status,
            @RequestParam(value = "website") String website,
            @RequestParam(value = "redirectUrls") String redirectUrls,
            @RequestParam(value = "userId") Long userId,
            @RequestParam(value = "userType") String userType,
            @RequestParam(value = "scopes") String scopes,
            @RequestParam(value = "authorities",required = false) String authorities,
            @RequestParam(value = "grantTypes") String grantTypes
    ) {
        SystemAppDto app = new SystemAppDto();
        app.setAppName(appName);
        app.setAppNameEn(appNameEn);
        app.setAppType(appType);
        app.setAppOs(appOs);
        app.setAppIcon(appIcon);
        app.setAppDesc(appDesc);
        app.setStatus(status);
        app.setWebsite(website);
        app.setRedirectUrls(redirectUrls);
        app.setUserId(userId);
        app.setUserType(userType);
        app.setScopes(scopes);
        app.setAuthorities(authorities);
        app.setGrantTypes(grantTypes);
        String result = appInfoService.addAppInfo(app);
        return ResultBody.success(result);
    }

    /**
     * 编辑应用信息
     *
     * @param appId
     * @param appName      应用名称
     * @param appNameEn    应用英文名称
     * @param appOs        手机应用操作系统:ios-苹果 android-安卓
     * @param appType      应用类型:server-应用服务 app-手机应用 pc-PC网页应用 wap-手机网页应用
     * @param appIcon      应用图标
     * @param appDesc      应用说明
     * @param status       状态
     * @param website      官网地址
     * @param redirectUrls 第三方应用授权回调地址(多个使用,号隔开)
     * @param userId       开发者
     * @param userType     开发者类型
     * @param scopes       用户授权范围(多个使用,号隔开)
     * @param authorities  功能权限.这里指的是接口标识(多个使用,号隔开)
     * @param grantTypes   授权类型(多个使用,号隔开)
     * @return
     * @
     */
    @ApiOperation(value = "编辑应用信息", notes = "编辑应用信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用Id", required = true, paramType = "form"),
            @ApiImplicitParam(name = "appName", value = "应用名称", required = true, paramType = "form"),
            @ApiImplicitParam(name = "appNameEn", value = "应用英文名称", required = true, paramType = "form"),
            @ApiImplicitParam(name = "appType", value = "应用类型(server-应用服务 app-手机应用 pc-PC网页应用 wap-手机网页应用)", allowableValues = "server,app,pc,wap", required = true, paramType = "form"),
            @ApiImplicitParam(name = "appIcon", value = "应用图标", paramType = "form"),
            @ApiImplicitParam(name = "appOs", value = "手机应用操作系统", allowableValues = "android,ios", required = false, paramType = "form"),
            @ApiImplicitParam(name = "appDesc", value = "应用说明", required = false, paramType = "form"),
            @ApiImplicitParam(name = "status", required = true, defaultValue = "1", allowableValues = "0,1", value = "是否启用", paramType = "form"),
            @ApiImplicitParam(name = "website", value = "官网地址", required = true, paramType = "form"),
            @ApiImplicitParam(name = "redirectUrls", value = "第三方应用授权回调地址", required = true, paramType = "form"),
            @ApiImplicitParam(name = "userId", value = "0-平台,其他填写真实Id", required = true, paramType = "form"),
            @ApiImplicitParam(name = "userType", value = "开发者类型", allowableValues = "platform,isp,dev", required = true, paramType = "form"),
            @ApiImplicitParam(name = "scopes", value = "用户授权范围(多个使用,号隔开)", required = true, paramType = "form"),
            @ApiImplicitParam(name = "authorities", value = "功能权限.这里指的是接口标识(多个使用,号隔开)", required = true, paramType = "form"),
            @ApiImplicitParam(name = "grantTypes", value = "授权类型(多个使用,号隔开)", required = true, paramType = "form")
    })
    @PostMapping("/app/update")
    @Override
    public ResultBody updateApp(
            @RequestParam("appId") String appId,
            @RequestParam(value = "appName") String appName,
            @RequestParam(value = "appNameEn") String appNameEn,
            @RequestParam(value = "appType") String appType,
            @RequestParam(value = "appIcon", required = false) String appIcon,
            @RequestParam(value = "appOs", required = false) String appOs,
            @RequestParam(value = "appDesc", required = false) String appDesc,
            @RequestParam(value = "status", defaultValue = "1") Integer status,
            @RequestParam(value = "website") String website,
            @RequestParam(value = "redirectUrls") String redirectUrls,
            @RequestParam(value = "userId") Long userId,
            @RequestParam(value = "userType") String userType,
            @RequestParam(value = "scopes") String scopes,
            @RequestParam(value = "authorities",required = false) String authorities,
            @RequestParam(value = "grantTypes") String grantTypes
    ) {
        SystemAppDto app = new SystemAppDto();
        app.setAppId(appId);
        app.setAppName(appName);
        app.setAppNameEn(appNameEn);
        app.setAppType(appType);
        app.setAppOs(appOs);
        app.setAppIcon(appIcon);
        app.setAppDesc(appDesc);
        app.setStatus(status);
        app.setWebsite(website);
        app.setRedirectUrls(redirectUrls);
        app.setUserId(userId);
        app.setUserType(userType);
        app.setScopes(scopes);
        app.setAuthorities(authorities);
        app.setGrantTypes(grantTypes);
        appInfoService.updateInfo(app);
        return ResultBody.success();
    }

    /**
     * 重置应用秘钥
     *
     * @param appId 应用Id
     * @return 应用信息
     */
    @ApiOperation(value = "重置应用秘钥", notes = "重置应用秘钥")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用Id", required = true, paramType = "form"),
    })
    @PostMapping("/app/reset")
    @Override
    public ResultBody<String> resetSecret(
            @RequestParam("appId") String appId
    ) {
        String result = appInfoService.restSecret(appId);
        return ResultBody.success(result);
    }

    /**
     * 删除应用信息
     *
     * @param appId 应用Id
     * @return 应用信息
     */
    @ApiOperation(value = "删除应用信息", notes = "删除应用信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "appId", value = "应用Id", required = true, paramType = "form"),
    })
    @PostMapping("/app/remove")
    @Override
    public ResultBody removeApp(
            @RequestParam("appId") String appId
    ) {
        appInfoService.removeApp(appId);
        return ResultBody.success();
    }
}
