package com.fuint.base.service.source;

import com.fuint.base.dao.entities.TSource;
import com.fuint.base.dao.pagination.PaginationRequest;
import com.fuint.base.dao.pagination.PaginationResponse;
import com.fuint.base.service.entities.TreeNode;
import java.util.List;

/**
 * 菜单管理服务接口
 *
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
public interface TSourceService {

    /**
     * 分页数据获取
     *
     * @param paginationRequest
     * @return
     */
    PaginationResponse<TSource> findPlatformByPagination(PaginationRequest paginationRequest);


    /**
     * 添加菜单
     *
     * @param tSource
     */
    void addSource(TSource tSource);

    /**
     * 根据菜单ID查询菜单信息
     *
     * @param id
     * @return
     */
    TSource findSourceById(Long id);

    /**
     * 获取有效的菜单集合
     *
     * @return
     */
    List<TSource> getAvailableSources();

    /**
     * 获取菜单的属性结构
     *
     * @return
     */
    List<TreeNode> getSourceTree();

    /**
     * 修改菜单
     *
     * @param source
     */
    void editSource(TSource source);

    /**
     * 删除菜单
     *
     * @param sourceId
     */
    void deleteSource(long sourceId);

    /**
     * 根据菜单ID集合查询菜单列表信息
     *
     * @param ids
     * @return
     */
    public List<TSource> findDatasByIds(String[] ids);

    /**
     * 根据账户ID查询账户所能访问的source资源列表
     * @param accountId
     * @return
     */
    public List<TSource> findSourcesByAccountId(long accountId);

    /**
     * 根据资源CODE获取资源信息
     *
     * @param sourceCode
     * @return
     */
    public TSource findSourceByCode(String sourceCode);
}
