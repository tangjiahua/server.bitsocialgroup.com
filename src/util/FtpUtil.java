package util;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;

/**
 * ftp上传下载工具类
 * <p>Title: FtpUtil</p>
 * <p>Description: </p>
 * @author	张钊
 * @date	2018年7月29日下午8:11:51
 * @version 1.0
 */
public class FtpUtil {

    private static String host = "172.21.0.14";
    private static int port = 21;
    private static String username = "tangjiahua";
    private static String password = "BITsocialgroup.com126";
    private static String basePath = "/";





    /**
     * Description: 向FTP服务器上传文件
     * @param host FTP服务器hostname
     * @param port FTP服务器端口
     * @param username FTP登录账号
     * @param password FTP登录密码
     * @param basePath FTP服务器基础目录
     * @param filePath FTP服务器文件存放路径。例如分日期存放：/2015/01/01。文件的路径为basePath+filePath
     * @param filename 上传到FTP服务器上的文件名
     * @param input 输入流
     * @return 成功返回true，否则返回false
     */
    private static boolean uploadFile(String host, int port, String username, String password, String basePath,
                                     String filePath, String filename, InputStream input) {
        boolean result = false;
        FTPClient ftp = new FTPClient();
        try {
            int reply;
            ftp.connect(host, port);// 连接FTP服务器
            // 如果采用默认端口，可以使用ftp.connect(host)的方式直接连接FTP服务器
            ftp.login(username, password);// 登录
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return result;
            }
            //切换到上传目录
            if (!ftp.changeWorkingDirectory(basePath+filePath)) {
                //如果目录不存在创建目录
                String[] dirs = filePath.split("/");
                String tempPath = basePath;
                for (String dir : dirs) {
                    if (null == dir || "".equals(dir)) continue;
                    tempPath += "/" + dir;
                    if (!ftp.changeWorkingDirectory(tempPath)) {
                        if (!ftp.makeDirectory(tempPath)) {
                            return result;
                        } else {
                            ftp.changeWorkingDirectory(tempPath);
                        }
                    }
                }
            }
            //为了加大上传文件速度，将InputStream转成BufferInputStream
            BufferedInputStream  in = new BufferedInputStream(input);
            //加大缓存区
            ftp.setBufferSize(1024*1024);
//            设置上传文件的类型为二进制类型
            ftp.setControlEncoding("UTF-8"); // 中文支持
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
//            ftp.enterLocalPassiveMode();
            //上传文件

            if (!ftp.storeFile(filename, in)) {
                return result;
            }
            in.close();
            ftp.logout();
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return result;
    }

    /**
     * Description: 从FTP服务器下载文件
     * @param host FTP服务器hostname
     * @param port FTP服务器端口
     * @param username FTP登录账号
     * @param password FTP登录密码
     * @param remotePath FTP服务器上的相对路径
     * @param fileName 要下载的文件名
     * @param localPath 下载后保存到本地的路径
     * @return
     */
//    private static boolean downloadFile(String host, int port, String username, String password, String remotePath,
//                                       String fileName, String localPath) {
//        boolean result = false;
//        FTPClient ftp = new FTPClient();
//        try {
//            int reply;
//            ftp.connect(host, port);
//            // 如果采用默认端口，可以使用ftp.connect(host)的方式直接连接FTP服务器
//            ftp.login(username, password);// 登录
//            reply = ftp.getReplyCode();
//            if (!FTPReply.isPositiveCompletion(reply)) {
//                ftp.disconnect();
//                return result;
//            }
//            ftp.changeWorkingDirectory(remotePath);// 转移到FTP服务器目录
//            FTPFile[] fs = ftp.listFiles();
//            for (FTPFile ff : fs) {
//                if (ff.getName().equals(fileName)) {
//                    File localFile = new File(localPath + "/" + ff.getName());
//
//                    OutputStream is = new FileOutputStream(localFile);
//                    ftp.retrieveFile(ff.getName(), is);
//                    is.close();
//                }
//            }
//
//            ftp.logout();
//            result = true;
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (ftp.isConnected()) {
//                try {
//                    ftp.disconnect();
//                } catch (IOException ioe) {
//                }
//            }
//        }
//        return result;
//    }

    /**
     * 删除FTP上的文件
     */

    private static boolean deleteFile(String host, int port, String username, String password, String basePath,
                                     String filePath, String filename){

        boolean result = false;
        FTPClient ftp = new FTPClient();

        try {
            int reply;
            ftp.connect(host, port);
            // 如果采用默认端口，可以使用ftp.connect(host)的方式直接连接FTP服务器
            ftp.login(username, password);// 登录
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return result;
            }

            //切换到上传目录
            if (!ftp.changeWorkingDirectory(basePath+filePath)) {
                //如果目录不存在创建目录
                String[] dirs = filePath.split("/");
                String tempPath = basePath;
                for (String dir : dirs) {
                    if (null == dir || "".equals(dir)) continue;
                    tempPath += "/" + dir;
                    if (!ftp.changeWorkingDirectory(tempPath)) {
                        if (!ftp.makeDirectory(tempPath)) {
                            return result;
                        } else {
                            ftp.changeWorkingDirectory(tempPath);
                        }
                    }
                }
            }


            if (!ftp.deleteFile(filename)) {
                return result;
            }
            ftp.logout();
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return result;
    }

    /**
     * 改名FTP上的文件
     */

    private static boolean renameFile(String host, int port, String username, String password, String basePath,
                                     String filePath, String filename, String newfilename){

        boolean result = false;
        FTPClient ftp = new FTPClient();

        try {
            int reply;
            ftp.connect(host, port);
            // 如果采用默认端口，可以使用ftp.connect(host)的方式直接连接FTP服务器
            ftp.login(username, password);// 登录
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return result;
            }

            //切换到上传目录
            if (!ftp.changeWorkingDirectory(basePath+filePath)) {
                //如果目录不存在创建目录
                String[] dirs = filePath.split("/");
                String tempPath = basePath;
                for (String dir : dirs) {
                    if (null == dir || "".equals(dir)) continue;
                    tempPath += "/" + dir;
                    if (!ftp.changeWorkingDirectory(tempPath)) {
                        if (!ftp.makeDirectory(tempPath)) {
                            return result;
                        } else {
                            ftp.changeWorkingDirectory(tempPath);
                        }
                    }
                }
            }


            if (!ftp.rename(filename, newfilename)) {
                return result;
            }
            ftp.logout();
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return result;
    }




    public static Boolean uploadFileApi(String filePath, String filename, InputStream input){
        return uploadFile(host, port, username, password, basePath + "resource/", filePath, filename, input);
    }

    public static Boolean renameFileApi(String filePath, String filename, String newfilename){
        return renameFile(host, port, username, password, basePath + "resource/", filePath, filename, newfilename);
    }

    public static Boolean deleteFileApi(String filePath, String filename){
        return deleteFile(host, port, username, password, basePath + "resource/", filePath, filename);
    }



//    public static void main(String[] args) {
//        try {
//            FileInputStream in=new FileInputStream(new File("/Users/thomas/Downloads/zhang.jpg"));
//            boolean flag = uploadFile("172.21.0.14", 21, "tangjiahua", "BITsocialgroup.com126", "/","resource", "zhang.jpg", in);
//            System.out.println(flag);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//    }
}

