package wjw.shiro.redis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.crypto.RandomNumberGenerator;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户: 存放在hash里,key的模式是:"shiro_realm:users:$username" <br/>
 * 角色: 存放在set里,key的模式是:"shiro_realm:all_roles" <br/>
 * 用户拥有的角色:存放在set里,key的模式是:"shiro_realm:user_roles:$username" <br/>
 * 角色对应的权限:存放在hash里,key的模式是:"shiro_realm:roles_permissions" <br/>
 * 
 * @author Administrator
 * 
 */
public class RedisRealm extends AuthorizingRealm {
  private static Logger logger = LoggerFactory.getLogger(RedisRealm.class);

  private static final int hashIterations = 100000; //number of iterations used in the hash.  not used when validating the password, so don't change it.

  private static final String F_PASSWORD = "password";

  private static final String F_SALT = "salt";

  private static final String F_NAME = "name";

  private static final String F_ALGORITHM = "algorithm";

  private static final String F_HASHITERATIONS = "hashIterations";

  private RedisManager redisManager;

  /**
   * The Redis key prefix for the Realm
   */
  private String keyPrefix = "shiro_realm:";

  /**
   * Returns the Redis session keys prefix.
   * 
   * @return The prefix
   */
  public String getKeyPrefix() {
    return keyPrefix;
  }

  /**
   * Sets the Redis sessions key prefix.
   * 
   * @param keyPrefix
   *          The prefix
   */
  public void setKeyPrefix(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public RedisManager getRedisManager() {
    return redisManager;
  }

  public void setRedisManager(RedisManager redisManager) {
    this.redisManager = redisManager;

    // initialize the Redis manager instance
    this.redisManager.init();
  }

  protected boolean permissionsLookupEnabled = false;

  /**
   * Enables lookup of permissions during authorization. The default is "false"
   * - meaning that only roles are associated with a user. Set this to true in
   * order to lookup roles <b>and</b> permissions.
   * 
   * @param permissionsLookupEnabled
   *          true if permissions should be looked up during authorization, or
   *          false if only roles should be looked up.
   */
  public void setPermissionsLookupEnabled(boolean permissionsLookupEnabled) {
    this.permissionsLookupEnabled = permissionsLookupEnabled;
  }

  private HashedCredentialsMatcher matcher = new HashedCredentialsMatcher(Sha256Hash.ALGORITHM_NAME);
  private RandomNumberGenerator rng = new SecureRandomNumberGenerator();

  public RedisRealm() {
    matcher.setHashIterations(hashIterations);
    matcher.setStoredCredentialsHexEncoded(false);
    super.setCredentialsMatcher(matcher);
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    return token instanceof UsernamePasswordToken;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authToken) throws AuthenticationException {
    if (!(authToken instanceof UsernamePasswordToken)) {
      throw new AuthenticationException("This realm only supports UsernamePasswordTokens");
    }
    UsernamePasswordToken token = (UsernamePasswordToken) authToken;

    if (token.getUsername() == null) {
      throw new AuthenticationException("Cannot log in null user");
    }

    return findPasswordForUsername(token.getUsername());
  }

  @SuppressWarnings("unchecked")
  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    //null usernames are invalid
    if (principals == null) {
      throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
    }
    String username = (String) getAvailablePrincipal(principals);

    java.util.Set<String> roles = redisManager.smembers(keyPrefix + "user_roles:" + username);

    SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roles);

    if (permissionsLookupEnabled) {
      java.util.List<java.lang.String> permissionsList = redisManager.hmget("shiro_realm:roles_permissions", roles.toArray(new String[0]));
      Set<String> permissionsSet = new HashSet<String>(permissionsList.size());
      permissionsSet.addAll(permissionsList);
      info.setStringPermissions(permissionsSet);
    }

    return info;
  }

  /**
   * Does the actual mechanics of creating the Authentication info object from
   * the database.
   */
  public AuthenticationInfo findPasswordForUsername(String username) {
    Map<String, String> user = redisManager.hgetAll(keyPrefix + "users:" + username);

    if (user == null || user.size() == 0) {
      throw new UnknownAccountException("Unkown user " + username);
    }

    String password = user.get(F_PASSWORD);
    String salt = user.get(F_SALT);
    return new SimpleAuthenticationInfo(username, password, Sha256Hash.fromBase64String(salt), getName());
  }

  /**
   * 添加用户: 存放在hash里,key的模式是:"shiro_realm:users:$username"
   * 
   * @param username
   * @param plainTextPassword
   * @return
   */
  public boolean addUser(String username, String plainTextPassword) {
    ByteSource salt = rng.nextBytes();

    Map<String, String> user = new HashMap<String, String>(5);
    user.put(F_NAME, username);
    user.put(F_PASSWORD, new Sha256Hash(plainTextPassword, salt, hashIterations).toBase64());
    user.put(F_SALT, salt.toBase64());
    user.put(F_ALGORITHM, Sha256Hash.ALGORITHM_NAME);
    user.put(F_HASHITERATIONS, String.valueOf(hashIterations));

    if (redisManager.hmset("shiro_realm:users:" + username, user).equalsIgnoreCase("OK")) {
      return true;
    } else {
      return false;
    }
  }

  public boolean addRoles(String... roles) {
    redisManager.sadd("shiro_realm:all_roles", roles);
    return true;
  }

  public boolean addUserOwnedRoles(String username, String... roles) {
    redisManager.sadd("shiro_realm:user_roles:" + username, roles);
    return true;
  }

  public boolean addRolesPermissions(java.util.Map<String, String> rolesPermissions) {
    if (redisManager.hmset("shiro_realm:roles_permissions", rolesPermissions).equalsIgnoreCase("OK")) {
      return true;
    } else {
      return false;
    }

  }
}
