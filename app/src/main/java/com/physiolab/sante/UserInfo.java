package com.physiolab.sante;

import java.util.Date;

public class UserInfo {

    private static UserInfo singleton;
    public String name;
    public String height;
    public String weight;
    public String birth;
    public String memo;
    public boolean gender;

    public boolean leadoff = false;
    public Date measureTime;
    public boolean alarm = false;
    public int watchCnt = 0;

    public static UserInfo getInstance() {
        if (singleton == null) {
            singleton = new UserInfo();
        }

        return singleton;
    }
}
