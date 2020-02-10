package util;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import net.sf.json.JSONObject;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class Util {

    public static String RESOURCE_URL = "/home/ubuntu/Mask/apache-tomcat-9.0.26/webapps/resource/";




    /**
     * 将请求转换成jsonObject
     * @param request
     * @return
     * @throws IOException
     */
    public static JSONObject requestToJsonObject(HttpServletRequest request) throws IOException {
        // 获取请求的body
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8));
        StringBuffer sb = new StringBuffer("");
        String temp;
        while ((temp = br.readLine()) != null) {
            sb.append(temp);
        }
        br.close();
        // params是json格式的的字符串
        String params = sb.toString();

        return JSONObject.fromObject(params);
    }



//    /**
//     * 通过文件路径直接修改文件名
//     *
//     * @param filePath    需要修改的文件的完整路径
//     * @param newFileName 需要修改的文件的名称
//     * @return
//     */
//    public static String fixFileName(String filePath, String newFileName) {
//        File f = new File(filePath);
//
//        if (!f.exists()) { // 判断原文件是否存在（防止文件名冲突）
//            return null;
//        }
//
//        newFileName = newFileName.trim();
//
//        if ("".equals(newFileName)) // 文件名不能为空
//            return null;
//
//
//        String newFilePath = null;
//
//        if (f.isDirectory()) { // 判断是否为文件夹
//            newFilePath = filePath.substring(0, filePath.lastIndexOf("/")) + "/" + newFileName;
//        } else {
//            newFilePath = filePath.substring(0, filePath.lastIndexOf("/")) + "/" + newFileName
//                    + filePath.substring(filePath.lastIndexOf("."));
//        }
//        File nf = new File(newFilePath);
//        try {
//            f.renameTo(nf); // 修改文件名
//        } catch (Exception err) {
//            err.printStackTrace();
//            return null;
//        }
//        return newFilePath;
//    }


}
