package wjw.shiro.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wjw.kryo.wrapper.KryoSerializer;

public class SerializeUtils {

  private static Logger logger = LoggerFactory.getLogger(SerializeUtils.class);

  /**
   * 反序列化
   * 
   * @param bytes
   * @return
   */
  public static Object deserialize(byte[] bytes) {
    Object result = null;

    if (isEmpty(bytes)) {
      return null;
    }

    try {
      result = KryoSerializer.read(bytes);
    } catch (Exception e) {
      logger.error("Failed to deserialize", e);
    }
    return result;
  }

  /**
   * 序列化
   * 
   * @param object
   * @return
   */
  public static byte[] serialize(Object object) {
    byte[] result = null;

    if (object == null) {
      return new byte[0];
    }

    try {
      result = KryoSerializer.write(object);
    } catch (Exception ex) {
      logger.error("Failed to serialize", ex);
    }
    return result;
  }

  public static boolean isEmpty(byte[] data) {
    return (data == null || data.length == 0);
  }

}