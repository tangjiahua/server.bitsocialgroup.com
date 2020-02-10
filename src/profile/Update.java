package profile;

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
import java.util.List;

@WebServlet("/profile/update")
public class Update extends HttpServlet {

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
        String method = null;
        String user_id = null;
        String password = null;

        // 如果是以multipart/form的方式上传的

        if(ServletFileUpload.isMultipartContent(request)){
            // 如果上传的是以表单类型
            // 1 更改头像
            // 2 更改背景
            // 3 更改照片墙 增加新图
            JSONObject jsonObject = null;



            int new_wall_picture_count = 1; // 如果不是上传照片墙，那么也就没有必要去用这个3个变量
            int wall_picture_count_db = 99;
            int tmpNum = 1;

            try{
                DiskFileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                upload.setFileSizeMax(5 * 1024 * 1024);//单个文件5MB
                upload.setSizeMax(20 * 1024* 1024);//总文件20MB
                upload.setHeaderEncoding("utf-8");
                List<FileItem> list = upload.parseRequest(request);


                for(FileItem item: list){
                    if(item.isFormField()){
                        // 普通的表单类型
                        String fieldName = item.getFieldName();
                        if(fieldName.equals("json")){
                            String jsonStr = item.getString();
                            jsonObject = JSONObject.fromObject(jsonStr);

                            socialgroup_id = jsonObject.getString("socialgroup_id");
                            method = jsonObject.getString("method");
                            user_id = jsonObject.getString("user_id");
                            password = jsonObject.getString("password");

                            if(!TestInfo.testPassword(password) || !TestInfo.testUser_id(user_id) || !TestInfo.testSocialgroupId(socialgroup_id)){
                                //格式不对
                                Response.responseError(response, "Update.java: user_id || pwd || socialgroup_id格式不对");
                                return;
                            }

                            if(!Authenticator.authenticate(user_id, password)){
                                Response.responseError(response, "Update.java : 身份验证失败");
                                return;
                            }

                            //全部验证成功了
                            if(method.equals("4")){
                                // 如果是method = 4 那么就将需要初始化的变量在这里初始化一下
                                new_wall_picture_count = Integer.parseInt(jsonObject.getString("new_wall_picture_count"));
                                String sql = "SELECT wall_picture_count FROM user_profile WHERE user_id = ?;";

                                Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                                Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                                PreparedStatement stmt = conn.prepareStatement(sql);

                                //设置参数
                                stmt.setInt(1, Integer.parseInt(user_id));

                                ResultSet rs = stmt.executeQuery();

                                if(rs.next()){
                                    wall_picture_count_db = rs.getInt("wall_picture_count");
                                }

                                //关闭链接
                                rs.close();
                                stmt.close();
                                conn.close();

                            }
                        }else{
                            Response.responseError(response, "Update.java fieldName为json竟然不是！");
                            return;
                        }
                    }else{
                        //文件表单类型
                        //method: 1 更改头像，一张图片
                        //method: 2 更改背景，一张图片
                        //method: 4 增加背景墙图片，一张到多张

                        assert user_id != null;
                        if(TestInfo.testUser_id(user_id) && TestInfo.testPassword(password) && Authenticator.authenticate(user_id, password)) {
                            switch (method) {
                                case "1": {
                                    //method: 1 更改头像，一张图片

                                    String sql = "SELECT avatar FROM user_profile WHERE user_id = ?;";
                                    Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信

                                    Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

                                    PreparedStatement stmt = conn.prepareStatement(sql);

                                    //设置参数
                                    stmt.setInt(1, Integer.parseInt(user_id));

                                    ResultSet rs = stmt.executeQuery();

                                    if (rs.next()) {

                                        int oldCount = rs.getInt("avatar");
                                        int newCount = oldCount + 1;
                                        String newFileName = user_id + "#" + newCount + ".jpg";
                                        String newAvatarDirPath = Util.RESOURCE_URL + "socialgroup_" + socialgroup_id + "/profile/avatar";
                                        File file = new File(newAvatarDirPath, newFileName);
                                        if (!file.exists()) {
                                            file.createNewFile();
                                        }
                                        item.write(file);
                                        item.delete();
                                        // 保存完毕，现在将头像的thumbnail也保存一下
                                        String newThumbnailDirPath = Util.RESOURCE_URL + "socialgroup_" + socialgroup_id + "/profile/avatar/thumbnail";
                                        ImgCompress compressor = new ImgCompress(newAvatarDirPath + "/" + newFileName);
                                        compressor.resizeFix(150, 150, newThumbnailDirPath + "/" + newFileName);

                                        rs.close();

                                        // 现在将数据插入数据库
                                        String sql2 = "UPDATE user_profile SET avatar = ? WHERE user_id = ?;";

                                        conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                                        stmt = conn.prepareStatement(sql2);

                                        //设置参数
                                        stmt.setInt(1, newCount);
                                        stmt.setInt(2, Integer.parseInt(user_id));

                                        Integer result = stmt.executeUpdate();

                                        if (result != 1) {
                                            Response.responseError(response, "update.java 将数据avatar插入数据库失败");
                                            return;
                                        }

                                        // 所有工作完成
                                        stmt.close();
                                        conn.close();
                                        Response.responseSuccessInfo(response, "成功更换头像");
                                    } else {
                                        Response.responseError(response, "update.java: 更改头像却在user_profile查不到user_id对应的avatar");
                                    }
                                    return;

                                }
                                case "2": {
                                    //method: 2 更改背景，一张图片

                                    String sql = "SELECT background FROM user_profile WHERE user_id = ?;";

                                    Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信

                                    Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接

                                    PreparedStatement stmt = conn.prepareStatement(sql);

                                    //设置参数
                                    stmt.setInt(1, Integer.parseInt(user_id));

                                    ResultSet rs = stmt.executeQuery();

                                    if (rs.next()) {
                                        int oldCount = rs.getInt("background");
                                        int newCount = oldCount + 1;
                                        String newFileName = user_id + "#" + newCount + ".jpg";
                                        String newBackgroundDirPath = Util.RESOURCE_URL + "socialgroup_" + socialgroup_id + "/profile/background";
                                        File file = new File(newBackgroundDirPath, newFileName);
                                        if (!file.exists()) {
                                            file.createNewFile();
                                        }
                                        item.write(file);
                                        item.delete();

                                        // 保存完毕，现在将wall picture压缩一下
                                        ImgCompress compressor = new ImgCompress(newBackgroundDirPath + "/" + newFileName);
                                        compressor.resizeFix(1000, 1000, newBackgroundDirPath + "/" + newFileName);

                                        rs.close();
                                        // 现在将数据插入数据库

                                        String sql2 = "UPDATE user_profile SET background = ? WHERE user_id = ?;";

                                        stmt = conn.prepareStatement(sql2);
                                        stmt.setInt(1, newCount);
                                        stmt.setInt(2, Integer.parseInt(user_id));

                                        Integer result = stmt.executeUpdate();

                                        stmt.close();
                                        conn.close();

                                        if (result != 1) {
                                            Response.responseError(response, "update.java 将数据background插入数据库失败");
                                            return;
                                        }
                                        Response.responseSuccessInfo(response, "成功更换背景");
                                    } else {
                                        Response.responseError(response, "update.java: 更改背景却在user_profile查不到user_id对应的background");
                                    }
                                    return;

                                }
                                case "4":
                                    //method: 4 增加背景墙图片，一张到多张
                                    if (wall_picture_count_db == 99) {
                                        Response.responseError(response, "Update.java 在上传照片到wall时，" +
                                                "搜索出来的wall_picture_count_db的值没有初始化，仍然为99");
                                        return;
                                    }

                                    int tmpInt = wall_picture_count_db + tmpNum;
                                    if(tmpInt > 6){
                                        Response.responseError(response, "照片墙图片数量超过6张了");

                                        return;
                                    }

                                    String newFileName = user_id + "#" + tmpInt + ".jpg";
                                    String newWallDirPath = Util.RESOURCE_URL + "socialgroup_" + socialgroup_id + "/profile/wall";
                                    File file = new File(newWallDirPath, newFileName);
                                    if (!file.exists()) {
                                        file.createNewFile();
                                    }
                                    item.write(file);
                                    item.delete();

                                    // 保存完毕，现在将wall picture压缩一下
                                    ImgCompress compressor = new ImgCompress(newWallDirPath + "/" + newFileName);
                                    compressor.resizeFix(1000, 1000, newWallDirPath + "/" + newFileName);


                                    if (new_wall_picture_count == tmpNum) {
                                        // 代表这是最后一个表单，也就是需要将内容插入数据库了
                                        int newWallPicCountAll = new_wall_picture_count + wall_picture_count_db;

                                        String sql = "UPDATE user_profile SET wall_picture_count = ? WHERE user_id = ?;";
                                        Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                                        Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                                        PreparedStatement stmt = conn.prepareStatement(sql);
                                        stmt.setInt(1, newWallPicCountAll);
                                        stmt.setInt(2, Integer.parseInt(user_id));
                                        Integer result = stmt.executeUpdate();

                                        if (result != 1) {
                                            Response.responseError(response, "update.java 将数据wall_picture_count插入数据库失败");
                                        } else {
                                            // 所有工作完成
                                            Response.responseSuccessInfo(response, "成功上传照片墙");
                                        }
                                        stmt.close();
                                        conn.close();
                                        return;
                                    }
                                    tmpNum = tmpNum + 1;

                                    break;
                                default:
                                    Response.responseError(response, "Update.java:，没有该method");
                                    return;
                            }
                        }else{
                            Response.responseError(response, "身份验证失败或者账号密码格式不正确");
                            return;
                        }
                    }
                }

            } catch (Exception e) {
                Response.responseError(response, e.toString());
            }


        }else{
            // 如果是以application/json类型
            // 1 更改个人资料
            // 2 删除图片

            JSONObject jsonObject = Util.requestToJsonObject(request);
            method = (String)jsonObject.get("method");
            user_id = (String)jsonObject.get("user_id");
            password = (String)jsonObject.get("password");
            try {
                if(TestInfo.testUser_id(user_id) && TestInfo.testPassword(password)){
                    if(Authenticator.authenticate(user_id, password)){
                        //验证成功
                        if(method.equals("3")){
                            // 更新个人资料
                            socialgroup_id = (String)jsonObject.get("socialgroup_id");
                            String sql = "UPDATE user_profile SET nickname = ?, realname = ?, gender = ?, age = ?" +
                                    ", public_introduce = ?, private_introduce = ?, grade = ?, hometown = ?, major = ?, " +
                                    "relationship_status = ?, major = ? WHERE user_id = ?;";

                            Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                            Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                            PreparedStatement stmt = conn.prepareStatement(sql);

                            //设置参数
                            stmt.setString(1, jsonObject.getString("nickname"));
                            stmt.setString(2, jsonObject.getString("realname"));
                            stmt.setString(3, jsonObject.getString("gender"));
                            stmt.setInt(4, jsonObject.getInt("age"));
                            stmt.setString(5, jsonObject.getString("public_introduce"));
                            stmt.setString(6, jsonObject.getString("private_introduce"));
                            stmt.setString(7, jsonObject.getString("grade"));
                            stmt.setString(8, jsonObject.getString("hometown"));
                            stmt.setString(9, jsonObject.getString("major"));
                            stmt.setString(10, jsonObject.getString("relationship_status"));
                            stmt.setString(11, jsonObject.getString("major"));
                            stmt.setInt(12, jsonObject.getInt("user_id"));

                            Integer result = stmt.executeUpdate();

                            if(result == 1){
                                Response.responseSuccessInfo(response, "成功修改资料");
                            }else{
                                Response.responseError(response, "Update.java: 修改资料失败");
                            }
                            stmt.close();
                            conn.close();
                        }else if(method.equals("5")){
                            // 删除图片
                            socialgroup_id = jsonObject.getString("socialgroup_id");
                            int delete = Integer.parseInt(jsonObject.getString("delete"));
                            String sql = "SELECT wall_picture_count FROM user_profile WHERE user_id = ?;";

                            Class.forName(JDBC_DRIVER); //注册JDBC驱动程序，需要初始化驱动程序，这样就可以打开与数据库的通信
                            Connection conn = DriverManager.getConnection(DB_URL + DB_NAME_SG + socialgroup_id, USER, PASS);//创建一个Connection对象，代表数据库的物理连接
                            PreparedStatement stmt = conn.prepareStatement(sql);

                            //设置参数
                            stmt.setInt(1, Integer.parseInt(user_id));

                            ResultSet rs = stmt.executeQuery();

                            if(rs.next()){
                                int pic_count = rs.getInt("wall_picture_count");
                                if(pic_count >= delete){
                                    // 首先将本地的图片命名改变一下
                                    String localUrl = Util.RESOURCE_URL + "socialgroup_" + socialgroup_id + "/profile/wall/";
                                    String deleteFilePath = localUrl + user_id + "#" + delete + ".jpg";
                                    //先删除图片
                                    File deleteFile = new File(deleteFilePath);
                                    deleteFile.delete();
                                    //将其他图片改名
                                    for(int i = delete + 1; i <= pic_count; i++){
                                        String newFilePath = localUrl + user_id + "#" + (i-1) + ".jpg";
                                        String oldFilePath = localUrl + user_id + "#" + i + ".jpg";

                                        File oldFile = new File(oldFilePath);
                                        File newFile = new File(newFilePath);
                                        if(oldFile.renameTo(newFile)){
                                            rs.close();
                                        }else{
                                            Response.responseError(response, "Update.java: 删除照片墙图片时更改命名失败");
                                            rs.close();
                                            return;
                                        }

                                    }
                                    // 然后将pic_count - 1 传回数据库
                                    String updateSql = "UPDATE user_profile SET wall_picture_count = ? WHERE user_id = ?";
                                    stmt = conn.prepareStatement(updateSql);

                                    //设置参数
                                    stmt.setInt(1, pic_count - 1);
                                    stmt.setInt(2, Integer.parseInt(user_id));

                                    int result = stmt.executeUpdate();

                                    if(result == 1){
                                        Response.responseSuccessInfo(response, "删除照片墙图片成功！");
                                    }else{
                                        Response.responseError(response, "Update.java: 修改数据库-1时失败");
                                    }
                                }else{
                                    Response.responseError(response, "Update.java: 删除的图片id大于图片总数");
                                    rs.close();
                                }
                            }else{
                                //没有结果
                                Response.responseError(response, "Update.java 删除图片时查询数据库pic_count没有结果");
                                rs.close();
                            }

                            stmt.close();
                            conn.close();
                        }

                    }else{
                        //验证失败
                        Response.responseError(response, "Update.java: 身份验证失败");
                    }
                }else{
                    Response.responseError(response, "Update.java:  账号或密码格式不对");
                }

            } catch (SQLException | ClassNotFoundException e) {
                Response.responseError(response, e.toString());

            }


        }
    }





}
