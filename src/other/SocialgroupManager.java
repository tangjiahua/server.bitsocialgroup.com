package other;

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

@WebServlet("/socialgroup_manager")
public class SocialgroupManager extends HttpServlet {

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

        String method = null;
        String user_id = null;
        String password = null;

        JSONObject jsonObject = Util.requestToJsonObject(request);


        method = (String)jsonObject.get("method");
        user_id = (String)jsonObject.get("user_id");
        password = (String)jsonObject.get("password");

        try {
            if(TestInfo.testUser_id(user_id) && TestInfo.testPassword(password)){
                if(Authenticator.authenticate(user_id, password)){
                    //验证成功
                    if(method.equals("fetch")){
                        this.responseSocialgroupList(response);
                    }else if(method.equals("join")){
                        this.joinSocialgroup(response, jsonObject, user_id);
                    }

                }else{
                    //验证失败
                    Response.responseError(response, "身份验证失败");
                }
            }else{
                Response.responseError(response, "SocialgroupManager.java 账号或密码格式不对");
            }

        } catch (SQLException | ClassNotFoundException e) {
            Response.responseError(response, e.toString());
        }
    }


    private void responseSocialgroupList(HttpServletResponse response) throws ClassNotFoundException, SQLException, IOException {

        String sql = "SELECT * FROM socialgroup";
        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SGS, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        PreparedStatement stmt = conn.prepareStatement(sql);

        ResultSet rs = stmt.executeQuery();

        JSONArray socialgroup_list = new JSONArray();

        while(rs.next()){
            JSONObject socialgroup = new JSONObject();
            String socialgroup_id = String.valueOf(rs.getInt("socialgroup_id"));
            String name = rs.getString("name");
            String avatar = rs.getString("avatar");

            socialgroup.put("socialgroup_id", socialgroup_id);
            socialgroup.put("socialgroup_name", name);
            socialgroup.put("socialgroup_avatar", avatar);
            socialgroup_list.add(socialgroup);
        }
        rs.close();
        stmt.close();
        conn.close();
        Response.responseSuccessInfo(response, socialgroup_list);
    }


    private void joinSocialgroup(HttpServletResponse response, JSONObject jsonObject, String user_id) throws ClassNotFoundException, SQLException {
        String socialgroup_id = (String)jsonObject.get("socialgroup_id");
        //该用户想要加入社群id为socialgroup_id的社群
        String sql = "SELECT * FROM user_profile WHERE user_id = ?;";
        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        PreparedStatement stmt = conn.prepareStatement(sql);

        stmt.setInt(1, Integer.parseInt(user_id));


        ResultSet rs = stmt.executeQuery();

        if(rs.next()){
            //竟然已经有结果了
            Response.responseError(response, "已经加入了该社群");

        }else{
            //没有结果，说明可以加入
            String joinSql = "INSERT INTO user_profile(user_id) VALUES (?);";

            stmt = conn.prepareStatement(joinSql);

            stmt.setInt(1, Integer.parseInt(user_id));

            int result = stmt.executeUpdate();

            if(result == 1){
                //插入成功
                Response.responseSuccessInfo(response, "您已加入社群");
            }else{
                Response.responseError(response, "加入社群的时候插入数据库失败");
            }
        }

        rs.close();
        stmt.close();
        conn.close();
    }
}
