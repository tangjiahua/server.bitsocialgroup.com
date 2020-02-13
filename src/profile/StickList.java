package profile;

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

@WebServlet("/profile/stick_list")
public class StickList extends HttpServlet {

    private static String JDBC_DRIVER = Sql.GLOBAL_JDBC_DRIVER;
    private static String DB_URL = Sql.GLOBAL_DB_URL;
    private static String DB_NAME_SG = Sql.GLOBAL_DB_NAME_SG;
    private static String DB_NAME_SGS = Sql.GLOBAL_DB_NAME_SGS;
    private static String USER = Sql.GLOBAL_USER;
    private static String PASS = Sql.GLOBAL_PASS;


    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        this.doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        JSONObject jsonObject = Util.requestToJsonObject(request);

        String user_id = (String)jsonObject.get("user_id");
        String password = (String)jsonObject.get("password");
        String socialgroup_id = (String)jsonObject.get("socialgroup_id");

        if(TestInfo.testUser_id(user_id) && TestInfo.testPassword(password)){
            try {
                if(Authenticator.authenticate(user_id, password)){
                    // 验证成功
                    // SELECT user_id, nickname, realname, gender, avatar, age FROM stick, user_profile
                    // WHERE stick.stick_to_user_id = xx AND stick.stick_from_user_id = user_profile.user_id

                    String sql = "SELECT user_id, nickname, realname, gender, " +
                            "avatar, age FROM stick, user_profile " +
                            " WHERE stick.stick_to_user_id = ? AND " +
                            "stick.stick_from_user_id = user_profile.user_id";
                    Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                    Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                    PreparedStatement stmt = conn.prepareStatement(sql);

                    //设置参数
                    stmt.setInt(1, Integer.parseInt(user_id));

                    ResultSet rs = stmt.executeQuery();

                    JSONArray userlist = new JSONArray();
                    if(rs.next()){
                        JSONObject user = new JSONObject();
                        user.put("user_id", rs.getString("user_id"));
                        user.put("nickname", rs.getString("nickname"));
                        user.put("realname", rs.getString("realname"));
                        user.put("gender", rs.getString("gender"));
                        user.put("avatar", rs.getString("avatar"));
                        user.put("age", rs.getString("age"));

                        userlist.add(user);
                    }
                    Response.responseSuccessInfo(response, userlist);
                    rs.close();
                    stmt.close();
                    conn.close();

                }else{
                    // 验证失败
                    Response.responseError(response, "身份验证失败");
                }
            } catch (SQLException | ClassNotFoundException e) {
                Response.responseError(response, e.toString());
            }
        }else{
            Response.responseError(response, "账号格式或者密码格式不对");
        }
    }
}
