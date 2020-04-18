package square;

import com.sun.deploy.net.HttpRequest;
import com.sun.deploy.net.HttpResponse;
import com.sun.org.apache.xpath.internal.operations.Bool;
import net.sf.json.JSONObject;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@WebServlet("/square/push")
public class PushSquare extends HttpServlet {

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


        if(ServletFileUpload.isMultipartContent(request)){
            // 带图传送
            try {
                pushWithPicture(request, response);
            } catch (Exception e) {
                Response.responseError(response, e.toString());
            }
        }else{
            // 说明是只传送文字
            pushWithoutPicture(request, response);
        }
    }

    /**
     * 处理带图传送的发布
     * @param request
     * @param response
     */
    private void pushWithPicture(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try{
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setFileSizeMax(5 * 1024 * 1024);//单个文件5MB
            upload.setSizeMax(20 * 1024* 1024);//总文件20MB
            upload.setHeaderEncoding("utf-8");
            List<FileItem> list = upload.parseRequest(request);


            JSONObject jsonObject = null;
            String socialgroup_id = null;
            String square_item_type = null;
            String title = null;
            String content = null;
            String user_id = null;
            String password = null;


            int tmpNum = 1;  //保存图片的时候使用的临时变量
            int square_item_id = 0;  //插入数据库后获取到的自增长列的id
            int image_count = 0;

            for(FileItem item: list){
                if(item.isFormField()){
                    // 普通的表单类型，代表是json格式的字符串
                    String fieldName = item.getFieldName();
                    if(fieldName.equals("json")){
                        String jsonStr = item.getString("UTF-8");

                        jsonObject = JSONObject.fromObject(jsonStr);

                        socialgroup_id = jsonObject.getString("socialgroup_id");
                        square_item_type = jsonObject.getString("square_item_type");


                        image_count = Integer.parseInt(jsonObject.getString("image_count"));
                        if(image_count > 9 || image_count < 1){
                            Response.responseError(response, "超出了9张图片的限制");
                            return;
                        }
                        user_id = jsonObject.getString("user_id");
                        password = jsonObject.getString("password");

                        if(TestInfo.testPassword(password) &&
                                TestInfo.testUser_id(user_id) &&
                                TestInfo.testSocialgroupId(socialgroup_id) && Authenticator.authenticate(user_id, password)){
                            if(square_item_type.equals("broadcast")){
                                title = jsonObject.getString("title");
                                content = jsonObject.getString("content");
                            }else if(square_item_type.equals("circle")){
                                content = jsonObject.getString("content");
                            }else{
                                Response.responseError(response, "push.java: square_item_type不对");
                                return;
                            }
                            // 将获取到的信息插入数据库，并且是delete=1，目的是保证不会因为图片没有上传完毕就把该条目插入了数据库
                            // 因为如果网络突然断开的话，会出现服务器上找不到对应图片，但数据库中这条帖子是有图片的，这样的情况
//                            String square_item_type, String socialgroup_id, String user_id, String title, String content, int image_count
                            square_item_id = insertIntoDataBase(square_item_type, socialgroup_id, user_id, title, content, image_count);

                        }else{
                            Response.responseError(response, "push.java: 账号或密码格式不对，或者身份验证失败");
                            return;
                        }
                    }else{
                        Response.responseError(response, "Push.java fieldName != json");
                        return;
                    }

                }else{
                    // 文件表单类型，代表是上传的图片
                    // 假设之前各种验证出了问题，那就应该已经return过了，说明现在其实就不需要验证了已经。
                    if(tmpNum <= 9){
                        if(savePictures(item, square_item_id, tmpNum, socialgroup_id, square_item_type)){
                            if(tmpNum == image_count){
                                if(updateStatusInDataBase(response, tmpNum, image_count, square_item_type, socialgroup_id, square_item_id)){
                                    Response.responseSuccessInfo(response, "成功上传");
                                    return;
                                }
                            }
                            tmpNum = tmpNum + 1;
                        }
                    }else{
                        Response.responseError(response, "超出了9张图片");
                        return;
                    }



                }

            }

        } catch (FileUploadException | SQLException | ClassNotFoundException e) {
            Response.responseError(response, e.toString());
        }
    }


    /**
     * 保存图片
     * @param item
     * @return
     * @throws Exception
     */
    private Boolean savePictures(FileItem item, int square_item_id, int tmpNum, String socialgroup_id, String square_item_type) throws Exception {



        String fileName = square_item_id + "@" + tmpNum + ".jpg";
        String fileDirPath = Util.RESOURCE_URL + "socialgroup_"
                + socialgroup_id + "/square/" + square_item_type + "/picture";
        File file = new File(fileDirPath, fileName);
        if(!file.exists()){
            file.createNewFile();
        }
        item.write(file);
        item.delete();

        // 保存缩略图
        String thumbnailFileDirPath = Util.RESOURCE_URL + "socialgroup_"
                + socialgroup_id + "/square/" + square_item_type + "/thumbnail";
        ImgCompress compressor = new ImgCompress(fileDirPath + "/" + fileName);
        compressor.resizeFix(600, 600, thumbnailFileDirPath + "/" + fileName);

        //更新数据对应条目的状态 delete = 0
        return true;
    }

    /**
     * 最后一项工作，将数据库中对应的deleted改为0
     * @return
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private Boolean updateStatusInDataBase(HttpServletResponse response, int tmpNum, int image_count, String square_item_type, String socialgroup_id, int square_item_id) throws SQLException, ClassNotFoundException {
//        String sql = "UPDATE " + square_item_type + " " +
//                "SET deleted = 0 WHERE " + square_item_type + "_id = " + square_item_id;
        if(tmpNum != image_count){
            Response.responseError(response, "iamge_count和传入的图片数量不匹配");
            return false;
        }

        String sql = "UPDATE " + square_item_type + " SET deleted = 0 WHERE " + square_item_type + "_id = ?;";
        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        PreparedStatement stmt = conn.prepareStatement(sql);

        //设置参数
        stmt.setInt(1, Integer.parseInt(String.valueOf(square_item_id)));


        int result = stmt.executeUpdate();
        //关闭链接
        stmt.close();
        conn.close();
        return result == 1;
    }

    /**
     * 将数据插入数据库，但是注意deleted为1
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private int insertIntoDataBase(String square_item_type, String socialgroup_id, String user_id, String title, String content, int image_count) throws SQLException, ClassNotFoundException {
        if(square_item_type.equals("broadcast")){
            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateStr = format.format(date);

//            String sql = "INSERT INTO broadcast(user_id, type, deleted, title, content, create_date)" +
//                    " VALUES (" + user_id + ", 1, 1, '" + title + "', '" + content + "', " + dateStr + ")";
            String sql = "INSERT INTO broadcast(user_id, type, deleted, title, content, create_date, picture_count) VALUES" +
                    "(?, 1, 1, ?, ?, ?, ?);";


            Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
            Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
            PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);//创建一个Statement或者PreparedStatement对象

            stmt.setInt(1, Integer.parseInt(user_id));
            stmt.setString(2, title);
            stmt.setString(3, content);
            stmt.setString(4, dateStr);
            stmt.setInt(5, image_count);

            int tmpInt = stmt.executeUpdate();

            int square_item_id = 0;

            if(tmpInt == 1){
                ResultSet rs = stmt.getGeneratedKeys();
                if(rs.next()){
                    square_item_id = rs.getInt(1);
                }

                //关闭链接
                stmt.close();
                conn.close();
                rs.close();
                return square_item_id;
            }
        }else if(square_item_type.equals("circle")){
            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String dateStr = format.format(date);

            String sql = "INSERT INTO circle(user_id, type, deleted, content, create_date, picture_count) VALUES" +
                    "(?, 1, 1, ?, ?, ?);";
//            String sql = "INSERT INTO circle(user_id, type, deleted, content, create_date)" +
//                    " VALUES (" + user_id + ", 1, 1, '" + content + "', " + dateStr + ")";
            Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
            Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
            PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);//创建一个Statement或者PreparedStatement对象

            stmt.setInt(1, Integer.parseInt(user_id));
            stmt.setString(2, content);
            stmt.setString(3, dateStr);
            stmt.setInt(4, image_count);

            int tmpInt = stmt.executeUpdate();
            int square_item_id = 0;
            if(tmpInt == 1){
                ResultSet rs = stmt.getGeneratedKeys();
                if(rs.next()){
                    square_item_id = rs.getInt(1);
                }
                //关闭链接
                stmt.close();
                conn.close();
                rs.close();
                return square_item_id;
            }
        }
        // 前面已经处理过非broadcast和circle的square_type了
        // 所以这里不需要写什么else了
        return -1;
    }

    /**
     * 处理不带图传送的发布
     * @param request
     * @param response
     * @throws IOException
     */
    private void pushWithoutPicture(HttpServletRequest request, HttpServletResponse response) throws IOException {


        JSONObject jsonObject = Util.requestToJsonObject(request);

        String socialgroup_id = jsonObject.getString("socialgroup_id");
        String square_item_type = jsonObject.getString("square_item_type");

        String user_id = jsonObject.getString("user_id");
        String password = jsonObject.getString("password");


        // 判断
        try {
            if(TestInfo.testUser_id(user_id) && TestInfo.testPassword(password) && Authenticator.authenticate(user_id, password)){
                // 验证通过
                if(square_item_type.equals("broadcast")){
                    // 代表是broadcast类型的数据
                    pushWithoutPictureBroadcast(response, jsonObject, socialgroup_id, user_id);

                }else if(square_item_type.equals("circle")){
                    // 代表是circle类型的数据
                    pushWithoutPictureCircle(response, jsonObject, socialgroup_id, user_id);

                }else{
                    Response.responseError(response, "Push.java: 没有这种push方法");
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            Response.responseError(response, e.toString());
        }
    }

    /**
     * 发布不带图帖子 是broadcast类型
     * @param response
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private void pushWithoutPictureBroadcast(HttpServletResponse response, JSONObject jsonObject, String socialgroup_id, String user_id) throws SQLException, ClassNotFoundException {
        String title = jsonObject.getString("title");
        String content = jsonObject.getString("content");
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = format.format(date);

        String sql = "INSERT INTO broadcast(user_id, type, title, content, create_date) VALUES" +
                "(?, 1, ?, ?, ?);";

        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        PreparedStatement stmt = conn.prepareStatement(sql);

        stmt.setInt(1, Integer.parseInt(user_id));
        stmt.setString(2, title);
        stmt.setString(3, content);
        stmt.setString(4, dateStr);

        int result = stmt.executeUpdate();

        if(result == 1){
            Response.responseSuccessInfo(response, "成功发布braodcast");
        }else{
            Response.responseError(response, "插入数据库失败broadcast");
        }

        //关闭链接
        stmt.close();
        conn.close();
    }

    /**
     * 发布不带图帖子，是circle类型
     * @param response
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private void pushWithoutPictureCircle(HttpServletResponse response, JSONObject jsonObject, String socialgroup_id, String user_id) throws SQLException, ClassNotFoundException {
        String content = jsonObject.getString("content");
        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = format.format(date);


        String sql = "INSERT INTO circle(user_id, type, content, create_date) VALUES" +
                "(?, 1, ?, ?);";

        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
        PreparedStatement stmt = conn.prepareStatement(sql);

        //参数
        stmt.setInt(1, Integer.parseInt(user_id));
        stmt.setString(2, content);
        stmt.setString(3, dateStr);

        int result = stmt.executeUpdate();

        if(result == 1){
            Response.responseSuccessInfo(response, "成功发布circle");
        }else{
            Response.responseError(response, "插入数据库失败circle");
        }

        //关闭链接
        stmt.close();
        conn.close();

    }
}
