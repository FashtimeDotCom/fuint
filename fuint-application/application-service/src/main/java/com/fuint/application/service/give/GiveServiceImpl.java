package com.fuint.application.service.give;

import com.fuint.application.enums.StatusEnum;
import com.fuint.application.service.usergrade.UserGradeService;
import com.fuint.base.annoation.OperationServiceLog;
import com.fuint.base.dao.pagination.PaginationRequest;
import com.fuint.base.dao.pagination.PaginationResponse;
import com.fuint.exception.BusinessCheckException;
import com.fuint.application.ResponseObject;
import com.fuint.application.dao.entities.*;
import com.fuint.application.dao.repositories.*;
import com.fuint.application.BaseService;
import com.fuint.application.dto.GiveDto;
import com.fuint.application.service.coupon.CouponService;
import com.fuint.application.service.coupongroup.CouponGroupService;
import com.fuint.application.service.member.MemberService;
import com.fuint.application.service.sms.SendSmsInterface;
import org.apache.commons.collections.MapUtils;
import com.fuint.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.*;

/**
 * 转赠业务实现类
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
@Service
public class GiveServiceImpl extends BaseService implements GiveService {

    private static final Logger log = LoggerFactory.getLogger(GiveServiceImpl.class);

    @Autowired
    private MtGiveRepository giveRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private UserGradeService userGradeService;

    @Autowired
    private SendSmsInterface sendSmsService;

    @Autowired
    private MtUserCouponRepository userCouponRepository;

    @Autowired
    private MtGiveItemRepository giveItemRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponGroupService couponGroupService;

    @PersistenceContext(unitName = "defaultPersistenceUnit")
    private EntityManager entityManager;

    /**
     * 分页查询转赠列表
     *
     * @param paginationRequest
     * @return
     */
    @Override
    public PaginationResponse<GiveDto> queryGiveListByPagination(PaginationRequest paginationRequest) {
        Map<String, Object> params = paginationRequest.getSearchParams();
        Query query = getQueryByParams(params);
        PageRequest pageRequest = new PageRequest(paginationRequest.getCurrentPage() - 1, paginationRequest.getPageSize());
        query.setFirstResult(pageRequest.getOffset());
        query.setMaxResults(pageRequest.getPageSize());
        List<GiveDto> content = this.convert(query);

        Long total = this.getTotal(params);
        Page page = new PageImpl(content, pageRequest, total.longValue());
        PaginationResponse pageResponse = new PaginationResponse(page, MtGive.class);
        pageResponse.setContent(page.getContent());
        pageResponse.setCurrentPage(pageResponse.getCurrentPage() + 1);
        pageResponse.setTotalPages(pageResponse.getTotalPages());

        return pageResponse;
    }

    private List<GiveDto> convert(Query query) {
        List<Object[]> contentObjList = query.getResultList();
        List<GiveDto> content = new ArrayList<>();
        for (Object[] objArray : contentObjList) {
            GiveDto give = new GiveDto();
            give.setId(objArray[0] == null ? 0 : Integer.parseInt(objArray[0].toString()));
            give.setUserId(objArray[1] == null ? 0 : Integer.parseInt(objArray[1].toString()));
            give.setStoreId(objArray[2] == null ? 0 : Integer.parseInt(objArray[2].toString()));
            give.setGiveUserId(objArray[3] == null ? 0 : Integer.parseInt(objArray[3].toString()));
            give.setMobile(objArray[4] == null ? "" : objArray[4].toString());
            give.setUserMobile(objArray[5] == null ? "" : objArray[5].toString());
            give.setGroupIds(objArray[6] == null ? "" : objArray[6].toString());
            give.setGroupNames(objArray[7] == null ? "" : objArray[7].toString());
            give.setCouponIds(objArray[8] == null ? "" : objArray[8].toString());
            give.setCouponNames(objArray[9] == null ? "" : objArray[9].toString());
            give.setNum(objArray[10] == null ? 0 : Integer.parseInt(objArray[10].toString()));
            give.setMoney(objArray[11] == null ? BigDecimal.valueOf(0) : new BigDecimal(objArray[11].toString()));
            give.setNote(objArray[12] == null ? "" : objArray[12].toString());
            give.setMessage(objArray[13] == null ? "" : objArray[13].toString());
            give.setCreateTime(objArray[14] == null ? "" : objArray[14].toString());
            give.setUpdateTime(objArray[15] == null ? "" : objArray[15].toString());
            give.setStatus(objArray[16] == null ? "" : objArray[16].toString());
            content.add(give);
        }
        return content;
    }

    /**
     * 根据参数获取查询Query对象
     *
     * @param params
     * @return
     */
    private Query getQueryByParams(Map<String, Object> params) {
        StringBuffer queryStr = new StringBuffer();
        queryStr.append("SELECT * from mt_give t where t.status='A' ");

        if (params.get("EQ_userMobile") != null && StringUtil.isNotEmpty(params.get("EQ_userMobile").toString())) {
            queryStr.append(" and t.user_mobile like '%" + params.get("EQ_userMobile").toString().trim() + "%' ");
        }

        if (params.get("EQ_mobile") != null && StringUtil.isNotEmpty(params.get("EQ_mobile").toString())) {
            queryStr.append(" AND t.mobile ='" + params.get("EQ_mobile").toString().trim() + "'");
        }

        if (params.get("EQ_userId") != null && StringUtil.isNotEmpty(params.get("EQ_userId").toString())) {
            queryStr.append(" AND t.user_id ='" + params.get("EQ_userId").toString().trim() + "'");
        }

        if (params.get("EQ_storeId") != null && StringUtil.isNotEmpty(params.get("EQ_storeId").toString())) {
            queryStr.append(" AND t.store_id ='" + params.get("EQ_storeId").toString().trim() + "'");
        }

        if (params.get("EQ_giveUserId") != null && StringUtil.isNotEmpty(params.get("EQ_giveUserId").toString())) {
            queryStr.append(" AND t.give_user_id ='" + params.get("EQ_giveUserId").toString().trim() + "'");
        }

        if (params.get("LIKE_groupIds") != null && StringUtil.isNotEmpty(params.get("LIKE_groupIds").toString())) {
            queryStr.append(" AND find_in_set("+params.get("LIKE_groupIds").toString().trim()+", t.group_ids) > 0 ");
        }

        if (params.get("LIKE_groupNames") != null && StringUtil.isNotEmpty(params.get("LIKE_groupNames").toString())) {
            queryStr.append(" and t.group_names like '%" + params.get("LIKE_groupNames").toString().trim() + "%' ");
        }

        if (params.get("LIKE_couponIds") != null && StringUtil.isNotEmpty(params.get("LIKE_couponIds").toString())) {
            queryStr.append(" AND find_in_set("+params.get("LIKE_couponIds").toString().trim()+", t.coupon_ids) > 0 ");
        }

        if (params.get("LIKE_couponNames") != null && StringUtil.isNotEmpty(params.get("LIKE_couponNames").toString())) {
            queryStr.append(" and t.coupon_names like '%" + params.get("LIKE_couponNames").toString().trim() + "%' ");
        }

        queryStr.append(" order by t.id desc");
        return entityManager.createNativeQuery(queryStr.toString());
    }

    /**
     * 获取总数
     *
     * @param params
     * @return
     */
    private Long getTotal(Map<String, Object> params) {
        StringBuffer queryStr = new StringBuffer();
        queryStr.append("select count(DISTINCT(t.id)) from mt_give t " +
                "where t.status='A' ");

        if (params.get("EQ_userMobile") != null && StringUtil.isNotEmpty(params.get("EQ_userMobile").toString())) {
            queryStr.append(" and t.user_mobile like '%" + params.get("EQ_userMobile").toString().trim() + "%' ");
        }

        if (params.get("EQ_mobile") != null && StringUtil.isNotEmpty(params.get("EQ_mobile").toString())) {
            queryStr.append(" AND t.mobile ='" + params.get("EQ_mobile").toString().trim() + "'");
        }

        if (params.get("EQ_userId") != null && StringUtil.isNotEmpty(params.get("EQ_userId").toString())) {
            queryStr.append(" AND t.user_id ='" + params.get("EQ_userId").toString().trim() + "'");
        }

        if (params.get("EQ_storeId") != null && StringUtil.isNotEmpty(params.get("EQ_storeId").toString())) {
            queryStr.append(" AND t.store_id ='" + params.get("EQ_storeId").toString().trim() + "'");
        }

        if (params.get("EQ_giveUserId") != null && StringUtil.isNotEmpty(params.get("EQ_giveUserId").toString())) {
            queryStr.append(" AND t.give_user_id ='" + params.get("EQ_giveUserId").toString().trim() + "'");
        }

        if (params.get("LIKE_groupIds") != null && StringUtil.isNotEmpty(params.get("LIKE_groupIds").toString())) {
            queryStr.append(" AND find_in_set("+params.get("LIKE_groupIds").toString().trim()+", t.group_ids) > 0 ");
        }

        if (params.get("LIKE_groupNames") != null && StringUtil.isNotEmpty(params.get("LIKE_groupNames").toString())) {
            queryStr.append(" and t.group_names like '%" + params.get("LIKE_groupNames").toString().trim() + "%' ");
        }

        if (params.get("LIKE_couponIds") != null && StringUtil.isNotEmpty(params.get("LIKE_couponIds").toString())) {
            queryStr.append(" AND find_in_set("+params.get("LIKE_couponIds").toString().trim()+", t.coupon_ids) > 0 ");
        }

        if (params.get("LIKE_couponNames") != null && StringUtil.isNotEmpty(params.get("LIKE_couponNames").toString())) {
            queryStr.append(" and t.coupon_names like '%" + params.get("LIKE_couponNames").toString().trim() + "%' ");
        }

        Query query = entityManager.createNativeQuery(queryStr.toString());
        Object object = query.getSingleResult();
        if (null != object) {
            return new Long(object.toString());
        }

        return 0L;
    }

    /**
     * 卡券转赠
     *
     * @param paramMap
     * @throws BusinessCheckException
     */
    @Override
    @Transactional
    @OperationServiceLog(description = "转赠卡券")
    public ResponseObject addGive(Map<String, Object> paramMap) throws BusinessCheckException {
        MtGive give = new MtGive();

        String mobile = paramMap.get("mobile") == null ? "" : paramMap.get("mobile").toString();
        String couponId = paramMap.get("couponId") == null ? "" : paramMap.get("couponId").toString();
        String note = paramMap.get("note") == null ? "" : paramMap.get("note").toString();
        String message = paramMap.get("message") == null ? "" : paramMap.get("message").toString();
        Integer userId = paramMap.get("userId") == null ? 0 : (Integer) paramMap.get("userId");
        Integer storeId = paramMap.get("storeId") == null ? 0 : (Integer) paramMap.get("storeId");

        if (StringUtil.isEmpty(mobile) || mobile.length() > 11 || mobile.length() < 11) {
            throw new BusinessCheckException("转增对象手机号有误");
        }

        if (StringUtil.isEmpty(couponId)) {
            throw new BusinessCheckException("转增卡券不能为空");
        }

        String[] couponIds = couponId.split(",");
        if (couponIds.length > 10) {
            throw new BusinessCheckException("转增卡券数量不能超过10张");
        }

        // 如果赠予对象为空，则注册
        MtUser user = memberService.queryMemberByMobile(mobile);
        if (null == user) {
            MtUser userInfo = new MtUser();
            userInfo.setName(mobile);
            userInfo.setMobile(mobile);
            MtUserGrade grade = userGradeService.getInitUserGrade();
            userInfo.setGradeId(grade.getId()+"");
            userInfo.setBalance(new BigDecimal("0"));
            userInfo.setStatus(StatusEnum.ENABLED.getKey());
            user = memberService.addMember(userInfo);
        } else {
            if (!user.getStatus().equals(StatusEnum.ENABLED.getKey())) {
                throw new BusinessCheckException("转增对象可能已被禁用");
            }
        }

        if (null == user) {
            throw new BusinessCheckException("创建转增对象用户信息失败");
        }

        if (user.getId() == userId) {
            throw new BusinessCheckException("转增对象不能是自己");
        }

        BigDecimal money = new BigDecimal(0);
        List<String> couponIdList = new ArrayList<>();
        List<String> couponNames = new ArrayList<>();
        List<String> groupIds = new ArrayList<>();
        List<String> groupNames = new ArrayList<>();

        for (String id : couponIds) {
            MtUserCoupon userCoupon = userCouponRepository.findOne(Integer.parseInt(id));

            MtCoupon coupon = couponService.queryCouponById(userCoupon.getCouponId());

            if (!couponIdList.contains(coupon.getId().toString())) {
                couponIdList.add(coupon.getId().toString());
            }
            if (!couponNames.contains(coupon.getName())) {
                couponNames.add(coupon.getName());
            }

            MtCouponGroup group = couponGroupService.queryCouponGroupById(coupon.getGroupId());
            if (!groupIds.contains(group.getId().toString())) {
                groupIds.add(group.getId().toString());
            }
            if (!groupNames.contains(group.getName())) {
                groupNames.add(group.getName());
            }

            money = money.add(userCoupon.getAmount());
            if (null == userCoupon) {
                throw new BusinessCheckException("转增卡券不存在");
            } else {
                if (!userCoupon.getStatus().equals(StatusEnum.ENABLED.getKey())) {
                    throw new BusinessCheckException("转增卡券必须是未使用状态");
                }
                if (!userCoupon.getUserId().toString().equals(userId.toString())) {
                    throw new BusinessCheckException("您的券可能已经转赠出去了");
                }
            }
        }

        MtUser myUser = memberService.queryMemberById(userId);

        give.setMobile(mobile);
        give.setGiveUserId(userId);
        give.setUserId(user.getId());
        give.setStoreId(storeId);
        give.setMoney(money);
        give.setNum(couponIds.length);
        give.setNote(note);
        give.setMessage(message);
        give.setUserMobile(myUser.getMobile());

        String couponIdsStr = StringUtil.join(couponIdList.toArray(), ",");
        give.setGroupIds(StringUtil.join(groupIds.toArray(), ","));
        give.setGroupNames(StringUtil.join(groupNames.toArray(), ","));
        give.setCouponIds(couponIdsStr);
        give.setCouponNames(StringUtil.join(couponNames.toArray(), ","));

        give.setStatus(StatusEnum.ENABLED.getKey());

        Date createTime = new Date();
        give.setCreateTime(createTime);
        give.setUpdateTime(createTime);

        // 防止网络延迟，检查是否重复
        List<MtGive> uniqueData = giveRepository.queryForUnique(give.getUserId(), give.getGiveUserId(), couponIdsStr, createTime);
        if (uniqueData != null) {
            if (uniqueData.size() > 0) {
                throw new BusinessCheckException("当前网络延迟，不可重复操作");
            }
        }

        MtGive giveInfo = giveRepository.save(give);

        for (String id : couponIds) {
            MtUserCoupon userCoupon = userCouponRepository.findOne(Integer.parseInt(id));
            userCoupon.setUserId(user.getId());
            userCoupon.setUpdateTime(new Date());
            userCoupon.setMobile(user.getMobile());
            userCouponRepository.save(userCoupon);

            MtGiveItem item = new MtGiveItem();
            item.setCreateTime(new Date());
            item.setGiveId(giveInfo.getId());
            item.setStatus("A");
            item.setUpdateTiem(new Date());
            item.setUserCouponId(Integer.parseInt(id));

            giveItemRepository.save(item);
        }

        try {
            List<String> mobileList = new ArrayList<>();
            mobileList.add(mobile);
            Map<String, String> params = new HashMap<>();
            params.put("totalNum", couponIds.length+"");
            params.put("totalMoney", money+"");
            sendSmsService.sendSms("received-coupon", mobileList, params);
        } catch (Exception e) {
            //empty
        }

        return getSuccessResult(giveInfo);
    }

    /**
     * 根据ID获取转赠信息
     *
     * @param id ID
     * @throws BusinessCheckException
     */
    @Override
    public MtGive queryGiveById(Long id) {
        return giveRepository.findOne(id.intValue());
    }

    @Override
    public List<MtGiveItem> queryItemByParams(Map<String, Object> params) throws BusinessCheckException {
        if (MapUtils.isEmpty(params)) {
            params = new HashMap<>();
        }

        Specification<MtGiveItem> specification = giveItemRepository.buildSpecification(params);
        Sort sort = new Sort(Sort.Direction.DESC, "createTime");
        List<MtGiveItem> result = giveItemRepository.findAll(specification, sort);

        return result;
    }
}
