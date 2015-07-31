package wjw.shiro.redis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisCache<K, V> implements Cache<K, V> {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * The wrapped Jedis instance.
   */
  private RedisManager redisManager;

  /**
   * The Redis key prefix for the cache
   */
  private String keyPrefix = "shiro_cache:";

  //所有cache的key(目的是为了快速遍历!), 存放在set里,key的模式是:"shiro_realm:all_caches" <br/>
  private String all_caches_Key = "shiro_cache:all_caches";

  /**
   * 通过一个JedisManager实例构造RedisCache
   */
  public RedisCache(RedisManager redisManager) {
    if (redisManager == null) {
      throw new IllegalArgumentException("redisManager argument cannot be null.");
    }
    this.redisManager = redisManager;
  }

  /**
   * Constructs a cache instance with the specified Redis manager and using a
   * custom key prefix.
   * 
   * @param cache
   *          The redisManager instance
   * @param prefix
   *          The Redis key prefix
   */
  public RedisCache(RedisManager cache, String prefix) {
    this(cache);

    // set the prefix
    this.keyPrefix = prefix;
  }

  @Override
  public V get(K key) throws CacheException {
    logger.debug("get(K key) from redis:key [" + key + "]");
    try {
      if (key == null) {
        return null;
      } else {
        byte[] rawValue = redisManager.get(getByteKey(key));
        @SuppressWarnings("unchecked")
        V value = (V) SerializeUtils.deserialize(rawValue);
        return value;
      }
    } catch (Throwable t) {
      throw new CacheException(t);
    }

  }

  @Override
  public V put(K key, V value) throws CacheException {
    logger.debug("put(K key, V value) to redis:key [" + key + "]");
    try {
      this.redisManager.set(getByteKey(key), SerializeUtils.serialize(value));

      this.redisManager.sadd(all_caches_Key, key.toString());

      return value;
    } catch (Throwable t) {
      throw new CacheException(t);
    }
  }

  @Override
  public V remove(K key) throws CacheException {
    logger.debug("remove(K key) from redis: key [" + key + "]");
    try {
      V previous = this.get(key);

      this.redisManager.del(getByteKey(key));

      this.redisManager.srem(all_caches_Key, key.toString());

      return previous;
    } catch (Throwable t) {
      throw new CacheException(t);
    }
  }

  @Override
  public void clear() throws CacheException {
    logger.debug("Cache clear() from redis");
    try {
      java.util.Set<String> cacheKeys = this.redisManager.smembers(all_caches_Key);
      for (String key : cacheKeys) {
        this.redisManager.del((this.keyPrefix + key).getBytes());
      }

      this.redisManager.delStr(all_caches_Key);
    } catch (Throwable t) {
      throw new CacheException(t);
    }
  }

  @Override
  public int size() {
    try {
      return redisManager.scard(all_caches_Key).intValue();
    } catch (Throwable t) {
      throw new CacheException(t);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<K> keys() {
    try {
      java.util.Set<String> keys = this.redisManager.smembers(all_caches_Key);

      if (CollectionUtils.isEmpty(keys)) {
        return Collections.emptySet();
      } else {
        Set<K> newKeys = new HashSet<K>();
        for (String key : keys) {
          newKeys.add((K) key);
        }
        return newKeys;
      }
    } catch (Throwable t) {
      throw new CacheException(t);
    }
  }

  @Override
  public Collection<V> values() {
    try {
      java.util.Set<String> keys = this.redisManager.smembers(all_caches_Key);

      if (!CollectionUtils.isEmpty(keys)) {
        List<V> values = new ArrayList<V>(keys.size());
        for (String key : keys) {
          @SuppressWarnings("unchecked")
          V value = get((K) key);
          if (value != null) {
            values.add(value);
          }
        }
        return Collections.unmodifiableList(values);
      } else {
        return Collections.emptyList();
      }
    } catch (Throwable t) {
      throw new CacheException(t);
    }
  }

  /**
   * 获得byte[]型的key
   * 
   * @param key
   * @return
   */
  private byte[] getByteKey(K key) {
    if (key instanceof String) {
      String preKey = this.keyPrefix + key;
      return preKey.getBytes();
    } else {
      return SerializeUtils.serialize(key);
    }
  }

  /**
   * Returns the Redis cache keys prefix.
   * 
   * @return The prefix
   */
  public String getKeyPrefix() {
    return keyPrefix;
  }

  /**
   * Sets the Redis cache key prefix.
   * 
   * @param keyPrefix
   *          The prefix
   */
  public void setKeyPrefix(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

}
