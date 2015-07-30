package wjw.test.shrio.redis;

import java.util.HashMap;

import junit.framework.TestCase;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wjw.shiro.redis.RedisManager;
import wjw.shiro.redis.RedisRealm;

/**
 * 
 * 
 * @author Chris Spiliotopoulos
 * 
 */
public class TestRedisRealmManager extends TestCase {

  final static Logger log = LoggerFactory.getLogger(TestRedisRealmManager.class);

  @Before
  public void setUp() throws Exception {
    RedisManager redis = new RedisManager();
    redis.setServerlist("127.0.0.1:6379");
    redis.setMinConn(5);
    redis.setMaxConn(10);
    redis.init();

    RedisRealm redisRealm = new RedisRealm();
    redisRealm.setRedisManager(redis);
    redisRealm.setPermissionsLookupEnabled(true);

    redis.flushDB();
    //1. 初始化 用户: 存放在hash里,key的模式是:"shiro_realm:users:$username" <br/>
    redisRealm.addUser("root", "123456");
    redisRealm.addUser("guest", "guest123");

    //2. 初始化 角色: 存放在set里,key的模式是:"shiro_realm:all_roles"
    redisRealm.addRole("admin", "guest");

    //3. 用户拥有的角色:存放在set里,key的模式是:"shiro_realm:user_roles:$username" <br/>
    redisRealm.addUserOwnedRoles("root", "admin", "guest");
    redisRealm.addUserOwnedRoles("guest", new String[] { "guest" });

    //4.  角色对应的权限:存放在hash里,key的模式是:"shiro_realm:roles_permissions" <br/>
    redisRealm.addRolePermission("admin", "*");
    redisRealm.addRolePermission("guest", "*:*:view");
    redis.destroy();

    log.info("TestRedisRealmManager");

    // create a factory instance
    Factory<SecurityManager> factory = new IniSecurityManagerFactory("classpath:shiro-realm.ini");

    // get a new security manager instance
    SecurityManager securityManager = factory.getInstance();

    // set it globally
    SecurityUtils.setSecurityManager(securityManager);

  }

  @After
  public void tearDown() throws Exception {
    // logout the subject
    Subject subject = SecurityUtils.getSubject();
    subject.logout();
  }

  @Test
  public void testUser1() throws Exception {
    /*
     * login the current subject
     */
    Subject subject = SecurityUtils.getSubject();

    // use a username/pass token
    UsernamePasswordToken token = new UsernamePasswordToken("root", "123456");
    token.setRememberMe(true);
    subject.login(token);

    log.info("User successfuly logged in");

    Thread.sleep(2 * 1000);
    log.info("hasRole:" + subject.hasRole("admin"));

    Thread.sleep(2 * 1000);

    log.info("isPermitted:" + subject.isPermitted("*"));

    subject.logout();
  }

  @Test
  public void testCRUD() throws Exception {
    RedisManager redis = new RedisManager();
    redis.setServerlist("127.0.0.1:6379");
    redis.setMinConn(5);
    redis.setMaxConn(10);
    redis.init();

    RedisRealm redisRealm = new RedisRealm();
    redisRealm.setRedisManager(redis);
    redisRealm.setPermissionsLookupEnabled(true);
    
    
    redisRealm.removeUser("root");
    redisRealm.removeRole("admin");
    
    redis.destroy();
 }
}