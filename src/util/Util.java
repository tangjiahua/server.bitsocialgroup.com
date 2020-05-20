package util;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import net.sf.json.JSONObject;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    // MD5 encrypter
    public static String stringToMD5(String plainText) {
        plainText = plainText + "85d0311c3bfbf17ea4d2b61b8ae2f455";
        byte[] secretBytes = null;
        try {
            secretBytes = MessageDigest.getInstance("md5").digest(
                    plainText.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有这个md5算法！");
        }
        String md5code = new BigInteger(1, secretBytes).toString(16);
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = "0" + md5code;
        }
        return md5code;
    }



}
