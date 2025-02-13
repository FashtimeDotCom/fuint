package com.fuint.base.service.account;

import java.util.*;
import com.fuint.base.annoation.OperationServiceLog;
import com.fuint.base.dao.entities.*;
import com.fuint.base.dao.pagination.PaginationRequest;
import com.fuint.base.dao.pagination.PaginationResponse;
import com.fuint.base.dao.repositories.TAccountDutyRepository;
import com.fuint.base.dao.repositories.TAccountRepository;
import com.fuint.base.dao.repositories.TPlatformRepository;
import com.fuint.base.shiro.ShiroUser;
import com.fuint.enums.AccountEnum;
import com.fuint.exception.BusinessCheckException;
import com.fuint.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账户接口服务实现类
 *
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
@Service
public class TAccountServiceImpl implements TAccountService {

    /**
     * 账户操作DAO
     */
    @Autowired
    private TAccountRepository tAccountRepository;

    /**
     * 账户角色关系DAO
     */
    @Autowired
    private TAccountDutyRepository tAccountDutyRepository;

    /**
     * 平台服务
     */
    @Autowired
    private TPlatformRepository tPlatformRepository;

    @Override
    public TAccount findAccountByKey(String key) {
        return tAccountRepository.findByAccountKey(key);
    }

    /**
     * 根据账户ID获取账户信息
     *
     * @param id
     * @return
     */
    @Override
    public TAccount findAccountById(Long id) {
        return tAccountRepository.findOne(id);
    }

    @Override
    public PaginationResponse<TAccount> findAccountsByPagination(PaginationRequest paginationRequest) {
        Map<String, Object> params = paginationRequest.getSearchParams();
        params.put("NQ_accountStatus", AccountEnum.ACCOUNT_DELETE + "");
        paginationRequest.setSearchParams(params);
        return tAccountRepository.findResultsByPagination(paginationRequest);
    }

    @Override
    @OperationServiceLog(description = "新增后台用户")
    @Transactional
    public void addAccount(TAccount tAccount, List<TDuty> duties, String platform) throws BusinessCheckException {
        this.accountValidation(tAccount);//账户信息有效性检查
        if (StringUtil.isNotBlank(platform)) {
            long platformId = Long.parseLong(platform);
            TPlatform tPlatform = tPlatformRepository.findOne(platformId);
            if (tPlatform == null) {
                throw new BusinessCheckException("平台信息不存在.");
            }
            tAccount.settPlatform(tPlatform);
        } else {
            tAccount.settPlatform(null);
        }
        tAccount.setAccountName(tAccount.getAccountName().toLowerCase());
        tAccount.setAccountKey(this.createAccountKey());
        tAccount.setAccountStatus(AccountEnum.ACCOUNT_VALID);
        tAccount.setIsActive(AccountEnum.ACCOUNT_ACTIVE);
        tAccount.setCreateDate(new Date());
        tAccount.setModifyDate(new Date());
        tAccount.setStoreId(tAccount.getStoreId());
        tAccount.setStaffId(tAccount.getStaffId());
        if (duties != null && duties.size() > 0) {
            Set<TAccountDuty> accountDutySet = new HashSet<TAccountDuty>();
            TAccountDuty tAccountDuty = null;
            for (TDuty tDuty : duties) {
                tAccountDuty = new TAccountDuty();
                tAccountDuty.settDuty(tDuty);
                tAccountDuty.settAccount(tAccount);
                accountDutySet.add(tAccountDuty);
            }
            tAccount.setHtAccountDuties(accountDutySet);
        }
        this.entryptPassword(tAccount);
        tAccountRepository.save(tAccount);
    }

    /**
     * 生成账户编码
     *
     * @return 账户编码
     */
    private String createAccountKey() {
        String year = DateUtil.formatMonth(new Date());//年月
        String time = Long.toString(new Date().getTime());//时间戳
        int random = (int) (Math.random() * (9999 - 1000 + 1)) + 1000;//4位随机
        return year + time + random;
    }

    /**
     * 验证账户信息有效性检查
     *
     * @param sAccount 账户信息实体
     * @throws BusinessCheckException 业务检查异常
     */
    private void accountValidation(TAccount sAccount) throws BusinessCheckException {
        if (sAccount == null) {
            throw new BusinessCheckException("请填写账户信息");
        }
        if (StringUtil.isEmpty(sAccount.getAccountName())) {
            throw new BusinessCheckException("请填写账户名称");
        }
        if (StringUtil.isEmpty(sAccount.getPassword())) {
            throw new BusinessCheckException("请填写密码");
        }
        if (tAccountRepository.findByAccountName(sAccount.getAccountName()) != null) {
            throw new BusinessCheckException(sAccount.getAccountName() + "已经存在");
        }
    }


    @Override
    @OperationServiceLog(description = "禁用用户")
    @Transactional
    public void removeAccount(String accountKey, String isDelete) throws BusinessCheckException {
        if (StringUtil.isBlank(accountKey)) {
            throw new BusinessCheckException("账户编码不能为空");
        }

        TAccount taccount = tAccountRepository.findByAccountKey(accountKey);
        if (taccount == null) {
            throw new BusinessCheckException("账户不存在");
        }

        if (StringUtil.isNotEmpty(isDelete)) {
            taccount.setAccountStatus(AccountEnum.ACCOUNT_DELETE);
        } else {
            taccount.setAccountStatus(AccountEnum.ACCOUNT_NO_VALID);
        }

        this.tAccountRepository.merge(taccount);
    }

    public TAccount findByAccountName(String accountName) {
        return this.tAccountRepository.findByAccountName(accountName);
    }

    /**
     * 修改账户
     *
     * @param tAccount 账户实体
     * @throws BusinessCheckException
     */
    @Transactional
    @Override
    @OperationServiceLog(description = "修改用户")
    public void editAccount(TAccount tAccount, List<TDuty> duties, String platform) throws BusinessCheckException {
        TAccount oldAccount = tAccountRepository.findOne(tAccount.getId());
        if (oldAccount == null) {
            throw new BusinessCheckException("账户不存在.");
        }
        if (StringUtil.isNotBlank(platform)) {
            long platformId = Long.parseLong(platform);
            TPlatform tPlatform = tPlatformRepository.findOne(platformId);
            if (tPlatform == null) {
                throw new BusinessCheckException("平台信息不存在.");
            }
            oldAccount.settPlatform(tPlatform);
        } else {
            oldAccount.settPlatform(null);
        }
        if (StringUtil.isNotBlank(tAccount.getPassword())) {
            entryptPassword(tAccount);
            oldAccount.setPassword(tAccount.getPassword());
            oldAccount.setSalt(tAccount.getSalt());
        }
        oldAccount.setAccountStatus(tAccount.getAccountStatus());
        oldAccount.setIsActive(tAccount.getIsActive());
        oldAccount.setModifyDate(new Date());
        if (duties != null && duties.size() > 0) {
            tAccountDutyRepository.deleteDutiesByAccountId(tAccount.getId());
            Set<TAccountDuty> accountDutySet = new HashSet<TAccountDuty>();
            TAccountDuty tAccountDuty = null;
            for (TDuty tDuty : duties) {
                tAccountDuty = new TAccountDuty();
                tAccountDuty.settDuty(tDuty);
                tAccountDuty.settAccount(oldAccount);
                accountDutySet.add(tAccountDuty);
            }
            oldAccount.setHtAccountDuties(accountDutySet);
        }
        if (StringUtil.isNotEmpty(tAccount.getAccountName())) {
            oldAccount.setAccountName(tAccount.getAccountName());
        }
        oldAccount.setRealName(tAccount.getRealName());
        oldAccount.setStoreId(tAccount.getStoreId());
        oldAccount.setStaffId(tAccount.getStaffId());
        oldAccount.setStoreName(tAccount.getStoreName());
        tAccountRepository.merge(oldAccount);
    }

    /**
     * 根据账户名称获取账户所分配的角色ID集合
     *
     * @param accountId 账户
     * @return 角色ID集合
     */
    @Override
    public List<Long> getDutyIdsByAccountId(long accountId) {
        return tAccountDutyRepository.findDutyIdsByAccountId(accountId);
    }

    /**
     * 更新账户
     *
     * @param tAccount
     */
    @Override
    @Transactional
    public void updateAccount(TAccount tAccount) {
        tAccountRepository.merge(tAccount);
    }

    /**
     * 设定安全的密码，生成随机的salt并经过1024次 sha-1 hash
     */
    @Override
    public void entryptPassword(TAccount user) {
        byte[] salt = Digests.generateSalt(Constant.SaltConstant.SALT_SIZE);
        user.setSalt(Encodes.encodeHex(salt));
        byte[] hashPassword = Digests.sha1(user.getPassword().getBytes(), salt, Constant.SaltConstant.HASH_INTERATIONS);
        user.setPassword(Encodes.encodeHex(hashPassword));
    }

    /**
     * 获取加密密码
     * */
    @Override
    public String getEntryptPassword(String password, String salt) {
        byte[] salt1 = Encodes.decodeHex(salt);
        byte[] hashPassword = Digests.sha1(password.getBytes(), salt1, Constant.SaltConstant.HASH_INTERATIONS);
        return Encodes.encodeHex(hashPassword);
    }

    /**
     * 登录后台
     *
     * @param username
     */
    @Override
    @OperationServiceLog(description = "登录后台系统")
    public void doLogin(ShiroUser username) {
        // empty
    }

    /**
     * 退出登录
     *
     * @param username
     */
    @Override
    @OperationServiceLog(description = "退出后台系统")
    public void logout(ShiroUser username) {
        // empty
    }
}
