package util;

import com.sun.deploy.net.HttpResponse;
import net.sf.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PushMessage {

    private static String JDBC_DRIVER = Sql.GLOBAL_JDBC_DRIVER;
    private static String DB_URL = Sql.GLOBAL_DB_URL;
    private static String DB_NAME_SG = Sql.GLOBAL_DB_NAME_SG;
    private static String DB_NAME_SGS = Sql.GLOBAL_DB_NAME_SGS;
    private static String USER = Sql.GLOBAL_USER;
    private static String PASS = Sql.GLOBAL_PASS;



    /**
     * 戳一戳之后 发送远程推送
     * @param user_id
     * @param stick_to_user_id
     * @param socialgroup_id
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static void pushStickMessage(String user_id, String stick_to_user_id,  String socialgroup_id) throws SQLException, ClassNotFoundException {

        java.util.Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = format.format(date);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user_id", user_id);
        String push_message_sql = "INSERT INTO push_message(type, user_id, content, create_date) VALUES (?, ?, ?, ?);";

        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信

        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        PreparedStatement stmt = conn.prepareStatement(push_message_sql);

        //设置参数
        stmt.setInt(1, 3);
        stmt.setInt(2, Integer.parseInt(stick_to_user_id));
        stmt.setString(3, jsonObject.toString());
        stmt.setString(4, dateStr);

        //执行
        int result = stmt.executeUpdate();



        //关闭链接
        stmt.close();
        conn.close();
    }


    public static void pushCommentMessage(String socialgroup_id, String user_id, String square_item_type, String square_item_id) throws ClassNotFoundException, SQLException {
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = format.format(date);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("square_item_type", square_item_type);
        jsonObject.put("square_item_id", square_item_id);

        String push_message_sql = "INSERT INTO push_message(type, user_id, content, create_date) VALUES (?, ?, ?, ?);";

        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        PreparedStatement stmt = conn.prepareStatement(push_message_sql);

        //设置参数
        stmt.setInt(1, 1);
        stmt.setInt(2, Integer.parseInt(user_id));
        stmt.setString(3, jsonObject.toString());
        stmt.setString(4, dateStr);

        //执行
        int result = stmt.executeUpdate();


        //关闭链接
        stmt.close();
        conn.close();
    }

    public static void pushReplyMessage(String socialgroup_id, String user_id, String square_item_type, String square_item_id) throws ClassNotFoundException, SQLException {

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = format.format(date);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("square_item_type", square_item_type);
        jsonObject.put("square_item_id", square_item_id);

        String push_message_sql = "INSERT INTO push_message(type, user_id, content, create_date) VALUES (?, ?, ?, ?);";

        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        PreparedStatement stmt = conn.prepareStatement(push_message_sql);

        //设置参数
        stmt.setInt(1, 2);
        stmt.setInt(2, Integer.parseInt(user_id));
        stmt.setString(3, jsonObject.toString());
        stmt.setString(4, dateStr);

        //执行
        int result = stmt.executeUpdate();


        //关闭链接
        stmt.close();
        conn.close();
    }

    public static void pushAdminMessage(){

    }


}
