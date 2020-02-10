package util;

public class TestInfo {

    public static Boolean testUser_id(String user_id){
        String regex = "[0-9]{1,10}";
        return user_id.matches(regex);
    }

    public static Boolean testPassword(String password){
        String regex = "\\w{8,16}";
        return password.matches(regex);
    }

    public static Boolean testAccount(String account){
        String regex = "[0-9]{11}";
        return account.matches(regex);
    }


    public static Boolean testSocialgroupId(String socialgroup_id){
        String regex = "[0-9]{1,10}";
        return socialgroup_id.matches(regex);
    }

}
