package cyf.gradle.shiro.configuration;

import cyf.gradle.dao.mapper.SysPermissionMapper;
import cyf.gradle.dao.mapper.SysRolePermissionMapper;
import cyf.gradle.dao.model.*;
import cyf.gradle.shiro.service.UserInfoServie;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * realm
 *
 * @author Cheng Yufei
 * @create 2017-08-15 14:40
 **/
public class MyShiroRealm extends AuthorizingRealm {


    @Resource
    private UserInfoServie userInfoService;

    @Autowired
    SysRolePermissionMapper rolePermissionMapper;

    @Autowired
    SysPermissionMapper sysPermissionMapper;

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        System.out.println("权限配置-->MyShiroRealm.doGetAuthorizationInfo()");
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        User userInfo  = (User)principals.getPrimaryPrincipal();
        List<Integer> roleIds = new ArrayList<>();
        for(SysRole role:userInfo.getRoleList()){
            authorizationInfo.addRole(role.getRole());
            roleIds.add(role.getId());
        }

        // 获取 permissionId
        List<Integer> permissionIds = new ArrayList<>();
        SysRolePermissionExample rolePermissionExample = new SysRolePermissionExample();
        SysRolePermissionExample.Criteria criteria1 = rolePermissionExample.createCriteria();
        criteria1.andRoleIdIn(roleIds);
        List<SysRolePermission> sysRolePermissions = rolePermissionMapper.selectByExample(rolePermissionExample);
        for (SysRolePermission sysRolePermission : sysRolePermissions) {
            permissionIds.add(sysRolePermission.getPermissionId());
        }

        //获取 syspermission
        List<SysPermission> sysPermissions = new ArrayList<>();
        for (Integer permissionId : permissionIds) {
            sysPermissions.add(sysPermissionMapper.selectByPrimaryKey(permissionId));
        }

        for(SysPermission p:sysPermissions){
            authorizationInfo.addStringPermission(p.getPermission());
        }

        return authorizationInfo;
    }

    /*主要是用来进行身份认证的，也就是说验证用户输入的账号和密码是否正确。*/
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
            throws AuthenticationException {
        System.out.println("MyShiroRealm.doGetAuthenticationInfo()");
        //获取用户的输入的账号.
        String username = (String)token.getPrincipal();
        System.out.println(token.getCredentials());
        //通过username从数据库中查找 User对象
        //实际项目中，这里可以根据实际情况做缓存，如果不做，Shiro自己也是有时间间隔机制，2分钟内不会重复执行该方法
        User userInfo = userInfoService.findByUsername(username);


        System.out.println("----->>userInfo="+userInfo);
        if(userInfo == null){
            return null;
        }
        SimpleAuthenticationInfo authenticationInfo = new SimpleAuthenticationInfo(
                userInfo, //用户名
                userInfo.getPwd(), //密码
                // ByteSource.Util.bytes(userInfo.getCredentialsSalt()),//salt=username+salt
                getName()  //realm name
        );
        return authenticationInfo;
    }

}