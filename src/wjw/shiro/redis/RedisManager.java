package wjw.shiro.redis;

import java.util.Collection;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

/**
 * 
 * @author Administrator
 */
public class RedisManager {
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  static final String TOMCAT_SESSION_PREFIX = "TS:";
  static ShardedJedisPool _shardedPool = null;
  static JedisPool _pool = null;
  //->---------------属性----------------------

  protected String serverlist = "127.0.0.1:6379"; //用逗号(,)分隔的"ip:port"列表

  /**
   * the list of all cache servers;用逗号(,)分隔的"ip:port"列表
   * 
   * @return
   */
  public String getServerlist() {
    return serverlist;
  }

  /**
   * the list of all cache servers;用逗号(,)分隔的"ip:port"列表
   * 
   * @param serverlist
   */
  public void setServerlist(String serverlist) {
    this.serverlist = serverlist;
  }

  protected int minConn = 5;

  /**
   * Returns the minimum number of spare connections in available pool.
   * 
   * @return number of connections
   */
  public int getMinConn() {
    return minConn;
  }

  /**
   * Sets the minimum number of spare connections to maintain in our available
   * pool.
   * 
   * @param minConn
   *          - number of connections
   */
  public void setMinConn(int minConn) {
    this.minConn = minConn;
  }

  protected int maxConn = 100;

  /**
   * Returns the maximum number of spare connections allowed in available pool.
   * 
   * @return number of connections
   */
  public int getMaxConn() {
    return maxConn;
  }

  /**
   * Sets the maximum number of spare connections allowed in our available pool.
   * 
   * @param maxConn
   *          - number of connections
   */
  public void setMaxConn(int maxConn) {
    this.maxConn = maxConn;
  }

  protected int socketTO = 6000;

  /**
   * Returns the socket timeout for reads.
   * 
   * @return the socketTO timeout in ms
   */
  public int getSocketTO() {
    return socketTO;
  }

  /**
   * Sets the socket timeout for reads.
   * 
   * @param socketTO
   *          timeout in ms
   */
  public void setSocketTO(int socketTO) {
    this.socketTO = socketTO;
  }

  //0 - never expire
  protected int expire = 0;

  /**
   * @return the expire
   */
  public int getExpire() {
    return expire;
  }

  /**
   * @param expire
   *          the expire to set
   */
  public void setExpire(int expire) {
    this.expire = expire;
  }

  public static final String DEFAULT_ROOTKEY = "shiro:";
  //存放shiro数据的根key,必须以":"结尾!
  String rootKey = DEFAULT_ROOTKEY;

  public String getRootKey() {
    return rootKey;
  }

  public void setRootKey(String rootKey) {
    if (rootKey.endsWith(":") == false) {
      throw new IllegalArgumentException("rootKey must end with ':'!");
    }
    this.rootKey = rootKey;
  }

  //<----------------属性----------------------

  public RedisManager() {
    super();
  }

  /**
   * 初始化方法
   */
  public void init() {
    synchronized (RedisManager.class) {
      try {
        if (_shardedPool == null && _pool == null) {
          try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxActive(maxConn);
            poolConfig.setMinIdle(minConn);
            int maxIdle = poolConfig.minIdle + 5;
            if (maxIdle > poolConfig.maxActive) {
              maxIdle = poolConfig.maxActive;
            }
            poolConfig.setMaxIdle(maxIdle);
            poolConfig.setMaxWait(1000L);
            poolConfig.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
            poolConfig.setTestOnBorrow(false);
            poolConfig.setTestOnReturn(false);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMinEvictableIdleTimeMillis(1000L * 60L * 10L); //空闲对象,空闲多长时间会被驱逐出池里
            poolConfig.setTimeBetweenEvictionRunsMillis(1000L * 30L); //驱逐线程30秒执行一次
            poolConfig.setNumTestsPerEvictionRun(-1); //-1,表示在驱逐线程执行时,测试所有的空闲对象

            String[] servers = serverlist.split(",");
            java.util.List<JedisShardInfo> shards = new java.util.ArrayList<JedisShardInfo>(servers.length);
            for (int i = 0; i < servers.length; i++) {
              String[] hostAndPort = servers[i].split(":");
              JedisShardInfo shardInfo = new JedisShardInfo(hostAndPort[0], Integer.parseInt(hostAndPort[1]), socketTO);
              if (hostAndPort.length == 3) {
                shardInfo.setPassword(hostAndPort[2]);
              }
              shards.add(shardInfo);
            }

            if (shards.size() == 1) {
              _pool = new JedisPool(poolConfig, shards.get(0).getHost(), shards.get(0).getPort(), shards.get(0).getTimeout(), shards.get(0).getPassword());
              log.info("使用:JedisPool");
            } else {
              _shardedPool = new ShardedJedisPool(poolConfig, shards);
              log.info("使用:ShardedJedisPool");
            }

            log.info("RedisShards:" + shards.toString());
            log.info("初始化RedisManager:" + this.toString());
          } catch (Exception ex) {
            log.error("error:", ex);
          }
        }

      } catch (Exception ex) {
        log.error("error:", ex);
      }
    }
  }

  public void destroy() {
    try {
      synchronized (RedisManager.class) {
        if (_shardedPool != null) {
          ShardedJedisPool myPool = _shardedPool;
          _shardedPool = null;
          try {
            myPool.destroy();
            log.info("销毁RedisManager:" + this.toString());
          } catch (Exception ex) {
            log.error("error:", ex);
          }

        }

        if (_pool != null) {
          JedisPool myPool = _pool;
          _pool = null;
          try {
            myPool.destroy();
            log.info("销毁RedisManager:" + this.toString());
          } catch (Exception ex) {
            log.error("error:", ex);
          }

        }
      }
    } finally {
      //
    }
  }

  @Override
  public String toString() {
    return "RedisManager{" + "rootKey=" + rootKey + ",expire=" + expire + ",serverlist=" + serverlist + ",minConn=" + minConn + ",maxConn=" + maxConn + ",socketTO=" + socketTO + '}';
  }

  /**
   * get value from redis
   * 
   * @param key
   * @return
   */
  public byte[] get(byte[] key) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.get(key);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.get(key);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  public String getStr(String key) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.get(key);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.get(key);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  /**
   * set
   * 
   * @param key
   * @param value
   * @return
   */
  public byte[] set(byte[] key, byte[] value) {
    return this.set(key, value, expire);
  }

  /**
   * set
   * 
   * @param key
   * @param value
   * @param expire
   * @return
   */
  public byte[] set(byte[] key, byte[] value, int expire) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        if (this.expire == 0) {
          jedis.set(key, value);
        } else {
          jedis.setex(key, expire, value);
        }
        return value;
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        if (this.expire == 0) {
          jedis.set(key, value);
        } else {
          jedis.setex(key, expire, value);
        }
        return value;
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  public String setStr(String key, String value) {
    return this.setStr(key, value, expire);
  }

  public String setStr(String key, String value, int expire) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        if (this.expire == 0) {
          jedis.set(key, value);
        } else {
          jedis.setex(key, expire, value);
        }

        return value;
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        if (this.expire == 0) {
          jedis.set(key, value);
        } else {
          jedis.setex(key, expire, value);
        }

        return value;
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  /**
   * del
   * 
   * @param key
   */
  public void del(byte[] key) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        jedis.del(key);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        Jedis jedisA = jedis.getShard(key);
        jedisA.del(key);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  /**
   * size
   */
  public long dbSize() {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.dbSize();
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      long dbSize = 0;
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        Collection<Jedis> jedisList = jedis.getAllShards();
        for (Jedis item : jedisList) {
          dbSize = dbSize + item.dbSize();
        }

        return dbSize;
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  /**
   * keys
   * 
   * @param regex
   * @return
   */
  //  public Set<byte[]> keys(String pattern) {
  //    if (_pool != null) {
  //      Jedis jedis = null;
  //      try {
  //        jedis = _pool.getResource();
  //        return jedis.keys(pattern.getBytes());
  //      } finally {
  //        if (jedis != null) {
  //          try {
  //            _pool.returnResource(jedis);
  //          } catch (Throwable thex) {
  //          }
  //        }
  //      }
  //    } else {
  //      Set<byte[]> keys = new HashSet<byte[]>();
  //      ShardedJedis jedis = null;
  //      try {
  //        jedis = _shardedPool.getResource();
  //        Collection<Jedis> jedisList =
  //            jedis.getAllShards();
  //        for (Jedis item : jedisList) {
  //          keys.addAll(item.keys(pattern.getBytes()));
  //        }
  //
  //        return keys;
  //      } finally {
  //        if (jedis != null) {
  //          try {
  //            _shardedPool.returnResource(jedis);
  //          } catch (Throwable thex) {
  //          }
  //        }
  //      }
  //    }
  //  }

  public java.util.Map<String, String> hgetAll(String key) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.hgetAll(key);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.hgetAll(key);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  public java.util.List<java.lang.String> hmget(String key, String... fields) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.hmget(key, fields);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.hmget(key, fields);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  //Return OK or Exception if hash is empty
  public String hmset(String key, java.util.Map<String, String> hash) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.hmset(key, hash);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.hmset(key, hash);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  public String hget(String key, String field) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.hget(key, field);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.hget(key, field);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  //If the field already exists, and the HSET just produced an update of the value, 0 is returned, otherwise if a new field is created 1 is returned.
  public java.lang.Long hset(String key, String field, String value) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.hset(key, field, value);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.hset(key, field, value);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  public Long hdel(String hkey, String field) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.hdel(hkey, field);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.hdel(hkey, field);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  public java.lang.Long sadd(String key, String... members) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.sadd(key, members);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.sadd(key, members);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  public Long srem(String key, String... members) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.srem(key, members);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.srem(key, members);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  public Long scard(String key) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.scard(key);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.scard(key);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  public java.util.Set<String> smembers(String key) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.smembers(key);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.smembers(key);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  /**
   * del
   * 
   * @param key
   */
  public void delStr(String key) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        jedis.del(key);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        Jedis jedisA = jedis.getShard(key);
        jedisA.del(key);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  public boolean exists(byte[] key) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.exists(key);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.exists(key);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

}