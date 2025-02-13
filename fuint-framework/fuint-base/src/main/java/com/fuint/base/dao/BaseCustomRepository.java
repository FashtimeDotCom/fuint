package com.fuint.base.dao;

import com.fuint.base.dao.pagination.PaginationRequest;
import com.fuint.base.dao.pagination.PaginationResponse;
import com.fuint.base.util.DynamicSpecifications;
import com.fuint.base.util.SearchFilter;
import com.fuint.util.StringUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局扩展repository实现
 *
 * @author FSQ
 * @version $Id: BaseRepository.java
 */
public class BaseCustomRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID>
        implements
        BaseRepository<T, ID> {

    private static final String DEFAULT_SORT_COLUMN = "id"; //默认排序
    private static final int DEFAULT_PAGE_SIZE = 10;  //默认每页大小

    private final EntityManager entityManager;
    private final Class<T> entityClass;

    private String entityName;

    /**
     * 构造方法
     *
     * @param domainClass
     * @param em
     */
    public BaseCustomRepository(Class<T> domainClass, EntityManager em) {
        super(domainClass, em);
        entityManager = em;
        entityClass = domainClass;
    }

    /**
     * 构造方法
     *
     * @param entityInformation
     * @param entityManager
     */
    public BaseCustomRepository(final JpaEntityInformation<T, ?> entityInformation,
                                final EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityClass = entityInformation.getJavaType();
        this.entityName = entityInformation.getEntityName();
    }

    @Transactional
    @Override
    public T merge(T obj) {
        obj = this.entityManager.merge(obj);
        return obj;
    }


    @Transactional(readOnly = true)
    @Override
    public PaginationResponse<T> findResultsByPagination(PaginationRequest paginationRequest) {
        PageRequest pageRequest = buildPageRequest(paginationRequest);
        Specification<T> spec = buildSpecification(paginationRequest.getSearchParams());
        Page<T> page = findAll(spec, pageRequest);
        PaginationResponse<T> pageResponse = new PaginationResponse<T>(page, entityClass);
        pageResponse.setContent(page.getContent());
        pageResponse.setCurrentPage(pageResponse.getCurrentPage() + 1);//SPRING DATA JPA 分页页码从0开始，因此需要对返回的当前页码进行+1操作
        return pageResponse;
    }

    /**
     * 构建分页请求对象
     *
     * @param pagination 分页信息实体对象
     * @return 分页请求对象
     */
    private PageRequest buildPageRequest(PaginationRequest pagination) {
        Sort sort = null;
        //如果排序字段为null 或者 排序字段为 ‘auto’ 时，默认按照主键进行降序排序
        String[] sortColumn = pagination.getSortColumn();
        if (null == sortColumn || sortColumn.length == 0) {
            sort = new Sort(Direction.DESC, DEFAULT_SORT_COLUMN);
        } else {
            for (int i = 0; i < sortColumn.length; i++) {
                if ("auto".equalsIgnoreCase(sortColumn[i])) {
                    sortColumn[i] = DEFAULT_SORT_COLUMN;
                }
            }
            if (StringUtil.isBlank(pagination.getSortType())) {//如果排序字段不为空，但排序类型为空是默认为降序
                for (String column : sortColumn) {
                    String[] columnType = column.split(" ");
                    if (columnType.length > 1 && StringUtil.equalsIgnoreCase(columnType[1], "ASC")) {
                        if (null == sort) {
                            sort = new Sort(Direction.ASC, columnType[0]);
                        } else {
                            sort = sort.and(new Sort(Direction.ASC, columnType[0]));
                        }
                    } else {
                        if (null == sort) {
                            sort = new Sort(Direction.DESC, columnType[0]);
                        } else {
                            sort = sort.and(new Sort(Direction.DESC, columnType[0]));
                        }
                    }
                }
            } else {
                Direction direction = null;
                if (StringUtil.equals(pagination.getSortType(), "DESC") || StringUtil.equals(pagination.getSortType(), "desc")) {
                    direction = Direction.DESC;
                } else {
                    direction = Direction.ASC;
                }
                sort = new Sort(direction, sortColumn);//如果排序字段和排序类型均不为空，则设置排序属性
            }
        }
        pagination.setCurrentPage(pagination.getCurrentPage() - 1);//SPRING DATA JPA 分页页码从0开始，因此需要对传递的当前页码进行-1操作
        int pageSize = 0;
        if (pagination.getPageSize() == 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        } else {
            pageSize = pagination.getPageSize();
        }
        return new PageRequest(pagination.getCurrentPage(), pageSize, sort);
    }

    /**
     * 创建动态查询条件组合.
     */
    public Specification<T> buildSpecification(Map<String, Object> searchParams) {
        if (searchParams == null) {
            searchParams = new HashMap<String, Object>();
        }
        Map<String, SearchFilter> filters = SearchFilter.parse(searchParams, entityClass);
        Specification<T> spec = DynamicSpecifications.bySearchFilter(filters.values(), entityClass);
        return spec;
    }


    public List<T> findAll(Specification<T> specification) {
        return super.findAll(specification);
    }

    public List<T> findAll(Specification<T> specification, Sort sort) {
        return super.findAll(specification, sort);
    }

    /**
     * HQL执行语句返回HQL语句对象集合
     *
     * @param hql
     * @return
     */
    public List<T> findListByHql(String hql) {
        Query query = this.entityManager.createQuery(hql);
        List<T> result = query.getResultList();
        return result;
    }

    /**
     * 执行SQL语句返回指定对象集合
     *
     * @param entityClass
     * @param sql
     * @return
     */
    public List<T> findAllBySql(Class<T> entityClass, String sql) {
        //创建原生SQL查询QUERY实例,指定了返回的实体类型
        Query query = entityManager.createNativeQuery(sql, entityClass);
        //执行查询，返回的是实体列表,
        List<T> EntityList = query.getResultList();
        return EntityList;
    }


    /**
     * 批量添加
     *
     * @param list
     */
    @Transactional
    @Override
    public void batchInsert(List<T> list) {
        for (int i = 0; i < list.size(); i++) {
            entityManager.persist(list.get(i));
            if (i % 30 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }

    /**
     * 批量删除
     *
     * @param list
     */
    @Transactional
    @Override
    public void batchDelete(List<T> list) {
        for (int i = 0; i < list.size(); i++) {
            entityManager.remove(list.get(i));
            if (i % 30 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }

    /**
     * 批量更新
     *
     * @param list
     */
    @Transactional
    @Override
    public void batchUpdate(List<T> list) {
        for (int i = 0; i < list.size(); i++) {
            entityManager.merge(list.get(i));
            if (i % 30 == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }
}