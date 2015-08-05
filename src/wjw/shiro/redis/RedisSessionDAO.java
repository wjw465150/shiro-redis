package wjw.shiro.redis;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisSessionDAO extends CachingSessionDAO {
  private static Logger logger = LoggerFactory.getLogger(RedisSessionDAO.class);

  private RedisManager redisManager;

  /**
   * The Redis key prefix for the sessions
   */
  private String keyPrefix = RedisManager.DEFAULT_ROOTKEY + "session:";

  //所有session的key(目的是为了快速遍历!), 存放在set里,key的模式是:keyPrefix+"all_sessions" <br/>
  private String all_sessions_Key = keyPrefix + "all_sessions";

  @Override
  protected void doDelete(Session session) {
    if (session == null || session.getId() == null) {
      logger.error("session or session id is null");
      return;
    }
    redisManager.del(this.getByteKey(session.getId()));

    redisManager.srem(all_sessions_Key, session.getId().toString());
  }

  @Override
  protected void doUpdate(Session session) {
    this.saveSession(session);
  }

  @Override
  public Collection<Session> getActiveSessions() {
    super.getActiveSessions(); //调用父类的此方法的目的是:强制清理一下无用的Cache!

    Collection<Session> sessions = new HashSet<Session>();
    Set<String> keys = redisManager.smembers(all_sessions_Key);
    if (keys != null && keys.size() > 0) {
      for (String key : keys) {
        byte[] rawValue = redisManager.get((this.keyPrefix + key).getBytes());
        if (rawValue == null) {
          redisManager.srem(all_sessions_Key, key);
        } else {
          Session s = (Session) SerializeUtils.deserialize(rawValue);
          sessions.add(s);
        }
      }
    }

    return sessions;
  }

  @Override
  protected Serializable doCreate(Session session) {
    Serializable sessionId = this.generateSessionId(session);
    this.assignSessionId(session, sessionId);

    this.saveSession(session);

    return sessionId;
  }

  @Override
  protected Session doReadSession(Serializable sessionId) {
    if (sessionId == null) {
      logger.error("session id is null");
      return null;
    }

    Session s = (Session) SerializeUtils.deserialize(redisManager.get(this.getByteKey(sessionId)));
    return s;
  }

  /**
   * save session
   * 
   * @param session
   * @throws UnknownSessionException
   */
  private void saveSession(Session session) throws UnknownSessionException {
    if (session == null || session.getId() == null) {
      logger.error("session or session id is null");
      return;
    }

    byte[] key = getByteKey(session.getId());
    byte[] value = SerializeUtils.serialize(session);
    session.setTimeout(redisManager.getExpire() * 1000);
    this.redisManager.set(key, value, redisManager.getExpire());

    this.redisManager.sadd(all_sessions_Key, session.getId().toString());
  }

  /**
   * 获得byte[]型的key
   * 
   * @param key
   * @return
   */
  private byte[] getByteKey(Serializable sessionId) {
    String preKey = this.keyPrefix + sessionId;
    return preKey.getBytes();
  }

  public RedisManager getRedisManager() {
    return redisManager;
  }

  public void setRedisManager(RedisManager redisManager) {
    if (redisManager == null) {
      throw new IllegalArgumentException("redisManager argument cannot be null.");
    }
    this.redisManager = redisManager;

    // initialize the Redis manager instance
    this.redisManager.init();

    this.keyPrefix = this.redisManager.rootKey + "session:";
    this.all_sessions_Key = this.keyPrefix + "all_sessions";
  }

}