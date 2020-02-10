package square;

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

@WebServlet("/square/comment")
public class Comment extends HttpServlet {
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

    /**
     * POST 处理
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{

        String socialgroup_id = null;
        String square_item_type = null;
        String square_item_id = null;
        String content = null;
        String user_id = null;
        String password = null;

        JSONObject jsonObject = Util.requestToJsonObject(request);
        socialgroup_id = (String)jsonObject.get("socialgroup_id");
        square_item_type = (String )jsonObject.get("square_item_type");
        square_item_id = (String)jsonObject.get("square_item_id");
        content = (String)jsonObject.get("content");

        user_id = (String)jsonObject.get("user_id");
        password = (String)jsonObject.get("password");

        try{
            if(TestInfo.testUser_id(user_id) && TestInfo.testPassword(password)
                    && TestInfo.testSocialgroupId(socialgroup_id) && Authenticator.authenticate(user_id, password)){
                // 验证通过
                Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信

                if(square_item_type.equals("broadcast") || square_item_type.equals("circle")){
                    commentSquareItem(response, square_item_type, socialgroup_id, square_item_id, user_id, content);
                }else{
                    Response.responseError(response, "judge.java square_item_type 不正确");
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            Response.responseError(response, e.toString());
        }

    }

    private void commentSquareItem(HttpServletResponse response, String square_item_type, String socialgroup_id, String square_item_id, String user_id, String content) throws SQLException, ClassNotFoundException {

        String sql = "INSERT INTO " + square_item_type + "_comment( " + square_item_type +"_id, user_id, deleted, content, " +
                "create_date, reply_count) VALUES (?, ?, 0, ?, ?, 0)";
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        PreparedStatement stmt = conn.prepareStatement(sql);


        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = format.format(date);

        stmt.setInt(1, Integer.parseInt(square_item_id));
        stmt.setInt(2, Integer.parseInt(user_id));
        stmt.setString(3, content);
        stmt.setString(4, dateStr);

        int result = stmt.executeUpdate();

        if(result == 1){
            Response.responseSuccessInfo(response, "成功评论");
        }else{
            Response.responseError(response, "评论失败");
        }

        PushMessage.pushCommentMessage(socialgroup_id, user_id, square_item_type, square_item_id);

        stmt.close();
        conn.close();
    }


}
