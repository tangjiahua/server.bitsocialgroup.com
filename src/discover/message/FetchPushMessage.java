package discover.message;


import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

@WebServlet("/discover/fetch_push_message")
public class FetchPushMessage extends HttpServlet {

    private static String JDBC_DRIVER = Sql.GLOBAL_JDBC_DRIVER;
    private static String DB_URL = Sql.GLOBAL_DB_URL;
    private static String DB_NAME_SG = Sql.GLOBAL_DB_NAME_SG;
    private static String DB_NAME_SGS = Sql.GLOBAL_DB_NAME_SGS;
    private static String USER = Sql.GLOBAL_USER;
    private static String PASS = Sql.GLOBAL_PASS;




    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String newest_push_message_id = null;
        String socialgroup_id = null;
        String user_id = null;
        String password = null;

        JSONObject jsonObject = Util.requestToJsonObject(req);

        newest_push_message_id = jsonObject.getString("newest_push_message_id");
        socialgroup_id = jsonObject.getString("socialgroup_id");
        user_id = jsonObject.getString("user_id");
        password = jsonObject.getString("password");

        try {
            if(TestInfo.testSocialgroupId(socialgroup_id) && TestInfo.testUser_id(user_id) && TestInfo.testPassword(password)
                    && Authenticator.authenticate(user_id, password)){
                // 身份验证成功
                fetchPushMessage(resp, socialgroup_id, newest_push_message_id, user_id);
            }else{
                Response.responseError(resp, "fetchPushhMesage.java 身份验证失败");
            }
        } catch (SQLException | ClassNotFoundException e) {
            Response.responseError(resp, "fetchPushhMesage.java: " + e.toString());
        }
    }


    private void fetchPushMessage(HttpServletResponse response, String socialgroup_id, String newest_push_message_id, String user_id) throws SQLException, ClassNotFoundException {
        if(newest_push_message_id.equals("0")){
            // 下载应用后第一次拉取
            String sql = "select max(push_message_id) as push_message_id_new from push_message";
            fetchPushMessageFirstSql(response, sql, socialgroup_id);
        }else{
            String sql = "SELECT * FROM push_message WHERE push_message_id > ? AND user_id = ?";
            fetchPushMessageSql(response, sql, socialgroup_id, user_id, newest_push_message_id);
        }
    }


    /*
        第一次拉取
     */
    private void fetchPushMessageFirstSql(HttpServletResponse response, String sql, String socialgroup_id) throws SQLException, ClassNotFoundException {
        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
        JSONObject json_result = new JSONObject();// 最终的返回的结果
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        PreparedStatement  stmt = conn.prepareStatement(sql);

        // 设置参数

        ResultSet rs = stmt.executeQuery();
        JSONArray json_array = new JSONArray();

        while(rs.next()){
            json_result.put("push_message_id_new", rs.getString("push_message_id_new"));

        }

        json_result.put("push_message", json_array);    // 显然，push_message对应的数组的空的

        Response.responseSuccessInfo(response, json_result);
    }



    /*
        不是第一次拉取
     */
    private void fetchPushMessageSql(HttpServletResponse response, String sql, String socialgroup_id, String user_id, String newest_push_message_id) throws SQLException, ClassNotFoundException {
        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
        JSONObject json_result = new JSONObject();// 最终的返回的结果
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        // 首先 单独接收查找最大值的函数
        String sql_max_message_id = "select max(push_message_id) as push_message_id_new from push_message";
        PreparedStatement  stmt = conn.prepareStatement(sql_max_message_id);

        // 设置参数

        ResultSet rs_message_id = stmt.executeQuery();

        while(rs_message_id.next()){
            json_result.put("push_message_id_new", rs_message_id.getString("push_message_id_new"));

        }



        stmt = conn.prepareStatement(sql);
        // 设置参数
        stmt.setInt(1, Integer.valueOf(newest_push_message_id));
        stmt.setInt(2, Integer.valueOf(user_id));

        ResultSet rs = stmt.executeQuery();
        JSONArray json_array = new JSONArray();

        while(rs.next()){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("push_message_id", rs.getString("push_message_id"));
            jsonObject.put("type", rs.getString("type"));
            jsonObject.put("content", rs.getString("content"));
            jsonObject.put("create_date", rs.getString("create_date"));

            json_array.add(jsonObject);

        }

        json_result.put("push_message", json_array);    // 显然，push_message对应的数组的空的

        Response.responseSuccessInfo(response, json_result);

    }



}