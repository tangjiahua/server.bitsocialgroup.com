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

@WebServlet("/square/fetch_detail_reply")
public class FetchDetailReply extends HttpServlet {

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
        String comment_id;

        String user_id;
        String password;

        socialgroup_id = jsonObject.getString("socialgroup_id");
        square_item_type = jsonObject.getString("square_item_type");
        square_item_id = jsonObject.getString("square_item_id");
        comment_id = jsonObject.getString("comment_id");

        user_id = (String)jsonObject.get("user_id");
        password = (String)jsonObject.get("password");

        try{
            if(TestInfo.testSocialgroupId(socialgroup_id) && TestInfo.testUser_id(user_id) && TestInfo.testPassword(password)
                    && Authenticator.authenticate(user_id, password)){
                // 检测完毕 可以拉取
                Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

                switch(square_item_type){
                    case "broadcast":{
                        String sql_broadcast_reply = "SELECT reply_id, reply_from_user_id, nickname," +
                                " avatar, reply_to_user_id, user_profile.nickname as reply_to_user_nickname, content," +
                                " create_date FROM broadcast_reply, user_profile " +
                                " WHERE deleted = 0 AND broadcast_id = ? AND broadcast_reply.reply_from_user_id = user_profile.user_id" +
                                " AND comment_id = ?";


                        sqlExecute(response, square_item_id, comment_id, conn, sql_broadcast_reply);

                        break;
                    }


                    case "circle":{
                        String sql_circle_reply = "SELECT reply_id, reply_from_user_id, nickname," +
                                " avatar, reply_to_user_id, user_profile.nickname as reply_to_user_nickname, content," +
                                " create_date FROM circle_reply, user_profile " +
                                " WHERE deleted = 0 AND circle_id = ? AND circle_reply.reply_from_user_id = user_profile.user_id" +
                                " AND comment_id = ?";

                        sqlExecute(response, square_item_id, comment_id, conn, sql_circle_reply);

                        break;
                    }


                }

            }
        } catch (SQLException | ClassNotFoundException e) {
            Response.responseError(response, e.toString());
        }



    }

    private void sqlExecute(HttpServletResponse response, String square_item_id, String comment_id, Connection conn, String sql_circle_reply) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql_circle_reply);
        stmt.setInt(1, Integer.parseInt(square_item_id));
        stmt.setInt(2, Integer.parseInt(comment_id));
        ResultSet rs = stmt.executeQuery();
        JSONArray json_array_reply = new JSONArray();
        while(rs.next()){
            JSONObject json_object = new JSONObject();
            json_object.put("reply_id",rs.getString("reply_id"));
            json_object.put("reply_from_user_id", rs.getString("reply_from_user_id"));
            json_object.put("nickname", rs.getString("nickname"));
            json_object.put("avatar", rs.getString("avatar"));
            json_object.put("reply_to_user_id", rs.getString("reply_to_user_id"));
            json_object.put("reply_to_user_nickname", rs.getString("reply_to_user_nickname"));
            json_object.put("content", rs.getString("content"));
            json_object.put("create_date", rs.getString("create_date"));

            json_array_reply.add(json_object);
        }
        rs.close();

        JSONObject json_result = new JSONObject();
        json_result.put("reply", json_array_reply);

        Response.responseSuccessInfo(response, json_result);
    }
}
