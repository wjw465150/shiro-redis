package wjw.shiro.redis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.shiro.ShiroException;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.util.Destroyable;
import org.apache.shiro.util.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisCacheManager implements CacheManager, Initializable, Destroyable {

  private static final Logger logger = LoggerFactory.getLogger(RedisCacheManager.class);

  // fast lookup by name map
  @SuppressWarnings("rawtypes")
  private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();

  private RedisManager redisManager;

  @Override
  public void init() throws ShiroException {
    /** Nothing to do here */
  }

  @Override
  public void destroy() throws Exception {
    if (!caches.isEmpty()) {
      logger.debug("Shutting down all RedisCache.");
      caches.clear();
      redisManager.destroy();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> Cache<K, V> getCache(String name) throws CacheException {
    logger.debug("getCache() RedisCache, name:" + name);

    Cache<K, V> c = caches.get(name);

    if (c == null) {
      logger.debug("getCache() RedisCache, name:" + name + ",does not yet exist.  Creating now.", name);

      // create a new cache instance
      c = new RedisCache<K, V>(redisManager);

      // add it to the cache collection
      caches.put(name, c);
    }
    return c;
  }

  public RedisManager getRedisManager() {
    return redisManager;
  }

  public void setRedisManager(RedisManager redisManager) {
    this.redisManager = redisManager;

    // initialize the Redis manager instance
    redisManager.init();
  }

}