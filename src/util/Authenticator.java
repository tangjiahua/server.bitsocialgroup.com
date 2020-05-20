package util;

import net.sf.json.JSONObject;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

public class Authenticator {

    private static String JDBC_DRIVER = Sql.GLOBAL_JDBC_DRIVER;
    private static String DB_URL = Sql.GLOBAL_DB_URL;
    private static String DB_NAME_SG = Sql.GLOBAL_DB_NAME_SG;
    private static String DB_NAME_SGS = Sql.GLOBAL_DB_NAME_SGS;
    private static String USER = Sql.GLOBAL_USER;
    private static String PASS = Sql.GLOBAL_PASS;


    public static Boolean authenticate(String user_id, String password) throws SQLException, ClassNotFoundException {
        return privateAuthenticate(user_id, password);
    }

    public static Boolean deleteAuthenticate(String user_id, String password)throws SQLException, ClassNotFoundException {
        return privateDeleteAutenticate(user_id, password);
    }


    private static Boolean privateDeleteAutenticate(String user_id, String password)throws SQLException, ClassNotFoundException {
        Jedis jedis = RedisUtil.getJedis();

        assert jedis != null;
        if(jedis.exists(user_id)){
            jedis.del(user_id);
            jedis.close();
        }
        return true;
    }

    private static Boolean privateAuthenticate(String user_id, String password) throws ClassNotFoundException, SQLException {

        Jedis jedis = RedisUtil.getJedis();

        assert jedis != null;
        if(jedis.exists(user_id)){
            String password_redis = jedis.get(user_id);

            String passwordMD5 = Util.stringToMD5(password);
            if(passwordMD5.equals(password_redis)){
                //正确
                jedis.close();
                return true;
            }
        }
        //如果redis中不存在，或者是密码对不上，那么都要去数据库看一下最后的数据


        String sql = "SELECT * FROM user WHERE user_id = ? LIMIT 1";
        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SGS, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        PreparedStatement stmt = conn.prepareStatement(sql);

        //设置参数
        stmt.setInt(1, Integer.parseInt(user_id));

        //执行
        ResultSet rs = stmt.executeQuery();

        if(rs.next()){
            // 代表有结果
            String pwdMD5DB = rs.getString("password");
            String passwordMD5 = Util.stringToMD5(password);
            if(pwdMD5DB.equals(passwordMD5)){
                //数据库中的结果是密码匹配，而redis中的结果是密码不匹配
                jedis.set(user_id, pwdMD5DB);
                jedis.close();
                //关闭链接
                stmt.close();
                conn.close();
                rs.close();

                return true;
            }
        }
        jedis.close();
        //关闭链接
        stmt.close();
        conn.close();
        rs.close();
        return false;
    }
}
