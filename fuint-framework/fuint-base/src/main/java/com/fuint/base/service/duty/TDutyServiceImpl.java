package com.fuint.base.service.duty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.fuint.base.annoation.OperationServiceLog;
import com.fuint.base.dao.entities.*;
import com.fuint.base.dao.pagination.PaginationRequest;
import com.fuint.base.dao.pagination.PaginationResponse;
import com.fuint.base.dao.repositories.TDutyRepository;
import com.fuint.base.dao.repositories.TDutySourceRepository;
import com.fuint.base.service.entities.TreeNode;
import com.fuint.exception.BusinessCheckException;
import com.fuint.util.ArrayUtil;
import com.fuint.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色服务实现类
 *
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
@Service
public class TDutyServiceImpl implements TDutyService {

    @Autowired
    private TDutyRepository tDutyRepository;

    @Autowired
    private TDutySourceRepository tDutySourceRepository;


    public List<TDuty> getAvailableRoles() {
        return tDutyRepository.findByStatus("A");
    }


    public TDuty getRoleById(Long roleId) {
        TDuty htDuty = tDutyRepository.findOne(roleId);
        return htDuty;
    }

    /**
     * 根据ID数组获取角色集合
     *
     * @param ids
     * @return
     */
    public List<TDuty> findDatasByIds(String[] ids) {
        Long[] arrays = new Long[ids.length];
        for (int i = 0; i < ids.length; i++) {
            arrays[i] = Long.parseLong(ids[i]);
        }
        return tDutyRepository.findByIdIn(ArrayUtil.toList(arrays));
    }

    /**
     * 删除方法
     *
     * @param dutyId
     */
    @Override
    @OperationServiceLog(description = "删除后台角色")
    @Transactional
    public void deleteDuty(long dutyId) {
        tDutyRepository.delete(dutyId);
    }

    /**
     * 修改角色
     *
     * @param tduty
     */
    @Transactional(value = "transactionManager")
    @Override
    @OperationServiceLog(description = "更新后台角色")
    public void updateDuty(TDuty tduty, List<TSource> sources) throws BusinessCheckException {
        TDuty existsDuty = this.tDutyRepository.findOne(tduty.getId());
        if (existsDuty == null) {
            throw new BusinessCheckException("角色不存在.");
        }
        if (!StringUtil.equals(tduty.getName(), existsDuty.getName())) {
            TDuty tDuty = this.findByNameAndStatus(tduty.getName(), "A");
            if (tDuty != null) {
                throw new BusinessCheckException("角色名已存在.");
            }
        }
        existsDuty.setDescribe(tduty.getDescribe());
        existsDuty.setDutyType(tduty.getDutyType());
        existsDuty.setName(tduty.getName());
        existsDuty.setStatus(tduty.getStatus());

        if (sources != null && sources.size() > 0) {
            tDutySourceRepository.deleteSourcesByDutyId(tduty.getId());
            Set<TDutySource> dutySources = new HashSet<TDutySource>();
            TDutySource dutySource = null;
            for (TSource tSource : sources) {
                dutySource = new TDutySource();
                dutySource.settDuty(tduty);
                dutySource.settSource(tSource);
                dutySources.add(dutySource);
            }
            existsDuty.settDutySources(dutySources);
        }
        tDutyRepository.merge(existsDuty);
    }

    /**
     * 根据角色名称合状态查询角色
     *
     * @param name
     * @param status
     * @return
     */
    @Override
    public TDuty findByNameAndStatus(String name, String status) {
        return this.tDutyRepository.findByNameAndStatus(name, "A");
    }

    /**
     * 根据角色名称获取已经分配的菜单ID集合
     *
     * @param dutyId
     * @return
     */
    @Override
    public List<Long> getSourceIdsByDutyId(long dutyId) {
        return tDutySourceRepository.findSourceIdsByDutyId(dutyId);
    }

    /**
     * 角色保存方法
     *
     * @param duty
     */
    @Override
    @Transactional
    @OperationServiceLog(description = "编辑后台角色")
    public void saveDuty(TDuty duty, List<TSource> sources) throws BusinessCheckException {
        TDuty existsDuty = this.tDutyRepository.findByNameAndStatus(duty.getName(), "A");
        if (existsDuty != null) {
            throw new BusinessCheckException("角色名称已经存在.");
        }
        if (sources != null && sources.size() > 0) {
            Set<TDutySource> dutySources = new HashSet<TDutySource>();
            TDutySource dutySource = null;
            for (TSource tSource : sources) {
                dutySource = new TDutySource();
                dutySource.settDuty(duty);
                dutySource.settSource(tSource);
                dutySources.add(dutySource);
            }
            duty.settDutySources(dutySources);
        }
        this.tDutyRepository.save(duty);
    }


    @Override
    public PaginationResponse<TDuty> findDutiesByPagination(PaginationRequest paginationRequest) {
        return tDutyRepository.findResultsByPagination(paginationRequest);
    }

    /**
     * 获取菜单的属性结构
     *
     * @return
     */
    @Override
    public List<TreeNode> getDutyTree() {
        List<TDuty> tDuties = this.getAvailableRoles();
        List<TreeNode> trees = new ArrayList<TreeNode>();
        if (tDuties != null && tDuties.size() > 0) {
            TreeNode sourceTreeNode = null;
            for (TDuty tDuty : tDuties) {
                sourceTreeNode = new TreeNode();
                sourceTreeNode.setName(tDuty.getName());
                sourceTreeNode.setId(tDuty.getId());
                sourceTreeNode.setLevel(1);
                sourceTreeNode.setpId(0);
                trees.add(sourceTreeNode);
            }
        }
        return trees;
    }

    /**
     * 根据账户获取角色
     *
     * @param accountId
     * @return
     */
    @Override
    public List<TDuty> findDutiesByAccountId(long accountId) {
        return tDutyRepository.findDutysByIdAccountId(accountId);
    }
}
