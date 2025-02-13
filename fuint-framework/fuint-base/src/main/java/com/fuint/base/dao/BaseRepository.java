package com.fuint.base.dao;

import com.fuint.base.dao.pagination.PaginationRequest;
import com.fuint.base.dao.pagination.PaginationResponse;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 全局扩展repository
 *
 * @author FSQ
 * @version $Id: BaseRepository.java
 */
@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

    /**
     * merge
     *
     * @param obj ： 目标 object
     * @return merger 后的 object
     */
    T merge(T obj);

    /**
     * 根据分页对象进行分页查询
     *
     * @param paginationRequest 分页请求对象
     * @return 分页请求响应结果对象
     */
    @Transactional(readOnly = true)
    PaginationResponse<T> findResultsByPagination(PaginationRequest paginationRequest);

    /**
     * HQL执行语句返回HQL语句对象集合
     *
     * @param hql
     * @return
     */
    List<T> findListByHql(String hql);

    Specification<T> buildSpecification(Map<String, Object> searchParams);

    List<T> findAll(Specification<T> specification);

    List<T> findAll(Specification<T> specification, Sort sort);

    /**
     * 执行SQL语句返回指定对象集合
     *
     * @param entityClass
     * @param sql
     * @return
     */
    List<T> findAllBySql(Class<T> entityClass, String sql);


    /**
     * 批量添加
     *
     * @param list
     */
    public void batchInsert(List<T> list);

    /**
     * 批量删除
     *
     * @param list
     */
    public void batchDelete(List<T> list);

    /**
     * 批量更新
     *
     * @param list
     */
    public void batchUpdate(List<T> list);
}