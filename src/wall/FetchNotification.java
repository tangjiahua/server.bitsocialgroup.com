package wall;

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

@WebServlet("/wall/fetch")
public class FetchNotification extends HttpServlet {

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

        String socialgroup_id = null;
        String method = null;
        String notification_id = null;

        String user_id = null;
        String password = null;

        JSONObject jsonObject = Util.requestToJsonObject(req);

        socialgroup_id = jsonObject.getString("socialgroup_id");
        method = jsonObject.getString("method");
        notification_id = jsonObject.getString("notification_id");


        user_id = jsonObject.getString("user_id");
        password = jsonObject.getString("password");

        try {
            if(TestInfo.testSocialgroupId(socialgroup_id) && TestInfo.testUser_id(user_id) && TestInfo.testPassword(password)
            && Authenticator.authenticate(user_id, password)){
                // 身份验证成功
                fetchNotification(resp,method,  socialgroup_id, notification_id);
            }else{
                Response.responseError(resp, "fetchnotification.java 身份验证失败");
            }
        } catch (SQLException | ClassNotFoundException e) {
            Response.responseError(resp, "fetch.java: " + e.toString());
        }
    }

    private void fetchNotification(HttpServletResponse response, String method, String socialgroup_id, String notification_id) throws ClassNotFoundException, SQLException {
        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信

        if(method.equals("1")){
            fetchNewNotification(response, socialgroup_id, notification_id);
        }else if(method.equals("2")){
            fetchOldNotification(response, socialgroup_id, notification_id);
        }else{
            Response.responseError(response, "fetch.java: 没有这样的method");
        }
    }

    private void fetchNewNotification(HttpServletResponse response, String socialgroup_id, String notification_id) throws SQLException {

        String sql = "SELECT notification.notification_id, user_id, user_nickname, user_avatar, type, create_date, brief, welcome, " +
                "hold_date, hold_location, holder, detail, link FROM notification, notification_poster " +
                "WHERE notification.notification_id = notification_poster.notification_id " +
                "AND notification.notification_id > ? AND deleted = 0 ORDER BY notification_id DESC LIMIT 10;";
        fetchNotificationSql(response, sql, socialgroup_id, notification_id);

    }

    private void fetchOldNotification(HttpServletResponse response, String socialgroup_id, String notification_id) throws SQLException {
        String sql = "SELECT notification.notification_id, user_id, user_nickname, user_avatar, type, create_date, brief, welcome, " +
                "hold_date, hold_location, holder, detail, link FROM notification, notification_poster " +
                "WHERE notification.notification_id = notification_poster.notification_id " +
                "AND notification.notification_id < ? AND deleted = 0 ORDER BY notification_id DESC LIMIT 10;";
        fetchNotificationSql(response, sql, socialgroup_id, notification_id);
    }


    private void fetchNotificationSql(HttpServletResponse response, String sql, String socialgroup_id, String notification_id) throws SQLException {
        JSONObject json_result = new JSONObject();// 最终的返回的结果
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        PreparedStatement  stmt = conn.prepareStatement(sql);

        // 设置参数
        stmt.setInt(1, Integer.parseInt(notification_id));

        ResultSet rs = stmt.executeQuery();
        JSONArray json_array = new JSONArray();

        while(rs.next()){
            JSONObject json_object = new JSONObject();
            json_object.put("notification_id", rs.getString("notification_id"));
            json_object.put("user_id", rs.getString("user_id"));
            json_object.put("user_nickname", rs.getString("user_nickname"));
            json_object.put("user_avatar", rs.getString("user_avatar"));
            json_object.put("type", rs.getString("type"));
            json_object.put("create_date", rs.getString("create_date"));
            json_object.put("brief", rs.getString("brief"));
            json_object.put("welcome", rs.getString("welcome"));
            json_object.put("hold_date", rs.getString("hold_date"));
            json_object.put("hold_location", rs.getString("hold_location"));
            json_object.put("holder", rs.getString("holder"));
            json_object.put("detail", rs.getString("detail"));
            json_object.put("link", rs.getString("link"));

            json_array.add(json_object);
        }

        json_result.put("poster", json_array);

        

        Response.responseSuccessInfo(response, json_result);
    }
}