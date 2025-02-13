package com.fuint.base.util;

import com.fuint.base.dao.pagination.PaginationRequest;
import com.fuint.exception.BusinessRuntimeException;
import com.fuint.util.DateUtil;
import com.fuint.util.StringUtil;
import org.slf4j.Logger;
import org.springframework.ui.Model;
import org.springframework.web.servlet.support.RequestContextUtils;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

/**
 * 用户请求处理类
 *
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
public class RequestHandler {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(RequestHandler.class);
    /**
     * 当前页码
     */
    private static final String CURRENT_PAGE = "current_page";
    /**
     * 每页大小
     */
    private static final String PAGE_SIZE = "page_size";
    /**
     * 默认页码
     */
    private static final int DEFAULT_PAGE_NUM = 1;
    /**
     * 默认每页大小
     */
    private static final int DEFAULT_PAGE_SIZE = 10;
    /**
     * 查询标签
     */
    private static final String[] QUERY_TAG = {"EQ_", "LIKE_", "GT_", "LT_", "GTE_",
            "LTE_","NQ_"};

    /**
     *  构建分页对象
     *
     * @param request Http Request 请求
     * @return 分页请求对象
     */
    public static PaginationRequest buildPaginationRequest(final HttpServletRequest request,
                                                           final Model model) {
        PaginationRequest paginationRequest = new PaginationRequest();
        String currentPage = (StringUtil.equals("",request.getParameter(CURRENT_PAGE))||StringUtil.equals("null",request.getParameter(CURRENT_PAGE)))?"1":request.getParameter(CURRENT_PAGE);
        String pageSize = request.getParameter(PAGE_SIZE);
        if (StringUtil.isBlank(currentPage)) {
            paginationRequest.setCurrentPage(DEFAULT_PAGE_NUM);
        } else {
            paginationRequest.setCurrentPage(Integer.parseInt(currentPage));
        }
        if (StringUtil.isBlank(pageSize)) {
            paginationRequest.setPageSize(DEFAULT_PAGE_SIZE);
        } else {
            paginationRequest.setPageSize(Integer.parseInt(pageSize));
        }
        paginationRequest.setSearchParams(requestParams(request, model));
        return paginationRequest;
    }

    /**
     * 构建请求参数集合
     *
     * @param request Http Request 请求
     * @return 请求参数集合
     */
    public static final Map<String, Object> requestParams(final HttpServletRequest request, final Model model) {
        Enumeration<String> parameterNames = request.getParameterNames();
        Map<String, Object> params = new HashMap<String, Object>();
        while (parameterNames.hasMoreElements()) {
            String name = StringUtil.trim(parameterNames.nextElement().toString());
            boolean isHandle = false;
            for (String tag : QUERY_TAG) {
                if (StringUtil.isNotBlank(name) && name.startsWith(tag)) {
                    isHandle = true;
                    break;
                }
            }
            if (isHandle) {
                String value = StringUtil.trim(request.getParameter(name));
                if (StringUtil.isNotBlank(value)) {
                    params.put(name, value);
                    if (StringUtil.indexOf(name, ".") > -1) {
                        name = StringUtil.replace(name, ".", "_");
                    }
                    model.addAttribute(name, value);
                }
            }
        }
        Map<String, ?> map = RequestContextUtils.getInputFlashMap(request);
        if (map != null && map.size() > 0) {
            Set<String> keySet = map.keySet();
            Iterator<String> iterator = keySet.iterator();
            String key = "";
            while (iterator.hasNext()) {
                model.addAttribute(key, map.get(key));
                params.put(key, map.get(key));
            }
        }
        return params;
    }

    /**
     * 根据request请求的参数构建指定的object对象，并设置相关属性值
     *
     * @param request HttpServletRequest 请求
     * @param obj     等构建的对象
     * @return 属性设置后的对象
     */
    public static final Object createBean(final HttpServletRequest request, Object obj) {
        if (obj != null) {
            Enumeration<String> parameterNames = request.getParameterNames();
            //解析待提交的表单数据
            Map<String, String> params = new HashMap<String, String>();
            while (parameterNames.hasMoreElements()) {
                String name = StringUtil.trim(parameterNames.nextElement().toString());
                String value = StringUtil.trim(request.getParameter(name));
                if (StringUtil.isNotBlank(value)) {
                    params.put(name, StringUtil.trim(value));
                }
            }
            //反射设置属性
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field fd : fields) {
                //判断提交的表单属性是否存在于对象的属性列表种
                if (params.get(fd.getName()) == null) {
                    continue;
                }
                try {
                    fd.setAccessible(true); //设置些属性是可以访问的  
                    String type = fd.getType().toString();//得到此属性的类型
                    logger.info("表单赋值，类型为:{},值为:{}",type,params.get(fd.getName()));
                    if (type.endsWith("String")) {
                        fd.set(obj, params.get(fd.getName())); //给属性设值  
                    } else if (type.endsWith("int") || type.endsWith("Integer")) {
                        fd.set(obj, Integer.parseInt(params.get(fd.getName()))); //给属性设值  
                    } else if (type.endsWith("date")|| type.endsWith("Date")) {
                        fd.set(obj, DateUtil.parseDateNewFormat(params.get(fd.getName()))); //给属性设值
                    } else if (type.endsWith("long") || type.endsWith("Long")) {
                        fd.set(obj, Long.parseLong(params.get(fd.getName()))); //给属性设值
                    }else if (type.endsWith("BigDecimal")) {
                        fd.set(obj, new BigDecimal(params.get(fd.getName()))); //给属性设值
                    } else {
                        fd.set(obj, params.get(fd.getName())); //给属性设值
                    }
                } catch (Exception e) {
                    logger.error("表单赋值错误，类型不匹配:"+e);
                    e.printStackTrace();
                    throw new BusinessRuntimeException("表单赋值错误，类型不匹配");
                }
            }
        }
        return obj;
    }
}
