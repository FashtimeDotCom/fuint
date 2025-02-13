/**
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
package com.fuint.base.service.account;

import com.fuint.base.dao.entities.TAccount;
import com.fuint.base.dao.entities.TDuty;
import com.fuint.base.dao.pagination.PaginationRequest;
import com.fuint.base.dao.pagination.PaginationResponse;
import com.fuint.base.shiro.ShiroUser;
import com.fuint.exception.BusinessCheckException;
import java.util.List;

/**
 * 账户接口服务类
 *
 * @author FSQ
 * @version $Id: TAccountService.java
 */
public interface TAccountService {

    /**
     * 根据账户编码获取账户信息
     *
     * @param key 账户编码
     * @return 账户实体信息
     */
    TAccount findAccountByKey(String key);

    /**
     * 根据账户ID获取账户信息
     *
     * @param id
     * @return
     */
    TAccount findAccountById(Long id);

    /**
     * 用户账户信息分页查询
     *
     * @param paginationRequest 分页查询请求对象
     * @return 分页查询结果对象
     */
    PaginationResponse<TAccount> findAccountsByPagination(PaginationRequest paginationRequest);

    /**
     * 添加账户
     *
     * @param htAccount 账户实体
     */
    void addAccount(TAccount htAccount, List<TDuty> duties,String platform) throws BusinessCheckException;

    /**
     * 禁用账户
     *
     * @param accountKey 账户编码
     * @throws BusinessCheckException
     */
    void removeAccount(String accountKey, String isDelete) throws BusinessCheckException;

    /**
     * 根据账户获取账户实体
     *
     * @param accountName 账户
     * @return 账户实体
     */
    TAccount findByAccountName(String accountName);

    /**
     * 修改账户
     *
     * @param tAccount 账户实体
     * @throws BusinessCheckException
     */
    void editAccount(TAccount tAccount, List<TDuty> duties,String platform) throws BusinessCheckException;

    /**
     * 根据账户名称获取账户所分配的角色ID集合
     *
     * @param accountId 账户
     * @return 角色ID集合
     */
    List<Long> getDutyIdsByAccountId(long accountId);

    /**
     * 更新账户
     * @param tAccount
     */
    void updateAccount(TAccount tAccount);

    void entryptPassword(TAccount user);

    String getEntryptPassword(String password, String salt);

    void doLogin(ShiroUser username);

    void logout(ShiroUser username);
}
