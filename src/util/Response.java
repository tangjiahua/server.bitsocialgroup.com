package util;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Response {

    /**
     * 成功的时候返回result=1
     * @param response
     * @param info
     * @return
     */
    public static Boolean responseSuccessInfo(HttpServletResponse response, String info) {
        try{
            response.setContentType("application/json;charset=UTF-8");
            JSONObject responseJson = new JSONObject();
            responseJson.put("result", 1);
            responseJson.put("info", info);
            PrintWriter out = response.getWriter();
            out.println(responseJson);
            return true;
        }catch (IOException e){
            responseError(response, e.toString());
            return false;
        }
    }

    public static Boolean responseSuccessInfo(HttpServletResponse response, JSONObject jsonObject) {
        try{
            response.setContentType("application/json;charset=UTF-8");
            JSONObject responseJson = new JSONObject();
            responseJson.put("result", 1);
            responseJson.put("info", jsonObject);
            PrintWriter out = response.getWriter();
            out.println(responseJson);
            return true;
        }catch (IOException e){
            responseError(response, e.toString());
            return false;
        }
    }

    public static Boolean responseSuccessInfo(HttpServletResponse response, JSONArray jsonArray) {
        try{
            response.setContentType("application/json;charset=UTF-8");
            JSONObject responseJson = new JSONObject();
            responseJson.put("result", 1);
            responseJson.put("info", jsonArray);
            PrintWriter out = response.getWriter();
            out.println(responseJson);
            return true;
        }catch (IOException e){
            responseError(response, e.toString());
            return false;
        }
    }


    /**
     * result为0代表是用户也无法处理的，需要debug
     * @param response
     * @param error
     * @return
     */
    public static Boolean responseError(HttpServletResponse response, String error){
        try{
            response.setContentType("application/json;charset=UTF-8");
            JSONObject responseJson = new JSONObject();
            responseJson.put("result", 0);
            responseJson.put("info", error);
            PrintWriter out = response.getWriter();
            out.println(responseJson);
            return true;
        }catch (IOException e){
            responseError(response, e.toString());
            return false;
        }
    }



    public static void responseAuthenError(HttpServletResponse response){
        try{
            response.setContentType("application/json;charset=UTF-8");
            JSONObject responseJson = new JSONObject();
            responseJson.put("result", 0);
            responseJson.put("info", "身份验证失败");
            PrintWriter out = response.getWriter();
            out.println(responseJson);
        }catch (IOException e){
            responseError(response, e.toString());
        }
    }

    /**
     * 只要result为2那就是用户自己就可以处理的错误
     * @param response
     * @param info
     */
    public static void responseInfo(HttpServletResponse response, String info){
        try{
            response.setContentType("application/json;charset=UTF-8");
            JSONObject responseJson = new JSONObject();
            responseJson.put("result", 2);
            responseJson.put("info", info);
            PrintWriter out = response.getWriter();
            out.println(responseJson);
        }catch (IOException e){
            responseError(response, e.toString());
        }
    }
}
