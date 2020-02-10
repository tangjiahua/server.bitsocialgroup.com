package profile;

import com.sun.deploy.net.HttpResponse;
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
import java.text.SimpleDateFormat;
import java.util.Date;

@WebServlet("/profile/stick")
public class Stick extends HttpServlet {

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

        String stick_to_user_id = (String)jsonObject.get("stick_to_user_id");
        String user_id = (String)jsonObject.get("user_id");
        String password = (String)jsonObject.get("password");
        String socialgroup_id = (String)jsonObject.get("socialgroup_id");

        if(TestInfo.testUser_id(user_id) && TestInfo.testPassword(password)){
            try {
                if(Authenticator.authenticate(user_id, password)){
                    // 验证成功
                    Date date = new Date();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String dateStr = format.format(date);

                    String sql = "INSERT INTO stick(stick_from_user_id, stick_to_user_id, create_date) VALUES " +
                            "(?, ?, ?);";

                    Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                    Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                    PreparedStatement stmt = conn.prepareStatement(sql);

                    //设置参数
                    stmt.setInt(1, Integer.parseInt(user_id));
                    stmt.setInt(2, Integer.parseInt(stick_to_user_id));
                    stmt.setString(3, dateStr);

                    int result = stmt.executeUpdate();

                    if(result == 1){
                        // 接下来将该戳一戳信息加入到push_message中
                        Response.responseSuccessInfo(response, "戳一戳成功");
                        PushMessage.pushStickMessage(user_id, stick_to_user_id, socialgroup_id);

                    }else{
                        // 插入失败
                        Response.responseError(response, "Stick插入数据库失败");
                    }
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
