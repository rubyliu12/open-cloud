package com.github.lyd.base.provider.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.lyd.base.provider.service.SystemRoleService;
import com.github.lyd.common.test.BaseTest;
import com.github.lyd.common.utils.DateUtils;
import com.github.lyd.common.utils.SignatureUtils;
import com.github.lyd.base.client.entity.SystemRole;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

/**
 * @author: liuyadu
 * @date: 2018/11/19 15:23
 * @description:
 */
public class RoleServiceImplTest extends BaseTest {
    @Autowired
    private SystemRoleService roleService;

    @Test
    public void getRole() throws Exception {
        SystemRole role = roleService.getRole(1l);
        System.out.println(JSONObject.toJSONString(role));
    }

    @Test
    public void removeRole() throws Exception {
        roleService.removeRole(5l);
    }

    @Test
    public void removeUserRoleByRole() throws Exception {
        roleService.removeRoleMembers(4l);
    }

    @Test
    public void removeUserRoleByUserId() throws Exception {
        roleService.removeMemberRoles(1l);
    }

    public static void main(String[] args) {
        //参数签名算法测试例子
        HashMap<String, String> signMap = new HashMap<String, String>();
        signMap.put("clientId", "BC5549D899ED");
        signMap.put("signType", "MD5");
        signMap.put("timestamp", DateUtils.getTimestampStr());
        signMap.put("nonce", "5df8f2914d9f485a96947cd379b6b34b");
        signMap.put("userId", "1");
        signMap.put("type", "worker");
        signMap.put("name", "中文测试");
        String sign = SignatureUtils.getSign(signMap, "e10adc3949ba59abbe56e057f20f883f");
        System.out.println("得到签名sign1:" + sign);
        signMap.put("sign", sign);
        System.out.println(SignatureUtils.validateSign(signMap, "e10adc3949ba59abbe56e057f20f883f"));
    }
}
