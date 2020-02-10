package util;

public class Sql {


    private static final long serialVersionUID = 1L;//serialVersionUID作用：序列化时为了保持版本的兼容性，即在版本升级时反序列化仍保持对象的唯一性。
    public static final String GLOBAL_JDBC_DRIVER = "com.mysql.jdbc.Driver";
    public static final String GLOBAL_DB_URL = "jdbc:mysql://localhost:3306/";
    public static final String GLOBAL_DB_NAME_SG = "socialgroup_";
    public static final String GLOBAL_DB_NAME_SGS = "socialgroup_system";
    public static final String GLOBAL_USER = "tangjiahua";
    public static final String GLOBAL_PASS = "tangjiahua";

}
