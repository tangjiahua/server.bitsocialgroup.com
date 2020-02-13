package square;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import util.Response;
import util.Sql;
import util.TestInfo;
import util.Util;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

@WebServlet("/square/fetch")
public class Fetch extends HttpServlet {

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
        String square_item_type = null;
        String method = null;
        String square_item_id = null;
        JSONObject jsonObject = Util.requestToJsonObject(req);

        socialgroup_id = jsonObject.getString("socialgroup_id");
        square_item_type = jsonObject.getString("square_item_type");
        method = jsonObject.getString("method");
        square_item_id = jsonObject.getString("square_item_id");

        if(!TestInfo.testSocialgroupId(socialgroup_id)){
            Response.responseError(resp, "fetch.java socialgroup_id格式不对");
            return;
        }

        try{
            Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信

            if(method.equals("1")){
                fetchNew(req, resp, square_item_type,  socialgroup_id,  square_item_id);
            }else if(method.equals("2")){
                fetchOld(req, resp, square_item_type,  socialgroup_id,  square_item_id);
            }else{
                Response.responseError(resp, "fetch.java: 没有这样的method");
            }
        } catch (SQLException | ClassNotFoundException e) {
            Response.responseError(resp, "fetch.java: " + e.toString());
        }


    }

    /**
     * 拉取新的帖子
     * @param req
     * @param resp
     */
    private void fetchNew(HttpServletRequest req, HttpServletResponse resp, String square_item_type, String socialgroup_id, String square_item_id) throws SQLException, ClassNotFoundException {
        if(square_item_type.equals("broadcast")){
            fetchNewBroadcast(req, resp,  socialgroup_id, square_item_id);
        }else if(square_item_type.equals("circle")){
            fetchNewCircle(req, resp,  socialgroup_id, square_item_id);
        }
    }

    /**
     * 拉取旧的帖子
     * @param req
     * @param resp
     */
    private void fetchOld(HttpServletRequest req, HttpServletResponse resp, String square_item_type, String socialgroup_id, String square_item_id) throws SQLException, ClassNotFoundException {
        if(square_item_type.equals("broadcast")){
            fetchOldBroadcast(req, resp,  socialgroup_id, square_item_id);
        }else if(square_item_type.equals("circle")){
            fetchOldCircle(req, resp,  socialgroup_id, square_item_id);
        }
    }


    /**
     * 拉取新的broadcast帖子
     * @param req
     * @param resp
     */
    private void fetchNewBroadcast(HttpServletRequest req, HttpServletResponse resp, String socialgroup_id, String square_item_id) throws SQLException, ClassNotFoundException {
        String sql = "SELECT broadcast_id, type, title, content, create_date, comment_count, like_count" +
                ", dislike_count, picture_count FROM broadcast WHERE broadcast_id > ? AND deleted = 0 " +
                "ORDER BY broadcast_id DESC LIMIT 50;";
        fetchBroadcastSql(sql, resp, socialgroup_id, square_item_id);
    }

    /**
     * 拉取新的circle帖子
     * @param req
     * @param resp
     */
    private void fetchNewCircle(HttpServletRequest req, HttpServletResponse resp, String socialgroup_id, String square_item_id) throws SQLException {
        String sql = "SELECT circle_id, circle.user_id, nickname, avatar, type, content," +
                " create_date, comment_count, like_count, picture_count " +
                "FROM circle, user_profile WHERE circle.user_id = user_profile.user_id " +
                "AND circle_id > ? AND deleted = 0 ORDER BY circle_id DESC LIMIT 50;";
        fetchCircleSql(sql,resp, socialgroup_id, square_item_id);
    }

    /**
     * 拉取旧的broadcast帖子
     * @param req
     * @param resp
     */
    private void fetchOldBroadcast(HttpServletRequest req, HttpServletResponse resp, String socialgroup_id, String  square_item_id) throws ClassNotFoundException, SQLException {
        String sql = "SELECT broadcast_id, type, title, content, create_date, comment_count, like_count" +
                ", dislike_count, picture_count FROM broadcast WHERE broadcast_id < ? AND deleted = 0 " +
                "ORDER BY broadcast_id DESC LIMIT 50;";
       fetchBroadcastSql( sql,  resp,  socialgroup_id,  square_item_id);
    }

    /**
     * 拉取旧的circle帖子
     * @param req
     * @param resp
     */
    private void fetchOldCircle(HttpServletRequest req, HttpServletResponse resp, String socialgroup_id, String square_item_id) throws SQLException {
        String sql = "SELECT circle_id, circle.user_id, nickname, avatar, type, content," +
                " create_date, comment_count, like_count, picture_count " +
                "FROM circle, user_profile WHERE circle.user_id = user_profile.user_id " +
                "AND circle_id < ? AND deleted = 0 ORDER BY circle_id DESC LIMIT 50;";
        fetchCircleSql(sql, resp,  socialgroup_id,  square_item_id);
    }


    /**
     * 拉取broadcast的SQL程序
     * @param sql
     * @param resp
     * @throws SQLException
     */
    private void fetchBroadcastSql(String sql, HttpServletResponse resp, String socialgroup_id, String square_item_id) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        PreparedStatement stmt = conn.prepareStatement(sql);

        // 设置参数
        stmt.setInt(1, Integer.parseInt(square_item_id));

        // 执行
        ResultSet rs = stmt.executeQuery();
        JSONArray jsonArray = new JSONArray();
        while(rs.next()){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("broadcast_id", rs.getString("broadcast_id"));
            jsonObject.put("type", rs.getString("type"));
            jsonObject.put("title", rs.getString("title"));
            jsonObject.put("content", rs.getString("content"));
            jsonObject.put("create_date", rs.getString("create_date"));
            jsonObject.put("comment_count", rs.getString("comment_count"));
            jsonObject.put("like_count", rs.getString("like_count"));
            jsonObject.put("dislike_count", rs.getString("dislike_count"));
            jsonObject.put("picture_count", rs.getString("picture_count"));
            jsonArray.add(jsonObject);
        }

        Response.responseSuccessInfo(resp, jsonArray);
        rs.close();
        stmt.close();
        conn.close();
    }


    /**
     * 拉取circle的sql程序
     * @param sql
     * @param resp
     * @throws SQLException
     */
    private void fetchCircleSql(String sql, HttpServletResponse resp, String socialgroup_id, String square_item_id) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        PreparedStatement stmt = conn.prepareStatement(sql);

        // 设置参数
        stmt.setInt(1, Integer.parseInt(square_item_id));

        // 执行
        ResultSet rs = stmt.executeQuery();
        JSONArray jsonArray = new JSONArray();
        while(rs.next()){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("circle_id", rs.getString("circle_id"));
            jsonObject.put("user_id", rs.getString("user_id"));
            jsonObject.put("user_nickname", rs.getString("nickname"));
            jsonObject.put("user_avatar", rs.getString("avatar"));
            jsonObject.put("type", rs.getString("type"));
            jsonObject.put("content", rs.getString("content"));
            jsonObject.put("create_date", rs.getString("create_date"));
            jsonObject.put("comment_count", rs.getString("comment_count"));
            jsonObject.put("like_count", rs.getString("like_count"));
            jsonObject.put("picture_count", rs.getString("picture_count"));
            jsonArray.add(jsonObject);
        }

        Response.responseSuccessInfo(resp, jsonArray);
        rs.close();
        stmt.close();
        conn.close();
    }
}
