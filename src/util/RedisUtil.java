package util;
import redis.clients.jedis.*;

public class RedisUtil {

    //服务器IP地址
    private static String ADDR = "localhost";
    //端口
    private static int PORT = 6379;
    //密码
    private static String AUTH = "bitsocialgroupredis";
    //连接实例的最大连接数
    private static int MAX_ACTIVE = 1024;
    //控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
    private static int MAX_IDLE = 200;
    //等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException
    private static int MAX_WAIT = 10000;
    //连接超时的时间　　
    private static int TIMEOUT = 10000;
    // 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
    private static boolean TEST_ON_BORROW = true;
    // 未知
    private static JedisPool jedisPool = null;
    //数据库模式是16个数据库 0~15
    public static final int DEFAULT_DATABASE = 0;

    /**
     * 初始化Redis连接池
     */
    static{
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(MAX_ACTIVE);
            config.setMaxIdle(MAX_IDLE);
            config.setMaxWaitMillis(MAX_WAIT);
            config.setTestOnBorrow(TEST_ON_BORROW);
            jedisPool = new JedisPool(config, ADDR, PORT, TIMEOUT,AUTH,DEFAULT_DATABASE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取Jedis实例
     */
    public synchronized static Jedis getJedis() {

        try {
            if (jedisPool != null) {
                Jedis resource = jedisPool.getResource();
                System.out.println("redis--服务正在运行: "+resource.ping());
                return resource;
            } else {
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }


    /**
     * 关闭jedis
     * @param jedis
     */
    private void close(Jedis jedis){
        if(jedis != null){
            jedis.close();
        }
    }

    /**
     * 添加/更改/设置键值对
     * @param key
     * @param value
     * @return 返回String类型的OK代表成功
     */
    private String set(String key, String value){
        Jedis jedis = getJedis();
        assert jedis != null;
        return jedis.set(key, value);
    }

    /**
     * 获得key对应的value
     * @param key
     * @return value值
     */
    private String get(String key){
        Jedis jedis = getJedis();
        String value = null;
        assert jedis != null;
        value = jedis.get(key);
        return value;
    }

    /**
     * 删除指定的key
     * @param key
     * @return 如果是1就是说明删除成功
     */
    private Long del(String key){
        Jedis jedis = getJedis();
        assert jedis != null;
        return jedis.del(key);
    }

    /**
     * 判断key是否存在
     *
     * @param key
     * @return 存在就是true
     */
    public Boolean exists(String key) {
        Jedis jedis = getJedis();
        assert jedis != null;
        return jedis.exists(key);
    }
}
