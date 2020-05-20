package wall;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@WebServlet("/wall/push_by_user")
public class PushPosterByUser extends HttpServlet {

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

        String socialgroup_id = null;
        int notification_id = 0;

        String brief = null;
        String welcome = null;
        String hold_date = null;
        String hold_location = null;
        String holder = null;
        String detail = null;
        String link = null;
        String user_id = null;
        String password = null;


        if(ServletFileUpload.isMultipartContent(request)){
            try{
                DiskFileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                upload.setFileSizeMax(5 * 1024 * 1024);//单个文件5MB
                upload.setSizeMax(20 * 1024* 1024);//总文件20MB
                upload.setHeaderEncoding("utf-8");
                List<FileItem> list = upload.parseRequest(request);

                for(FileItem item:list){
                    if(item.isFormField()){
                        // 普通的表单类型
                        String fieldName = item.getFieldName();
                        if(fieldName.equals("json")){
                            String jsonStr = item.getString("UTF-8");
                            JSONObject jsonObject = JSONObject.fromObject(jsonStr);

                            socialgroup_id = jsonObject.getString("socialgroup_id");

//                            brief = URLDecoder.decode(jsonObject.getString("brief"), "utf-8");
//                            welcome = URLDecoder.decode(jsonObject.getString("welcome"), "utf-8");
//                            hold_date = URLDecoder.decode(jsonObject.getString("hold_date"), "utf-8");
//                            hold_location = URLDecoder.decode(jsonObject.getString("hold_location"), "utf-8");
//                            holder = URLDecoder.decode(jsonObject.getString("holder"), "utf-8");
//                            detail = URLDecoder.decode(jsonObject.getString("detail"), "utf-8");

                            brief = jsonObject.getString("brief");
                            welcome = jsonObject.getString("welcome");
                            hold_date = jsonObject.getString("hold_date");
                            hold_location = jsonObject.getString("hold_location");
                            holder = jsonObject.getString("holder");
                            detail = jsonObject.getString("detail");


                            link = jsonObject.getString("link");
                            user_id = jsonObject.getString("user_id");
                            password = jsonObject.getString("password");



                            // 验证身份
                            if(TestInfo.testUser_id(user_id) && TestInfo.testPassword(password) && TestInfo.testSocialgroupId(socialgroup_id)){
                                // 验证内容是否有效
                                if(testBrief(brief) && testWelcome(welcome) && testHoldDate(hold_date) && testHoldLocation(hold_location) &&
                                        testHolder(holder) && testDetail(detail) && testLinkWithTimeOut(link, 3000)){

                                    notification_id = publish(response, socialgroup_id,  user_id, brief,
                                            welcome,  hold_date,  hold_location,  holder,  detail,  link);

                                }else{
                                    Response.responseError(response, "pushposter.java 有可能的错误原因：" +
                                            "填写的内容超过了字符长度要求，或者是个无效的url（link）无法打开");
                                }
                            }else{
                                Response.responseError(response, "pushposter.java 用户验证失败");
                            }

                        }else{
                            Response.responseError(response, "pushposter.java fieldName不是json");
                        }

                    }else{
                        //保存图片!!!
                        // 文件表单类型，代表是上传的图片
                        // 假设之前各种验证出了问题，那就应该已经return过了，说明现在其实就不需要验证了已经。
                        if(savePosterPicture(item,  socialgroup_id,  notification_id)){
                            Response.responseSuccessInfo(response, "成功上传Poster");
                        }else{
                            Response.responseError(response, "上传失败，在savePictures(item)里面出现错误");
                        }
                        return;
                    }
                }

            } catch (Exception e) {
                Response.responseError(response, "pushposter.java " + e.toString());
            }
        }else{
            Response.responseError(response, "pushposter.java 不是以multipart/form提交的");
        }
    }




    private Boolean testBrief(String brief){
        return (brief.length() <= 20);
    }

    private Boolean testWelcome(String welcome){
        return (welcome.length() <= 400);
    }

    private Boolean testHoldDate(String holddate){
        return (holddate.length() <= 50);
    }

    private Boolean testHoldLocation(String holdlocation){
        return (holdlocation.length() <= 50);
    }

    private Boolean testHolder(String holder){
        return (holder.length() <= 50);
    }

    private Boolean testDetail(String detail){
        return (detail.length() <= 800);
    }


    private Boolean testLinkWithTimeOut(String urlString, int timeOutMillSeconds){
        URL url;
        try {
            url = new URL(urlString);
            URLConnection co =  url.openConnection();
            co.setConnectTimeout(timeOutMillSeconds);
            co.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

//    private Boolean testUserIfPublisher(String socialgroup_id, String user_id) throws ClassNotFoundException, SQLException {
//        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
//        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
//
//        String sql = "SELECT role FROM user_profile WHERE user_id = ?";
//        PreparedStatement stmt = conn.prepareStatement(sql);
//        stmt.setInt(1, Integer.parseInt(user_id));
//
//        ResultSet rs = stmt.executeQuery();
//        if(rs.next()){
//            // role是1代表是publisher
//            return rs.getString("role").equals("1");
//        }
//        return false;
//    }

    private int publish(HttpServletResponse response, String socialgroup_id, String user_id,String brief,
                        String welcome, String hold_date, String hold_location, String holder, String detail, String link) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

        String user_nickname = null;
        int user_avatar = 0;

        String sqlPre = "SELECT nickname, avatar FROM user_profile WHERE user_id = ?;";
        PreparedStatement stmt = conn.prepareStatement(sqlPre);
        stmt.setInt(1, Integer.parseInt(user_id));

        ResultSet rs = stmt.executeQuery();
        if(rs.next()){
            user_nickname = rs.getString("nickname");
            user_avatar = rs.getInt("avatar");
        }
        rs.close();


        String sql = "INSERT INTO notification(user_id, user_nickname, user_avatar, deleted, type, create_date) VALUE (?, ?, ?, ?, ?, ?)";
        stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

        java.util.Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = format.format(date);

        stmt.setInt(1, Integer.parseInt(user_id));
        stmt.setString(2, user_nickname);
        stmt.setInt(3, user_avatar);

        stmt.setInt(4, 1);
        stmt.setInt(5, 1);
        stmt.setString(6, dateStr);


        int result = stmt.executeUpdate();
        int notification_id = 0;

        if(result == 1){
            //添加到notification完成
            rs = stmt.getGeneratedKeys();
            if(rs.next()){
                notification_id = rs.getInt(1);
            }
            rs.close();
        }else{
            Response.responseError(response, "pushposter.java 插入notification失败");
            return -1;
        }

        sql = "INSERT INTO notification_poster(notification_id, brief, welcome, hold_date, hold_location, " +
                "holder, detail, link) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        stmt = conn.prepareStatement(sql);

        stmt.setInt(1, notification_id);
        stmt.setString(2, brief);
        stmt.setString(3, welcome);
        stmt.setString(4, hold_date);
        stmt.setString(5, hold_location);
        stmt.setString(6, holder);
        stmt.setString(7, detail);
        stmt.setString(8, link);

        result = stmt.executeUpdate();

        stmt.close();
        conn.close();

        if (result == 1) {
            // 添加到notification_poster完成
            return notification_id;
        }else{
            Response.responseError(response, "插入notification_poster失败");
        }
        return -1;
    }


    private Boolean savePosterPicture(FileItem item, String socialgroup_id, int notification_id) throws Exception {
        String fileDirPath = Util.RESOURCE_URL + "socialgroup_" + socialgroup_id + "/wall/poster";
        String fileName = notification_id + ".jpg";

        File file = new File(fileDirPath, fileName);
//        if(!file.exists()){
//            file.createNewFile();
//        }
        if (!file.getParentFile().exists()&&!file.isDirectory()){
            file.getParentFile().mkdirs();
        }
        file.createNewFile();
        item.write(file);
        item.delete();

        // 保存压缩后的图
        ImgCompress compressor = new ImgCompress(fileDirPath + "/" + fileName);
        compressor.resizeFix(1000, 1000, fileDirPath + "/" + fileName);

        File compressedFile = new File(fileDirPath + "/" + fileName);
        FileInputStream compressedFileInputStream = new FileInputStream(compressedFile);
        if(FtpUtil.uploadFileApi("socialgroup_" + socialgroup_id + "/wall/poster",
                fileName, compressedFileInputStream)){
            compressedFile.delete();
            file.delete();
        }
        compressedFileInputStream.close();

        //更新数据对应条目的状态 delete = 0
        return true;
//        return updateStatusInDataBase(socialgroup_id, notification_id);
    }


//    private Boolean updateStatusInDataBase(String socialgroup_id, int notification_id) throws SQLException, ClassNotFoundException {
////        String sql = "UPDATE " + square_item_type + " " +
////                "SET deleted = 0 WHERE " + square_item_type + "_id = " + square_item_id;
//        String sql = "UPDATE notification SET deleted = 0 WHERE notification_id = ?;";
//        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
//        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
//        PreparedStatement stmt = conn.prepareStatement(sql);
//
//        //设置参数
//        stmt.setInt(1, notification_id);
//
//        int result = stmt.executeUpdate();
//
//        //关闭链接
//        stmt.close();
//        conn.close();
//        return result == 1;
//    }

}

