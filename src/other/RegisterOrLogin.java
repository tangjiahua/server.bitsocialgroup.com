package other;

import net.sf.json.JSONObject;
import redis.clients.jedis.Jedis;
import util.*;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;

@WebServlet("/register_or_login")
public class RegisterOrLogin extends  HttpServlet {

    private static String JDBC_DRIVER = Sql.GLOBAL_JDBC_DRIVER;
    private static String DB_URL = Sql.GLOBAL_DB_URL;
    private static String DB_NAME_SG = Sql.GLOBAL_DB_NAME_SG;
    private static String DB_NAME_SGS = Sql.GLOBAL_DB_NAME_SGS;
    private static String USER = Sql.GLOBAL_USER;
    private static String PASS = Sql.GLOBAL_PASS;


    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        this.doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        Connection conn = null;


        JSONObject jsonObject = Util.requestToJsonObject(request);

        String method = (String)jsonObject.get("method");
        String account = (String)jsonObject.get("account");
        String password = (String)jsonObject.get("password");
        String device_type = null;


        //判断所有字符串是否符合规范

        PreparedStatement stmt;
        if(method.equals("register")) {
            // 注册
            // 判断account是否符合规范
            if(TestInfo.testAccount(account)){
                // 判断密码格式
                if(TestInfo.testPassword(password)){
                    // 查找数据库中是否有
                    try {

                        device_type = (String)jsonObject.get("device_type");
                        String sql = "SELECT * FROM user WHERE account = ? limit 1";
                        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                        conn = DriverManager.getConnection(DB_URL + DB_NAME_SGS, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                        stmt = conn.prepareStatement(sql);
                        stmt.setString(1, account);
                        ResultSet rs = stmt.executeQuery();

                        if(rs.next()){
                            // 代表有结果，意味着已被注册
                            Response.responseInfo(response, "账号已经被注册");

                        }else{

                            String insert_sql = "INSERT INTO user(account, password, device_type) VALUES" +
                                    "(?, ?, ?);";
                            stmt = conn.prepareStatement(insert_sql);
                            String passwordMD5 = Util.stringToMD5(password);
                            stmt.setString(1, account);
                            stmt.setString(2, passwordMD5);
                            stmt.setString(3, device_type);

                            int result = stmt.executeUpdate();

                            if(result == 1){
                                Response.responseSuccessInfo(response, "成功注册");
                            }
                        }
                        //Clean-up environment
                        rs.close();
                        stmt.close();
                        conn.close();

                    } catch (ClassNotFoundException | SQLException e) {
                        Response.responseError(response, e.toString());
                    }

                }else{
                    Response.responseError(response, "密码格式不对");
                }
            }else{
                Response.responseError(response, "账号格式不对");
            }
        }else if(method.equals("login")){
            // 登录
            //检查是否正确
            if(TestInfo.testAccount(account)){
                if(TestInfo.testPassword(password)){
                    //查找数据库是否有
                    try {

                        String sql = "SELECT * FROM user WHERE account = ? LIMIT 1";

                        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                        conn = DriverManager.getConnection(DB_URL + DB_NAME_SGS, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                        stmt = conn.prepareStatement(sql);

                        stmt.setString(1, account);

                        ResultSet rs = stmt.executeQuery();

                        if(rs.next()){
                            // 有结果，那么去判断一下密码是否正确
                            String pwdMD5DB = rs.getString("password");
                            String user_id = rs.getString("user_id");

                            String passwordMD5 = Util.stringToMD5(password);

                            if(pwdMD5DB.equals(passwordMD5)){
                                // 登录成功
                                // 先在redis上set一下
                                Jedis jedis = RedisUtil.getJedis();
                                assert jedis != null;
                                jedis.set(user_id, passwordMD5);
                                jedis.close();

                                //返回结果
                                JSONObject jsonobj = new JSONObject();
                                jsonobj.put("user_id", user_id);

                                Response.responseSuccessInfo(response, jsonobj);

                            }else{
                                // 密码错误
                                Response.responseInfo(response, "账号或密码错误");
                            }
                        }else{
                            // 没有结果，也就是没有该账号
                            Response.responseInfo(response, "账号或密码错误");
                        }

                        //Clean-up environment
                        rs.close();
                        stmt.close();
                        conn.close();

                    } catch (ClassNotFoundException | SQLException e) {
                        Response.responseError(response, e.toString());
                    }
                }else{
                    Response.responseError(response, "密码格式不对");
                }
            }else{
                Response.responseError(response, "账号格式不对");
            }

        }else{
            //method参数不正确
            Response.responseError(response, "传入的参数method不对");
        }
    }
}

