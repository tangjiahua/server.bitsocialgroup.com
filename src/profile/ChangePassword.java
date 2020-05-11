package profile;

import net.sf.json.JSONObject;
import util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

@WebServlet("/profile/change_password")
public class ChangePassword extends HttpServlet {

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

        String user_id = (String)jsonObject.get("user_id");
        String password = jsonObject.getString("password");
        String new_password = (String)jsonObject.get("new_password");

        if(TestInfo.testUser_id(user_id) && TestInfo.testPassword(password) && TestInfo.testPassword(new_password)){
            try {
                if(Authenticator.authenticate(user_id, password)){
                    // 验证成功
                    String insert_sql = "UPDATE user SET password = ? WHERE user_id = ?";
                    Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                    PreparedStatement stmt;
                    Connection conn = null;
                    conn = DriverManager.getConnection(DB_URL + DB_NAME_SGS, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                    stmt = conn.prepareStatement(insert_sql);


                    stmt.setString(1, new_password);
                    stmt.setInt(2, Integer.valueOf(user_id));

                    int result = stmt.executeUpdate();

                    if(result == 1){
                        Response.responseSuccessInfo(response, "成功更改密码");
                        Authenticator.deleteAuthenticate(user_id, password);
                    }else{
                        // 失败
                        Response.responseError(response, "修改密码失败");
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
