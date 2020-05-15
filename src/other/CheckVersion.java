package other;

import net.sf.json.JSONObject;
import redis.clients.jedis.Jedis;
import util.*;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;

@WebServlet("/version")
public class CheckVersion extends  HttpServlet {

//    private static String JDBC_DRIVER = Sql.GLOBAL_JDBC_DRIVER;
//    private static String DB_URL = Sql.GLOBAL_DB_URL;
//    private static String DB_NAME_SG = Sql.GLOBAL_DB_NAME_SG;
//    private static String DB_NAME_SGS = Sql.GLOBAL_DB_NAME_SGS;
//    private static String USER = Sql.GLOBAL_USER;
//    private static String PASS = Sql.GLOBAL_PASS;


    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        this.doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException{
        Connection conn = null;


        JSONObject jsonObject = Util.requestToJsonObject(request);

        String device_type = (String)jsonObject.get("device_type");


        //判断所有字符串是否符合规范

        if(device_type.equals("android")){
            Response.responseSuccessInfo(response, "1.0.0");
        }else if(device_type.equals("ios")){
            Response.responseSuccessInfo(response, "1.0.0");
        }



    }
}

