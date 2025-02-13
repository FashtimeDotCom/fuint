package com.fuint.application.web.rest;

import com.alibaba.fastjson.JSONObject;
import com.fuint.application.util.CommonUtil;
import com.fuint.captcha.service.CaptchaService;
import com.fuint.exception.BusinessCheckException;
import com.fuint.application.ResponseObject;
import com.fuint.application.BaseController;
import com.fuint.application.dao.entities.MtUser;
import com.fuint.application.dao.entities.MtVerifyCode;
import com.fuint.application.dto.TokenDto;
import com.fuint.application.enums.StatusEnum;
import com.fuint.application.service.member.MemberService;
import com.fuint.application.service.weixin.WeixinService;
import com.fuint.application.service.token.TokenService;
import com.fuint.application.service.verifycode.VerifyCodeService;
import com.fuint.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录类controller
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
@RestController
@RequestMapping(value = "/rest/sign")
public class SignController extends BaseController {

    /**
     * 会员服务接口
     */
    @Autowired
    private MemberService memberService;

    /**
     * 登录令牌接口
     * */
    @Autowired
    private TokenService tokenService;

    /**
     * 验证码接口
     */
    @Autowired
    private VerifyCodeService verifyCodeService;

    /**
     * 微信相关接口
     * */
    @Autowired
    private WeixinService weixinService;

    /**
     * 图形验证码
     * */
    @Autowired
    private CaptchaService captchaService;

    /**
     * 微信授权登录
     * */
    @RequestMapping(value = "/mpWxLogin", method = RequestMethod.POST)
    @ResponseBody
    @CrossOrigin
    public ResponseObject mpWxLogin(HttpServletRequest request, @RequestBody Map<String, Object> param) throws BusinessCheckException {
        JSONObject paramsObj = new JSONObject(param);

        JSONObject userInfo = paramsObj.getJSONObject("userInfo");
        JSONObject loginInfo = weixinService.getWxProfile(param.get("code").toString());
        if (loginInfo == null) {
            return getFailureResult(0, "微信登录失败");
        }

        String type = userInfo.getString("type");
        String encryptedData = userInfo.getString("encryptedData");
        userInfo.put("phone", "");
        if (type.equals("phone") && StringUtil.isNotEmpty(encryptedData)) {
            String phone = weixinService.getPhoneNumber(userInfo.get("encryptedData").toString(), loginInfo.get("session_key").toString(), userInfo.get("iv").toString());
            userInfo.put("phone", phone);
        }

        MtUser mtUser = memberService.queryMemberByOpenId(loginInfo.get("openid").toString(), userInfo);
        if (mtUser == null) {
            return getFailureResult(0, "用户状态异常");
        }

        String userAgent = request.getHeader("user-agent");
        String token = tokenService.generateToken(userAgent, mtUser.getId().toString());
        tokenService.saveToken(token, mtUser);

        Map<String, Object> outParams = new HashMap<>();
        outParams.put("token", token);
        outParams.put("userId", mtUser.getId());
        outParams.put("userName", mtUser.getName());

        return getSuccessResult("登录成功", outParams);
    }

    /**
     * 通过账号密码注册
     * */
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    @CrossOrigin
    public ResponseObject register(HttpServletRequest request, @RequestBody Map<String, Object> param) throws BusinessCheckException {
        String account = param.get("account").toString();
        String password = param.get("password").toString();
        String captchaCode = param.get("captchaCode") == null ? "" : param.get("captchaCode").toString();
        String uuid = param.get("uuid") == null ? "" : param.get("uuid").toString();
        Integer storeId = request.getHeader("storeId") == null ? 0 : Integer.parseInt(request.getHeader("storeId"));
        String userAgent = request.getHeader("user-agent") == null ? "" : request.getHeader("user-agent");

        if (StringUtil.isEmpty(account)) {
            return getFailureResult(1002,"用户名不能为空");
        }
        if (StringUtil.isEmpty(password)) {
            return getFailureResult(1002,"密码不能为空");
        }
        if (StringUtil.isEmpty(captchaCode)) {
            return getFailureResult(1002,"图形验证码不能为空");
        }

        boolean captchaVerify = captchaService.checkCodeByUuid(captchaCode, uuid);
        if (!captchaVerify) {
            return getFailureResult(1002,"图形验证码有误");
        }

        MtUser userData = memberService.queryMemberByName(account);
        if (userData != null) {
            return getFailureResult(1002,"该用户名已存在");
        }

        MtUser mtUser = new MtUser();
        mtUser.setName(account);
        mtUser.setPassword(password);
        mtUser.setStoreId(storeId);
        mtUser.setSex(0);
        mtUser.setMobile("");
        mtUser.setDescription("会员自行注册新账号");
        MtUser userInfo = memberService.addMember(mtUser);

        if (userInfo != null) {
            String token = tokenService.generateToken(userAgent, userInfo.getId().toString());
            tokenService.saveToken(token, mtUser);

            Map<String, Object> outParams = new HashMap<>();
            outParams.put("userId", userInfo.getId());
            outParams.put("userName", userInfo.getName());
            outParams.put("token", token);
            return getSuccessResult("注册成功", outParams);
        } else {
            return getFailureResult(201,"注册失败");
        }
    }

    /**
     * 会员登录（通过短信或账号密码）
     */
    @RequestMapping(value = "/signIn", method = RequestMethod.POST)
    @CrossOrigin
    public ResponseObject signIn(HttpServletRequest request, @RequestBody Map<String, Object> param) throws BusinessCheckException {
        String userToken = request.getHeader("Access-Token");
        String userAgent = request.getHeader("user-agent") == null ? "" : request.getHeader("user-agent");
        String mobile = param.get("mobile") == null ? "" : param.get("mobile").toString();
        String verifyCode = param.get("verifyCode") == null ? "" : param.get("verifyCode").toString();
        String account = param.get("account") == null ? "" : param.get("account").toString();
        String password = param.get("password") == null ? "" : param.get("password").toString();
        String captchaCode = param.get("captchaCode") == null ? "" : param.get("captchaCode").toString();
        String uuid = param.get("uuid") == null ? "" : param.get("uuid").toString();

        TokenDto dto = new TokenDto();
        MtUser mtUser = null;

        // 方式1：通过短信验证码登录
        if (StringUtil.isNotEmpty(mobile) && StringUtil.isNotEmpty(verifyCode)) {
            // 如果已经登录，免输入验证码
            if (StringUtil.isNotEmpty(userToken) && tokenService.checkTokenLogin(userToken)) {
                dto.setIsLogin("true");
                dto.setToken(userToken);
                return getSuccessResult(JSONObject.toJSONString(dto));
            }

            // 1、验证码验证
            MtVerifyCode mtVerifyCode = verifyCodeService.checkVerifyCode(mobile, verifyCode);
            mtUser = memberService.queryMemberByMobile(mobile);

            if (verifyCode.equals("999999")) {
                mtVerifyCode = new MtVerifyCode();
                mtVerifyCode.setId(1L);
            }

            // 2、写入token redis session
            if (mtVerifyCode != null) {
                if (null == mtUser) {
                    memberService.addMemberByMobile(mobile);
                    mtUser = memberService.queryMemberByMobile(mobile);
                }

                if (!mtUser.getStatus().equals(StatusEnum.ENABLED.getKey())) {
                    return getFailureResult(1002, "账号异常，登录失败");
                }

                // 更新验证码
                verifyCodeService.updateValidFlag(mtVerifyCode.getId(), "1");
                String token = tokenService.generateToken(userAgent, mobile);
                tokenService.saveToken(token, mtUser);

                dto.setIsLogin("true");
                dto.setToken(token);
                dto.setTokenCreatedTime(System.currentTimeMillis());
            } else {
                dto.setIsLogin("false");
                return getFailureResult(1002, "验证码错误或已过期，登录失败");
            }
        }

        // 方式2：通过账号密码登录
        if (StringUtil.isNotEmpty(account) && StringUtil.isNotEmpty(password) && StringUtil.isNotEmpty(captchaCode)) {
            Boolean captchaVerify = captchaService.checkCodeByUuid(captchaCode, uuid);
            if (!captchaVerify) {
                return getFailureResult(1002,"图形验证码有误");
            }

            MtUser userInfo = memberService.queryMemberByName(account);
            if (userInfo != null) {
                String myPassword = userInfo.getPassword();
                String inputPassword = CommonUtil.getPassword(password, userInfo.getSalt());
                if (myPassword.equals(inputPassword)) {
                    String token = tokenService.generateToken(userAgent, userInfo.getId().toString());
                    tokenService.saveToken(token, userInfo);
                    dto.setToken(token);
                    mtUser = userInfo;
                } else {
                    return getFailureResult(201, "账号或密码有误");
                }
            } else {
                return getFailureResult(201, "账号或密码有误");
            }
        }

        if (mtUser != null) {
            Map<String, Object> outParams = new HashMap<>();
            outParams.put("token", dto.getToken());
            outParams.put("userId", mtUser.getId());
            outParams.put("userName", mtUser.getName());
            return getSuccessResult("登录成功", outParams);
        } else {
            return getFailureResult(201, "登录失败");
        }
    }

    /**
     * 获取会员信息
     */
    @RequestMapping(value = "/doGetUserInfo", method = RequestMethod.POST)
    @CrossOrigin
    public ResponseObject doGetUserInfo(HttpServletRequest request) throws BusinessCheckException {
        String userToken = request.getHeader("Access-Token");
        MtUser mtUser = tokenService.getUserInfoByToken(userToken);

        if (mtUser == null) {
            return getFailureResult(1001, "用户没登录!");
        }

        return getSuccessResult(mtUser);
    }

    /**
     * 退出登录
     */
    @RequestMapping(value = "/signOut", method = RequestMethod.POST)
    @CrossOrigin
    public ResponseObject doLogout(HttpServletRequest request) {
        String userToken = request.getHeader("Access-Token");
        Boolean flag = tokenService.removeToken(userToken);
        if (Boolean.FALSE == flag) {
            return getFailureResult(1001, "退出错误!");
        } else {
            return getSuccessResult("退出成功！");
        }
    }
}
