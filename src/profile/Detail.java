package profile;

import net.sf.json.JSONObject;
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

@WebServlet("/profile/detail")
public class Detail extends  HttpServlet {

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
            throws ServletException, IOException {


        JSONObject jsonObject = Util.requestToJsonObject(request);

        String method = (String)jsonObject.get("method");
        String socialgroup_id = (String)jsonObject.get("socialgroup_id");
        String user_id = (String)jsonObject.get("user_id");
        String password = (String)jsonObject.get("password");

        // 检查参数是否传入正确
        if(!TestInfo.testUser_id(user_id) || !TestInfo.testPassword(password) || !TestInfo.testSocialgroupId(socialgroup_id)){
            Response.responseError(response, "Detail.java user_id || pwd || socialgoupid wrong");
            return;
        }

        // 验证账号密码是否匹配
        try {
            if(Authenticator.authenticate(user_id, password)){
                // 身份验证成功 可以处理

                if(method.equals("2")){
                    // 查看的是自己的个人资料
                    // 所以不需要another_user_id这样的东西
                    String sql = "SELECT * FROM user_profile WHERE user_id = ?;";

                    Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                    Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                    PreparedStatement stmt = conn.prepareStatement(sql);

                    //设置参数
                    stmt.setInt(1, Integer.parseInt(user_id));

                    ResultSet rs = stmt.executeQuery();

                    getFullProfile(response, user_id, rs);

                    rs.close();
                    stmt.close();
                    conn.close();

                }else if(method.equals("1")){
                    // 查看别人的资料
                    // 第一种情况，你们之间没有互戳过
                    String another_user_id = (String)jsonObject.get("another_user_id");

                    String sql = "SELECT * FROM stick WHERE stick_from_user_id = ? AND " +
                            "stick_to_user_id = ? LIMIT 1;";

                    Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                    Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                    PreparedStatement stmt = conn.prepareStatement(sql);

                    //设置参数
                    stmt.setInt(1, Integer.parseInt(another_user_id));
                    stmt.setInt(2, Integer.parseInt(user_id));

                    ResultSet rs = stmt.executeQuery();

                    if(rs.next()){
                        //有结果，戳过你
                        String sql_tmp = "SELECT * FROM user_profile WHERE user_id = ?;";

                        stmt = conn.prepareStatement(sql_tmp);

                        stmt.setInt(1, Integer.parseInt(another_user_id));

                        rs = stmt.executeQuery();

                        getFullProfile(response, user_id, rs);
                        rs.close();
                        stmt.close();
                        conn.close();

                    }else{
                        //无结果，没有戳过你
                        String sql_tmp = "SELECT * FROM user_profile WHERE user_id = ?;";

                        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                        conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                        stmt = conn.prepareStatement(sql_tmp);

                        //设置参数
                        stmt.setInt(1, Integer.parseInt(another_user_id));

                        rs = stmt.executeQuery();

                        if(rs.next()){
                            JSONObject jsonObj = new JSONObject();
                            jsonObj.put("user_id", user_id);
                            jsonObj.put("nickname", rs.getString("nickname"));
                            jsonObj.put("realname", rs.getString("realname"));
                            jsonObj.put("gender", rs.getString("gender"));
                            jsonObj.put("age", rs.getString("age"));
                            jsonObj.put("avatar", rs.getString("avatar"));
                            jsonObj.put("background", rs.getString("background"));
                            jsonObj.put("stick_count", rs.getString("stick_count"));
                            jsonObj.put("wall_picture_count", rs.getString("wall_picture_count"));
                            jsonObj.put("public_introduce", rs.getString("public_introduce"));
    //                        jsonObj.put("private_introduce", rs.getString("private_introduce"));
                            jsonObj.put("grade", rs.getString("grade"));
                            jsonObj.put("hometown", rs.getString("hometown"));
                            jsonObj.put("major", rs.getString("major"));
                            jsonObj.put("relationship_status", rs.getString("relationship_status"));
                            jsonObj.put("role", rs.getString("role"));

                            Response.responseSuccessInfo(response, jsonObj);

                        }else{
                            Response.responseError(response, "该用户不在该社群中");
                        }

                        rs.close();
                        stmt.close();
                        conn.close();
                    }

                }

            }else{
                Response.responseAuthenError(response);
            }
        } catch (SQLException | ClassNotFoundException e) {
            Response.responseError(response, e.toString());
        }

    }

    private void getFullProfile(HttpServletResponse response, String user_id, ResultSet rs) throws SQLException {
        if(rs.next()){
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("user_id", user_id);
            jsonObj.put("nickname", rs.getString("nickname"));
            jsonObj.put("realname", rs.getString("realname"));
            jsonObj.put("gender", rs.getString("gender"));
            jsonObj.put("age", rs.getString("age"));
            jsonObj.put("avatar", rs.getString("avatar"));
            jsonObj.put("background", rs.getString("background"));
            jsonObj.put("stick_count", rs.getString("stick_count"));
            jsonObj.put("wall_picture_count", rs.getString("wall_picture_count"));
            jsonObj.put("public_introduce", rs.getString("public_introduce"));
            jsonObj.put("private_introduce", rs.getString("private_introduce"));
            jsonObj.put("grade", rs.getString("grade"));
            jsonObj.put("hometown", rs.getString("hometown"));
            jsonObj.put("major", rs.getString("major"));
            jsonObj.put("relationship_status", rs.getString("relationship_status"));
            jsonObj.put("role", rs.getString("role"));

            Response.responseSuccessInfo(response, jsonObj);

        }else{
            Response.responseError(response, "该用户不在该社群中");
        }
    }
}
