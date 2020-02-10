package square;

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

@WebServlet("/square/fetch_detail")
public class FetchDetail extends HttpServlet {
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

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject jsonObject = Util.requestToJsonObject(request);
        String socialgroup_id;
        String square_item_type;
        String square_item_id;
        String method;
        String user_id;
        String password;


        socialgroup_id = jsonObject.getString("socialgroup_id");
        square_item_type = jsonObject.getString("square_item_type");
        square_item_id = jsonObject.getString("square_item_id");
        method = jsonObject.getString("method");

        user_id = (String)jsonObject.get("user_id");
        password = (String)jsonObject.get("password");


        try{
            if(TestInfo.testSocialgroupId(socialgroup_id) && TestInfo.testUser_id(user_id) && TestInfo.testPassword(password)
            && Authenticator.authenticate(user_id, password)){
                // 检测完毕 可以拉取
                Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信

                switch (method) {
                    case "comment":
                        fetchDetail(response, square_item_type, socialgroup_id, square_item_id);
                        break;
                    case "like":
                        fetchDetailLike(response, square_item_type, socialgroup_id, square_item_id);
                        break;
                    case "dislike":
                        fetchDetailDislike(response, square_item_type, socialgroup_id, square_item_id);
                        break;
                    default:
                        Response.responseError(response, "commentdetail.java 没有这种拉取方法");
                        break;
                }

            }
        } catch (SQLException | ClassNotFoundException e) {
            Response.responseError(response, e.toString());
        }
    }

    /**
     * 拉取评论区
     * @param response
     */
    private void fetchDetail(HttpServletResponse response, String square_item_type, String socialgroup_id, String square_item_id) throws SQLException {
        if(square_item_type.equals("broadcast")){

            fetchDetailBroadcast(response, socialgroup_id, square_item_id);

        }else if(square_item_type.equals("circle")){

            fetchDetailCircle(response, socialgroup_id, square_item_id);

        }else{

            Response.responseError(response, "commentdetail.java method不对");

        }


    }


    /**
     * 拉取broadcast
     * @param response
     * @throws SQLException
     */
    private void fetchDetailBroadcast(HttpServletResponse response, String socialgroup_id, String square_item_id) throws SQLException {
        JSONObject json_result = new JSONObject();

        // 拉取评论的部分 并添加进json_result中
        String sql_comment = "SELECT comment_id, broadcast_comment.user_id," +
                " nickname, avatar, content, create_date, reply_count" +
                " FROM broadcast_comment, user_profile WHERE broadcast_comment.broadcast_id = ? AND " +
                "broadcast_comment.user_id = user_profile.user_id AND broadcast_comment.deleted = 0;";
        fetchDetailComment(json_result, sql_comment, socialgroup_id, square_item_id);

        // 拉取回复的部分 并添加进json_result中
        String sql_reply = "SELECT comment_id,reply_id, reply_from_user_id, reply_from_user_nickname, " +
                "reply_to_user_id, reply_to_user_nickname, content, create_date FROM broadcast_reply " +
                "WHERE deleted = 0 AND broadcast_id = ?;";
        fetchDetailReply(json_result, sql_reply, socialgroup_id, square_item_id);


        // 将他们都返回
        Response.responseSuccessInfo(response, json_result);

    }



    /**
     * 拉取circle
     * @param response
     * @throws SQLException
     */
    private void fetchDetailCircle(HttpServletResponse response, String socialgroup_id, String square_item_id) throws SQLException {
        JSONObject json_result = new JSONObject();

        // 拉取评论的部分 并添加进json_result中
        String sql_comment = "SELECT comment_id, circle_comment.user_id, " +
                "nickname, avatar, content, create_date, reply_count " +
                "FROM circle_comment, user_profile WHERE circle_comment.circle_id = ? AND " +
                "circle_comment.user_id = user_profile.user_id AND circle_comment.deleted = 0;";
        fetchDetailComment(json_result, sql_comment, socialgroup_id, square_item_id);

        // 拉取回复的部分 并添加进json_result中
        // "拉取该回复"和"拉取回复区的回复"的区别在于这个地方的回复最多两条
        String sql_reply = "SELECT comment_id,reply_id, reply_from_user_id, reply_from_user_nickname, " +
                "reply_to_user_id, reply_to_user_nickname, content, create_date FROM circle_reply " +
                "WHERE deleted = 0 AND circle_id = ?;";

        fetchDetailReply(json_result, sql_reply, socialgroup_id, square_item_id);

        // 将他们都返回
        Response.responseSuccessInfo(response, json_result);

    }




    private void fetchDetailComment(JSONObject json_result, String sql_comment, String socialgroup_id, String square_item_id) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        PreparedStatement stmt = conn.prepareStatement(sql_comment);
        stmt.setInt(1, Integer.parseInt(square_item_id));
        ResultSet rs = stmt.executeQuery();
        JSONArray json_array = new JSONArray();
        while(rs.next()){
            JSONObject json_object = new JSONObject();
            json_object.put("comment_id", rs.getInt("comment_id"));
            json_object.put("user_id", rs.getInt("user_id"));
            json_object.put("user_nickname", rs.getString("nickname"));
            json_object.put("user_avatar", rs.getString("avatar"));
            json_object.put("content", rs.getString("content"));
            json_object.put("create_date", rs.getString("create_date"));
            json_object.put("reply_count", rs.getInt("reply_count"));

            json_array.add(json_object);
        }

        json_result.put("comment", json_array);
        rs.close();
    }

    private void fetchDetailReply(JSONObject json_result, String sql_reply, String socialgroup_id, String square_item_id) throws SQLException {

        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        PreparedStatement stmt = conn.prepareStatement(sql_reply);
        stmt.setInt(1, Integer.parseInt(square_item_id));
        ResultSet rs = stmt.executeQuery();
        JSONArray json_array_reply = new JSONArray();
        while(rs.next()){
            JSONObject json_object = new JSONObject();
            json_object.put("comment_id", rs.getInt("comment_id"));
            json_object.put("reply_id",rs.getInt("reply_id"));
            json_object.put("reply_from_user_id", rs.getInt("reply_from_user_id"));
            json_object.put("reply_from_user_nickname", rs.getString("reply_from_user_nickname"));
            json_object.put("reply_to_user_id", rs.getInt("reply_to_user_id"));
            json_object.put("reply_to_user_nickname", rs.getString("reply_to_user_nickname"));
            json_object.put("content", rs.getString("content"));
            json_object.put("create_date", rs.getString("create_date"));

            json_array_reply.add(json_object);
        }
        json_result.put("reply", json_array_reply);
        rs.close();
    }






    /**
     * 拉取喜欢区
     * @param response
     */
    private void fetchDetailLike(HttpServletResponse response, String square_item_type, String socialgroup_id, String square_item_id) throws SQLException {

        int squaretype = 0;
        if(square_item_type.equals("broadcast")){
            squaretype = 1;
        }else if(square_item_type.equals("circle")){
            squaretype = 2;
        }else{
            Response.responseError(response, "commentdetail.java 没有该squareitemtype");
            return;
        }

        String sql = "SELECT judge_id, from_user_id, nickname, avatar FROM judge, user_profile WHERE judge_type = 1 AND " +
                "square_item_id = ? AND canceled = 0 AND square_item_type = ? AND user_profile.user_id = judge.from_user_id";


        // 将他们都返回
        fetchDetailLikeOrDislikeSql(response, sql, socialgroup_id, square_item_id, squaretype);

    }

    /**
     * 拉取疑惑区
     * @param response
     */
    private  void fetchDetailDislike(HttpServletResponse response, String square_item_type, String socialgroup_id, String square_item_id) throws SQLException {

        int squaretype = 0;
        if(square_item_type.equals("broadcast")){
            squaretype = 1;
        }else if(square_item_type.equals("circle")){
            squaretype = 2;
        }else{
            Response.responseError(response, "commentdetail.java 没有该squareitemtype");
            return;
        }
        String sql = "SELECT judge_id, from_user_id, nickname, avatar FROM judge, user_profile WHERE judge_type = 2 AND " +
                "square_item_id = ? AND canceled = 0 AND square_item_type = ? AND user_profile.user_id = judge.from_user_id";

        fetchDetailLikeOrDislikeSql(response, sql, socialgroup_id, square_item_id, squaretype);

    }



    private void fetchDetailLikeOrDislikeSql(HttpServletResponse response, String sql,String socialgroup_id, String square_item_id, int squaretype) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, Integer.parseInt(square_item_id));
        stmt.setInt(2, squaretype);
        ResultSet rs = stmt.executeQuery();
        JSONArray json_array = new JSONArray();
        while(rs.next()){
            JSONObject json_object = new JSONObject();
            json_object.put("judge_id", rs.getInt("judge_id"));
            json_object.put("user_id",rs.getInt("from_user_id"));
            json_object.put("nickname", rs.getString("nickname"));
            json_object.put("avatar", rs.getInt("avatar"));

            json_array.add(json_object);
        }
        // 将他们都返回
        Response.responseSuccessInfo(response, json_array);

        rs.close();
        stmt.close();
        conn.close();
    }

}
