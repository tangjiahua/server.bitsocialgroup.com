package square;

import net.sf.json.JSONObject;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.omg.CosNaming.NamingContextExtPackage.StringNameHelper;
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

@WebServlet("/square/judge")
public class Judge extends HttpServlet {
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
        String judge_type = null;
        String is_to_cancel = null;

        String user_id = null;
        String password = null;


        JSONObject jsonObject = Util.requestToJsonObject(request);
        socialgroup_id = (String)jsonObject.get("socialgroup_id");
        square_item_type = (String )jsonObject.get("square_item_type");
        square_item_id = (String)jsonObject.get("square_item_id");
        judge_type = (String)jsonObject.get("judge_type");
        is_to_cancel = (String)jsonObject.get("is_to_cancel");

        user_id = (String)jsonObject.get("user_id");
        password = (String)jsonObject.get("password");

        try{
            if(TestInfo.testUser_id(user_id) && TestInfo.testPassword(password)
                    && TestInfo.testSocialgroupId(socialgroup_id) && Authenticator.authenticate(user_id, password)){
                // 验证通过
                Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信


                if(square_item_type.equals("broadcast")){
                    judgeSquareItem(response, 1, socialgroup_id, judge_type, square_item_id, user_id, is_to_cancel);
                }else if(square_item_type.equals("circle")){
                    judgeSquareItem(response, 2, socialgroup_id, judge_type, square_item_id, user_id, is_to_cancel);
                }else{
                    Response.responseError(response, "judge.java square_item_type 不正确");
                }

            }
        } catch (SQLException | ClassNotFoundException e) {
            Response.responseError(response, e.toString());
        }
    }

    private void judgeSquareItem(HttpServletResponse response, Integer square_item_type_db,
                                 String socialgroup_id, String judge_type, String square_item_id, String user_id, String is_to_cancel) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        if(is_to_cancel.equals("0")){
            String sql = "INSERT INTO judge(judge_type, square_item_type, square_item_id, from_user_id, " +
                    "canceled, create_date) VALUES (?, ?, ?, ?, 0, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);


            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateStr = format.format(date);
            stmt.setInt(1, Integer.parseInt(judge_type));
            stmt.setInt(2, square_item_type_db);
            stmt.setInt(3, Integer.parseInt(square_item_id));
            stmt.setInt(4, Integer.parseInt(user_id));
            stmt.setString(5, dateStr);

            int result = stmt.executeUpdate();

            if(result == 1){
                Response.responseSuccessInfo(response, "成功judge");
            }

            stmt.close();
            conn.close();
        }else{
            String sql = "DELETE FROM judge WHERE judge_type = ? AND square_item_type = ? AND square_item_id = ? AND " +
                    "from_user_id = ?;";
            PreparedStatement stmt = conn.prepareStatement(sql);


            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateStr = format.format(date);
            stmt.setInt(1, Integer.parseInt(judge_type));
            stmt.setInt(2, square_item_type_db);
            stmt.setInt(3, Integer.parseInt(square_item_id));
            stmt.setInt(4, Integer.parseInt(user_id));

            int result = stmt.executeUpdate();

            if(result == 1){
                Response.responseSuccessInfo(response, "成功judge");
            }

            stmt.close();
            conn.close();

        }




    }

}
