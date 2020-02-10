package square;

import net.sf.json.JSONObject;
import util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.time.chrono.IsoChronology;


@WebServlet("/square/delete")
public class Delete extends HttpServlet {

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
        String delete_type = null;
        String correspond_id = null;
        String user_id = null;
        String password = null;

        JSONObject jsonObject = Util.requestToJsonObject(request);
        socialgroup_id = (String)jsonObject.get("socialgroup_id");
        square_item_type = (String )jsonObject.get("square_item_type");
        delete_type = (String)jsonObject.get("delete_type");
        correspond_id = (String)jsonObject.get("correspond_id");

        user_id = (String)jsonObject.get("user_id");
        password = (String)jsonObject.get("password");

        try{
            if(TestInfo.testSocialgroupId(socialgroup_id) && TestInfo.testUser_id(user_id) &&
            TestInfo.testPassword(password) && Authenticator.authenticate(user_id, password)){
                // 验证通过
                Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信

                if(square_item_type.equals("broadcast") || square_item_type.equals("circle")){
                    delete( response,  delete_type,  socialgroup_id,  square_item_type,  correspond_id,  user_id);
                }else{
                    Response.responseError(response, "judge.java square_item_type 不正确");
                }

            }
        } catch (SQLException | ClassNotFoundException e) {
            Response.responseError(response, e.toString());
        }
    }





    private void delete(HttpServletResponse response, String delete_type, String socialgroup_id, String square_item_type, String correspond_id, String user_id) throws SQLException {
        switch (delete_type) {
            case "1":
                if (testSquareItemIfCorrespondUser( socialgroup_id,  square_item_type,  correspond_id,  user_id)) {
                    deleteSquareItem( response,  socialgroup_id,  square_item_type,  correspond_id);
                } else {
                    Response.responseError(response, "delete.java 你不能删除别人发布的内容");
                }
                break;
            case "2":
                if (testSquareCommentIfCorrespondUser( socialgroup_id,  square_item_type,  correspond_id,  user_id)) {
                    deleteSquareComment( response,  socialgroup_id,  square_item_type,  correspond_id);

                }else{
                    Response.responseError(response, "delete.java 你不能删除别人发布的内容");
                }
                break;
            case "3":
                if (testSquareReplyIfCorrespondUser( square_item_type,  socialgroup_id,  correspond_id,  user_id)) {
                    deleteSquareReply(response, socialgroup_id, square_item_type, correspond_id);
//                    private void deleteSquareReply(HttpServletResponse response, String socialgroup_id, String square_item_type, String correspond_id) throws SQLException {

                }else{
                    Response.responseError(response, "delete.java 你不能删除别人发布的内容");

                }
                break;
            default:
                Response.responseError(response, "delete.java 没有这种delete_type");
                break;
        }
    }

    private Boolean testSquareItemIfCorrespondUser(String socialgroup_id, String square_item_type, String correspond_id, String user_id) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        String sql = "SELECT user_id FROM " + square_item_type + " WHERE " + square_item_type + "_id = ?;";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, Integer.parseInt(correspond_id));

        ResultSet rs = stmt.executeQuery();

        if(rs.next()){
            String user_id_db = String.valueOf(rs.getInt("user_id"));
            return user_id_db.equals(user_id);
        }

        stmt.close();
        conn.close();
        return false;
    }

    private Boolean testSquareCommentIfCorrespondUser(String socialgroup_id, String square_item_type, String correspond_id, String user_id) throws SQLException {
        String sql = "SELECT user_id FROM " + square_item_type + "_comment WHERE comment_id = ?;";
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, Integer.parseInt(correspond_id));

        ResultSet rs = stmt.executeQuery();

        if(rs.next()){
            String user_id_db = String.valueOf(rs.getInt("user_id"));
            return user_id_db.equals(user_id);
        }
        stmt.close();
        conn.close();
        return false;
    }

    private Boolean testSquareReplyIfCorrespondUser(String square_item_type, String socialgroup_id, String correspond_id, String user_id) throws SQLException {
        String sql = "SELECT reply_from_user_id FROM " + square_item_type + "_reply WHERE reply_id = ?;";
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, Integer.parseInt(correspond_id));

        ResultSet rs = stmt.executeQuery();

        if(rs.next()){
            String user_id_db = String.valueOf(rs.getInt("reply_from_user_id"));
            if(user_id_db.equals(user_id)){
                return true;
            }
        }
        stmt.close();
        conn.close();
        return false;
    }


    private void deleteSquareItem(HttpServletResponse response, String socialgroup_id, String square_item_type, String correspond_id) throws SQLException {


        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        String sql = "UPDATE " + square_item_type + " SET deleted = 1 WHERE " + square_item_type + "_id = ?;";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, Integer.parseInt(correspond_id));

        Integer result = stmt.executeUpdate();

        if(result == 1){
            Response.responseSuccessInfo(response, "删除squareItem成功");
        }else{
            Response.responseError(response, "删除squareItem失败");
        }

        stmt.close();
        conn.close();
    }

    private void deleteSquareComment(HttpServletResponse response, String socialgroup_id, String square_item_type, String correspond_id) throws SQLException {

        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        String sql = "UPDATE " + square_item_type + "_comment SET deleted = 1 WHERE comment_id = ?;";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, Integer.parseInt(correspond_id));

        int result = stmt.executeUpdate();

        if(result == 1){
            Response.responseSuccessInfo(response, "删除squareComment成功");
        }else{
            Response.responseError(response, "删除squareComment失败");
        }

        stmt.close();
        conn.close();
    }

    private void deleteSquareReply(HttpServletResponse response, String socialgroup_id, String square_item_type, String correspond_id) throws SQLException {

        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        String sql = "UPDATE " + square_item_type + "_reply SET deleted = 1 WHERE reply_id = ?;";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, Integer.parseInt(correspond_id));

        int result = stmt.executeUpdate();

        if(result == 1){
            Response.responseSuccessInfo(response, "删除squareReply成功");
        }else{
            Response.responseError(response, "删除squareReply失败");
        }

        stmt.close();
        conn.close();
    }


}
