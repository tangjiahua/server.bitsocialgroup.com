package discover.members;

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

@WebServlet("/discover/members/search")
public class Search extends HttpServlet {

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
        Connection conn = null;


        String socialgroup_id = null;
        String info = null;
        String method = null;

        String user_id = null;
        String password = null;

        JSONObject jsonObject = Util.requestToJsonObject(req);

        socialgroup_id = jsonObject.getString("socialgroup_id");
        method = jsonObject.getString("method");
        info = jsonObject.getString("info");

        user_id = jsonObject.getString("user_id");
        password = jsonObject.getString("password");

        try {
            if(TestInfo.testUser_id(user_id) && TestInfo.testPassword(password) && TestInfo.testSocialgroupId(socialgroup_id)
                    && Authenticator.authenticate(user_id, password)){
                Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

                if(method.equals("1")){
                    searchByRealName(resp, info, conn);
                }else if(method.equals("2")){
                    searchByNickName(resp, info, conn);
                }

                else{
                    Response.responseError(resp, "fetch.java: 没有这样的method");
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            Response.responseError(resp, e.toString());
        }
    }

    private void searchByRealName(HttpServletResponse response, String info, Connection conn) throws SQLException {
        String sql = "SELECT user_id, avatar, nickname, realname, gender, age " +
                "FROM user_profile WHERE realname LIKE '%" + info + "%';";
        fetchSql(response, sql, conn);
    }

    private void searchByNickName(HttpServletResponse response, String info, Connection conn) throws SQLException {
        String sql = "SELECT user_id, avatar, nickname, realname, gender, age " +
                "FROM user_profile WHERE nickname LIKE '%" + info + "%';";
        fetchSql(response, sql, conn);
    }

    private void fetchSql(HttpServletResponse response, String sql, Connection conn) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);



        // 执行
        ResultSet rs = stmt.executeQuery();
        JSONArray jsonArray = new JSONArray();

        while(rs.next()){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("user_id", String.valueOf(rs.getInt("user_id")));
            jsonObject.put("avatar", String.valueOf(rs.getInt("avatar")));
            jsonObject.put("nickname", rs.getString("nickname"));
            jsonObject.put("realname", rs.getString("realname"));
            jsonObject.put("gender", rs.getString("gender"));
            jsonObject.put("age", String.valueOf(rs.getInt("age")));
            jsonArray.add(jsonObject);
        }

        Response.responseSuccessInfo(response, jsonArray);
        rs.close();
        stmt.close();
        conn.close();
    }
}

