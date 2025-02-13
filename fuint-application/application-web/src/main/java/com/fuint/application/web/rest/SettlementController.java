package com.fuint.application.web.rest;

import com.fuint.application.PropertiesUtil;
import com.fuint.application.dao.entities.*;
import com.fuint.application.dao.repositories.MtOrderRepository;
import com.fuint.application.dto.OrderDto;
import com.fuint.application.enums.*;
import com.fuint.application.service.balance.BalanceService;
import com.fuint.application.service.coupon.CouponService;
import com.fuint.application.service.order.OrderService;
import com.fuint.application.service.member.MemberService;
import com.fuint.application.service.setting.SettingService;
import com.fuint.application.service.token.TokenService;
import com.fuint.application.service.usercoupon.UserCouponService;
import com.fuint.application.service.weixin.WeixinService;
import com.fuint.application.service.usergrade.UserGradeService;
import com.fuint.application.util.CommonUtil;
import com.fuint.application.util.DateUtil;
import com.fuint.util.StringUtil;
import com.fuint.base.shiro.util.ShiroUserHelper;
import com.fuint.exception.BusinessCheckException;
import com.fuint.application.ResponseObject;
import com.fuint.application.BaseController;
import com.fuint.base.shiro.ShiroUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;

/**
 * 结算中心接口
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
@RestController
@RequestMapping(value = "/rest/settlement")
public class SettlementController extends BaseController {

    /**
     * Token服务接口
     */
    @Autowired
    private TokenService tokenService;

    /**
     * 会员服务接口
     * */
    @Autowired
    private MemberService memberService;

    /**
     * 订单服务接口
     * */
    @Autowired
    private OrderService orderService;

    /**
     * 卡券服务接口
     */
    @Autowired
    private CouponService couponService;

    /**
     * 微信服务接口
     * */
    @Autowired
    private WeixinService weixinService;

    /**
     * 会员等级接口
     * */
    @Autowired UserGradeService userGradeService;

    /**
     * 会员卡券服务
     * */
    @Autowired
    private UserCouponService userCouponService;

    /**
     * 配置服务接口
     * */
    @Autowired
    private SettingService settingService;

    /**
     * 余额服务接口
     * */
    @Autowired
    private BalanceService balanceService;

    @Autowired
    private MtOrderRepository orderRepository;

    /**
     * 结算提交
     */
    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    @CrossOrigin
    public ResponseObject submit(HttpServletRequest request, @RequestBody Map<String, Object> param) throws BusinessCheckException {
        String token = request.getHeader("Access-Token");
        MtUser userInfo = tokenService.getUserInfoByToken(token);

        String operator = null;
        ShiroUser shiroUser = ShiroUserHelper.getCurrentShiroUser();
        if (shiroUser != null) {
            operator = ShiroUserHelper.getCurrentShiroUser().getAcctName();
            if (userInfo == null) {
                userInfo = memberService.getCurrentUserInfo(0);
            }
        }

        // 后台操作自动注册会员信息
        if ((userInfo == null || StringUtil.isEmpty(token))) {
            String mobile = param.get("mobile") == null ? "" : param.get("mobile").toString();
            if (StringUtil.isNotEmpty(operator) && StringUtil.isNotEmpty(mobile)) {
                userInfo = memberService.queryMemberByMobile(mobile);
                // 自动注册会员
                if (userInfo == null) {
                    userInfo = memberService.addMemberByMobile(mobile);
                }
            }
        }

        if (userInfo == null) {
            return getFailureResult(1001);
        }

        String cartIds = param.get("cartIds") == null ? "" : param.get("cartIds").toString();
        Integer targetId = param.get("targetId") == null ? 0 : Integer.parseInt(param.get("targetId").toString()); // 预存卡、升级等级必填
        String selectNum = param.get("selectNum") == null ? "" : param.get("selectNum").toString(); // 预存卡必填
        String remark = param.get("remark") == null ? "" : param.get("remark").toString();
        String type = param.get("type") == null ? "" : param.get("type").toString(); // 订单类型
        String payAmount = param.get("payAmount") == null ? "0" : StringUtil.isEmpty(param.get("payAmount").toString()) ? "0" : param.get("payAmount").toString(); // 支付金额
        Integer usePoint = param.get("usePoint") == null ? 0 : Integer.parseInt(param.get("usePoint").toString()); // 使用积分数量
        Integer couponId = param.get("couponId") == null ? 0 : Integer.parseInt(param.get("couponId").toString());
        String payType = param.get("payType") == null ? PayTypeEnum.JSAPI.getKey() : param.get("payType").toString();
        String authCode = param.get("authCode") == null ? "" : param.get("authCode").toString();
        Integer storeId = request.getHeader("storeId") == null ? 0 : Integer.parseInt(request.getHeader("storeId"));
        Integer userId = param.get("userId") == null ? 0 : (StringUtil.isNotEmpty(param.get("userId").toString()) ? Integer.parseInt(param.get("userId").toString()) : 0); // 指定下单会员 eg:收银功能
        String cashierPayAmount = param.get("cashierPayAmount") == null ? "" : param.get("cashierPayAmount").toString(); // 收银台实付金额
        String cashierDiscountAmount = param.get("cashierDiscountAmount") == null ? "" : param.get("cashierDiscountAmount").toString(); // 收银台优惠金额
        Integer goodsId = param.get("goodsId") == null ? 0 : Integer.parseInt(param.get("goodsId").toString()); // 立即购买商品ID
        Integer skuId = param.get("skuId") == null ? 0 : Integer.parseInt(param.get("skuId").toString()); // 立即购买商品skuId
        Integer buyNum = param.get("buyNum") == null ? 1 : Integer.parseInt(param.get("buyNum").toString()); // 立即购买商品数量
        String orderMode = param.get("orderMode") == null ? "" : param.get("orderMode").toString(); // 订单模式(配送or自取)
        Integer orderId = param.get("orderId") == null ? null : Integer.parseInt(param.get("orderId").toString()); // 订单ID

        if (userId <= 0) {
            userId = userInfo.getId();
        } else {
            if (StringUtil.isNotEmpty(operator)) {
                userInfo = memberService.queryMemberById(userId);
            }
        }
        param.put("userId", userId);

        // 订单所属店铺
        if (storeId < 1) {
            if (userInfo.getStoreId() > 0) {
                storeId = userInfo.getStoreId();
            }
        }

        // 生成订单数据
        OrderDto orderDto = new OrderDto();
        orderDto.setId(orderId);
        orderDto.setRemark(remark);
        orderDto.setUserId(userId);
        orderDto.setStoreId(storeId);
        orderDto.setType(type);
        orderDto.setGoodsId(goodsId);
        orderDto.setSkuId(skuId);
        orderDto.setBuyNum(buyNum);
        orderDto.setOrderMode(orderMode);
        orderDto.setOperator(operator);
        orderDto.setPayType(payType);
        orderDto.setCouponId(0);

        MtSetting pointSetting = settingService.querySettingByName(PointSettingEnum.CAN_USE_AS_MONEY.getKey());
        // 使用积分数量
        if (pointSetting != null && pointSetting.getValue().equals("true")) {
            orderDto.setUsePoint(usePoint);
        } else {
            orderDto.setUsePoint(0);
            usePoint = 0;
        }

        orderDto.setPointAmount(new BigDecimal("0"));
        orderDto.setDiscount(new BigDecimal("0"));
        orderDto.setPayAmount(new BigDecimal("0"));
        orderDto.setAmount(new BigDecimal("0"));
        orderDto.setCartIds(cartIds);

        // 预存卡的订单
        if (orderDto.getType().equals(OrderTypeEnum.PRESTORE.getKey())) {
            orderDto.setCouponId(targetId);
            String orderParam = "";
            BigDecimal totalAmount = new BigDecimal(0);

            MtCoupon couponInfo = couponService.queryCouponById(targetId);
            String inRule = couponInfo.getInRule();
            String[] selectNumArr = selectNum.split(",");
            String[] ruleArr = inRule.split(",");
            for (int i = 0; i < ruleArr.length; i++) {
                String item = ruleArr[i] + "_" + (StringUtil.isNotEmpty(selectNumArr[i]) ? selectNumArr[i] : 0);
                String[] itemArr = item.split("_");
                // 预存金额
                BigDecimal price = new BigDecimal(itemArr[0]);
                // 预存数量
                BigDecimal num = new BigDecimal(selectNumArr[i]);
                BigDecimal amount = price.multiply(num);
                totalAmount = totalAmount.add(amount);
                orderParam = StringUtil.isEmpty(orderParam) ?  item : orderParam + ","+item;
            }

            orderDto.setParam(orderParam);
            orderDto.setAmount(totalAmount);
            payAmount = totalAmount.toString();
        }

        // 付款订单
        if (orderDto.getType().equals(OrderTypeEnum.PAYMENT.getKey())) {
            orderDto.setAmount(new BigDecimal(payAmount));
            orderDto.setPayAmount(new BigDecimal(payAmount));
            orderDto.setDiscount(new BigDecimal("0"));
        }

        // 升级订单
        if (orderDto.getType().equals(OrderTypeEnum.MEMBER.getKey())) {
            orderDto.setParam(targetId+"");
            MtUserGrade userGrade = userGradeService.queryUserGradeById(targetId);
            if (userGrade != null) {
                orderDto.setRemark("付费升级" + userGrade.getName());
                orderDto.setAmount(new BigDecimal(userGrade.getCatchValue().toString()));
            }
        }

        // 商品订单
        if (orderDto.getType().equals(OrderTypeEnum.GOOGS.getKey())) {
            orderDto.setCouponId(couponId);
        }

        // 使用积分抵扣
        if (usePoint > 0) {
            List<MtSetting> settingList = settingService.getSettingList(SettingTypeEnum.POINT.getKey());
            String canUsedAsMoney = "false";
            String exchangeNeedPoint = "0";
            for (MtSetting setting : settingList) {
                if (setting.getName().equals("canUsedAsMoney")) {
                    canUsedAsMoney = setting.getValue();
                } else if (setting.getName().equals("exchangeNeedPoint")) {
                    exchangeNeedPoint = setting.getValue();
                }
            }
            // 是否可以使用积分，并且积分数量足够
            if (canUsedAsMoney.equals("true") && Float.parseFloat(exchangeNeedPoint) > 0 && (userInfo.getPoint() >= usePoint)) {
                orderDto.setUsePoint(usePoint);
                orderDto.setPointAmount(new BigDecimal(usePoint).divide(new BigDecimal(exchangeNeedPoint)));
                if (orderDto.getPayAmount().compareTo(orderDto.getPointAmount()) > 0) {
                    orderDto.setPayAmount(orderDto.getPayAmount().subtract(orderDto.getPointAmount()));
                } else {
                    orderDto.setPayAmount(new BigDecimal("0"));
                }
            }
        }

        // 会员付款折扣
        if (orderDto.getType().equals(OrderTypeEnum.PAYMENT.getKey())) {
            MtUserGrade userGrade = userGradeService.queryUserGradeById(Integer.parseInt(userInfo.getGradeId()));
            if (userGrade != null) {
                // 是否有会员折扣
                if (userGrade.getDiscount() > 0) {
                    BigDecimal percent = new BigDecimal(userGrade.getDiscount()).divide(new BigDecimal("10"));
                    BigDecimal payAmountDiscount = orderDto.getPayAmount().multiply(percent);
                    orderDto.setDiscount(orderDto.getDiscount().add(orderDto.getPayAmount().subtract(payAmountDiscount)));
                    orderDto.setPayAmount(payAmountDiscount);
                }
            }
        }

        // 生成订单
        MtOrder orderInfo;
        try {
            orderInfo = orderService.saveOrder(orderDto);
        } catch (BusinessCheckException e) {
            return getFailureResult(201, e.getMessage());
        }

        orderDto.setId(orderInfo.getId());
        param.put("orderId", orderInfo.getId());

        // 收银台实付金额、优惠金额
        if ((StringUtil.isNotEmpty(cashierPayAmount) || StringUtil.isNotEmpty(cashierDiscountAmount)) && StringUtil.isNotEmpty(operator)) {
            OrderDto reqOrder = new OrderDto();
            reqOrder.setId(orderInfo.getId());
            reqOrder.setAmount(new BigDecimal(cashierPayAmount).add(new BigDecimal(cashierDiscountAmount)));
            reqOrder.setDiscount(new BigDecimal(cashierDiscountAmount));
            orderService.updateOrder(reqOrder);
            orderInfo = orderRepository.findOne(orderInfo.getId());
        }

        // 付款订单使用卡券抵扣
        if (couponId > 0 && orderDto.getType().equals(OrderTypeEnum.PAYMENT.getKey())) {
            if (orderDto.getAmount().compareTo(new BigDecimal("0")) > 0) {
                MtUserCoupon userCouponInfo = userCouponService.getUserCouponDetail(couponId);
                if (userCouponInfo != null) {
                    MtCoupon couponInfo = couponService.queryCouponById(userCouponInfo.getCouponId());
                    if (couponInfo != null) {
                        boolean isEffective = couponService.isCouponEffective(couponInfo);
                        if (isEffective && userCouponInfo.getUserId().equals(orderDto.getUserId())) {
                            // 优惠券，直接减去优惠券金额
                            if (couponInfo.getType().equals(CouponTypeEnum.COUPON.getKey())) {
                                String useCode = couponService.useCoupon(couponId, orderDto.getUserId(), orderDto.getStoreId(), orderInfo.getId(), userCouponInfo.getAmount(), "核销");
                                if (StringUtil.isNotEmpty(useCode)) {
                                    orderDto.setCouponId(couponId);
                                    orderDto.setDiscount(orderDto.getDiscount().add(userCouponInfo.getAmount()));
                                    orderService.updateOrder(orderDto);
                                }
                            } else if(couponInfo.getType().equals(CouponTypeEnum.PRESTORE.getKey())) {
                                // 预存卡，减去余额
                                BigDecimal useCouponAmount = userCouponInfo.getBalance();
                                if (orderDto.getPayAmount().compareTo(userCouponInfo.getBalance()) <= 0) {
                                    useCouponAmount = orderDto.getPayAmount();
                                }
                                try {
                                    String useCode = couponService.useCoupon(couponId, orderDto.getUserId(), orderDto.getStoreId(), orderInfo.getId(), useCouponAmount, "核销");
                                    if (StringUtil.isNotEmpty(useCode)) {
                                        orderDto.setCouponId(couponId);
                                        orderDto.setDiscount(orderDto.getDiscount().add(useCouponAmount));
                                        orderDto.setPayAmount(orderDto.getPayAmount().subtract(useCouponAmount));
                                        orderService.updateOrder(orderDto);
                                    }
                                } catch (BusinessCheckException e) {
                                    return getFailureResult(201, e.getMessage());
                                }
                            }
                        }
                    }
                }
            }
        }

        // 生成支付订单
        orderInfo = orderRepository.findOne(orderInfo.getId());
        String ip = CommonUtil.getIPFromHttpRequest(request);
        BigDecimal realPayAmount = orderInfo.getAmount().subtract(new BigDecimal(orderInfo.getDiscount().toString())).subtract(new BigDecimal(orderInfo.getPointAmount().toString()));

        ResponseObject paymentInfo = null;
        String errorMessage = "";

        // 应付金额大于0才提交微信支付
        if (realPayAmount.compareTo(new BigDecimal("0")) > 0) {
            if (payType.equals(PayTypeEnum.CASH.getKey()) && StringUtil.isNotEmpty(operator)) {
                // 收银台现金支付，更新为已支付
                orderService.setOrderPayed(orderInfo.getId());
            } else if(payType.equals(PayTypeEnum.BALANCE.getKey())) {
                // 余额支付
                MtBalance balance = new MtBalance();
                balance.setMobile(userInfo.getMobile());
                balance.setOrderSn(orderInfo.getOrderSn());
                balance.setUserId(userInfo.getId());
                balance.setAmount(realPayAmount.subtract(realPayAmount).subtract(realPayAmount));
                boolean isPay = balanceService.addBalance(balance);
                if (isPay) {
                    orderService.setOrderPayed(orderInfo.getId());
                } else {
                    // 取消订单
                    orderService.cancelOrder(orderInfo.getId(), "");
                    errorMessage = PropertiesUtil.getResponseErrorMessageByCode(5001);
                }
            } else {
                BigDecimal wxPayAmount = realPayAmount.multiply(new BigDecimal("100"));
                // 微信扫码支付，先返回不处理，后面拿到支付二维码再处理
                if (payType.equals(PayTypeEnum.MICROPAY.getKey()) && StringUtil.isEmpty(authCode)) {
                    Map<String, String> data = new HashMap<>();
                    paymentInfo = getSuccessResult(data);
                } else {
                    paymentInfo = weixinService.createPrepayOrder(userInfo, orderInfo, (wxPayAmount.intValue()), authCode, 0, ip);
                }
                if (paymentInfo.getData() == null) {
                    errorMessage = PropertiesUtil.getResponseErrorMessageByCode(3000);
                }
            }
        } else {
            // 应付金额是0，直接更新为已支付
            orderService.setOrderPayed(orderInfo.getId());
        }

        Map<String, Object> outParams = new HashMap();
        outParams.put("isCreated", true);
        outParams.put("orderInfo", orderInfo);

        if (paymentInfo != null) {
            outParams.put("payment", paymentInfo.getData());
            outParams.put("payType", PayTypeEnum.JSAPI.getKey());
        } else {
            outParams.put("payment", null);
            outParams.put("payType", "BALANCE");
        }

        ResponseObject responseObject = getSuccessResult(outParams);

        // 1分钟后发送小程序订阅消息
        Date nowTime = new Date();
        Date sendTime = new Date(nowTime.getTime() + 60000);
        Map<String, Object> params = new HashMap<>();
        String dateTime = DateUtil.formatDate(Calendar.getInstance().getTime(), "yyyy-MM-dd HH:mm");
        params.put("time", dateTime);
        params.put("orderSn", orderInfo.getOrderSn());
        params.put("remark", "您的订单已生成，请留意~");
        weixinService.sendSubscribeMessage(userInfo.getId(), userInfo.getOpenId(), WxMessageEnum.ORDER_CREATED.getKey(), "pages/order/index", params, sendTime);

        if (StringUtil.isNotEmpty(errorMessage)) {
            return getFailureResult(201, errorMessage);
        } else {
            return getSuccessResult(responseObject.getData());
        }
    }
}
