package com.fuint.enums;

/**
 * 账户信息枚举
 *
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
public interface AccountEnum {
    /**
     * 账户有效
     */
    public static final int ACCOUNT_VALID     = 1;

    /**
     * 账户无效
     */
    public static final int ACCOUNT_NO_VALID  = 0;

    /**
     * 账户激活
     */
    public static final int ACCOUNT_ACTIVE    = 1;

    /**
     * 账户删除
     */
    public static final int ACCOUNT_DELETE = -1;
}
