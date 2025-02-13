package com.fuint.application.web.backendApi;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fuint.application.BaseController;
import com.fuint.application.ResponseObject;
import com.fuint.application.dao.entities.MtSetting;
import com.fuint.application.dto.AccountDto;
import com.fuint.application.dto.ParamDto;
import com.fuint.application.enums.SettingTypeEnum;
import com.fuint.application.enums.StatusEnum;
import com.fuint.application.enums.WxMessageEnum;
import com.fuint.application.service.setting.SettingService;
import com.fuint.application.service.token.TokenService;
import com.fuint.exception.BusinessCheckException;
import com.fuint.application.dto.SubMessageDto;
import com.fuint.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.env.Environment;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 订阅消息管理类controller
 *
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
@RestController
@RequestMapping(value = "/backendApi/subMessage")
public class BackendSubMessageController extends BaseController {

    /**
     * 配置服务接口
     * */
    @Autowired
    private SettingService settingService;

    /**
     * 登录令牌接口
     * */
    @Autowired
    private TokenService tokenService;

    @Autowired
    private Environment env;

    /**
     * 订阅消息管理
     *
     * @param  request  HttpServletRequest对象
     * @return 列表展现
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    @CrossOrigin
    public ResponseObject list(HttpServletRequest request) throws BusinessCheckException {
        String token = request.getHeader("Access-Token");
        String status = request.getParameter("status") == null ? "" : request.getParameter("status");
        String title = request.getParameter("title") == null ? "" : request.getParameter("title");

        AccountDto accountInfo = tokenService.getAccountInfoByToken(token);
        if (accountInfo == null) {
            return getFailureResult(401, "请先登录");
        }

        List<SubMessageDto> dataList = new ArrayList<>();
        for (WxMessageEnum wxMessageEnum : WxMessageEnum.values()) {
            SubMessageDto e = new SubMessageDto();
            MtSetting setting = settingService.querySettingByName(wxMessageEnum.getKey());
            e.setKey(wxMessageEnum.getKey());
            e.setTitle(wxMessageEnum.getValue());
            JSONObject jsonObject = null;
            if (setting != null) {
                try {
                    jsonObject = JSONObject.parseObject(setting.getValue());
                } catch (Exception ex) {
                    // empty
                }
                if (jsonObject != null) {
                    String templateId = jsonObject.get("templateId").toString();
                    String tid = jsonObject.get("tid").toString();
                    JSONArray paramArray = (JSONArray) JSONObject.parse(jsonObject.get("params").toString());
                    if (StringUtil.isEmpty(templateId) || StringUtil.isEmpty(tid) || paramArray.size() < 1) {
                        jsonObject = null;
                    }
                }
            }

            if (setting != null && jsonObject != null) {
                e.setStatus(setting.getStatus());
            } else {
                e.setStatus(StatusEnum.FORBIDDEN.getKey());
            }

            if (StringUtil.isNotEmpty(status)) {
                if (e.getStatus().equals(status)) {
                    dataList.add(e);
                }
            } else if(StringUtil.isNotEmpty(title)) {
                if (e.getTitle().indexOf(title) >= 0) {
                    dataList.add(e);
                }
            } else {
                dataList.add(e);
            }
        }

        Map<String, Object> result = new HashMap();
        result.put("dataList", dataList);

        return getSuccessResult(result);
    }

    /**
     * 消息详情
     *
     * @param key 消息键值
     * @return
     */
    @RequestMapping(value = "/info/{key}", method = RequestMethod.GET)
    @CrossOrigin
    public ResponseObject info(HttpServletRequest request, @PathVariable("key") String key) throws BusinessCheckException {
        String token = request.getHeader("Access-Token");
        AccountDto accountInfo = tokenService.getAccountInfoByToken(token);
        if (accountInfo == null) {
            return getFailureResult(401, "请先登录");
        }

        Map<String, Object> result = new HashMap();

        String name = WxMessageEnum.getValue(key);
        if (StringUtil.isNotEmpty(name)) {
            MtSetting mtSetting = settingService.querySettingByName(key);
            JSONObject jsonObject = null;
            try {
                if (mtSetting != null && mtSetting.getValue().indexOf('}') > 0) {
                    jsonObject = JSONObject.parseObject(mtSetting.getValue());
                }
                String templateId = "";
                String tid = "";
                JSONArray paramArray = null;

                if (jsonObject != null) {
                    templateId = jsonObject.get("templateId").toString();
                    tid = jsonObject.get("tid").toString();
                    paramArray = (JSONArray) JSONObject.parse(jsonObject.get("params").toString());
                }

                List<ParamDto> params = new ArrayList<>();
                String tplConfigJson = env.getProperty("weixin.subMessage." + key);
                tplConfigJson = new String(tplConfigJson.getBytes("ISO8859-1"), "UTF-8");

                if (StringUtil.isNotEmpty(tplConfigJson)) {
                    JSONArray jsonArray = (JSONArray)JSONObject.parse(tplConfigJson);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        ParamDto dto = new ParamDto();
                        dto.setKey(obj.get("key").toString());
                        dto.setName(obj.get("name").toString());
                        if (paramArray != null) {
                            dto.setValue("");
                            for (int j = 0; j < paramArray.size(); j++) {
                                 JSONObject paraObj = paramArray.getJSONObject(j);
                                 if (paraObj.get("key").toString().equals(obj.get("key").toString())) {
                                     dto.setValue(paraObj.get("value") == null ? "" : paraObj.get("value").toString());
                                 }
                            }
                        } else {
                            dto.setValue("");
                        }
                        params.add(dto);
                    }
                }

                result.put("params", params);
                result.put("name", name);
                result.put("key", key);
                result.put("templateId", templateId);
                result.put("tid", tid);
                return getSuccessResult(result);
            } catch (Exception e) {
                return getFailureResult(201, "操作失败");
            }
        }

        return getFailureResult(201, "操作失败");
    }

    /**
     * 保存消息设置
     *
     * @param request  HttpServletRequest对象
     * @return
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @CrossOrigin
    public ResponseObject saveHandler(HttpServletRequest request, @RequestBody Map<String, Object> param) throws BusinessCheckException {
        String token = request.getHeader("Access-Token");
        String key = param.get("key").toString();
        String templateId = param.get("templateId").toString();
        String tid = param.get("tid").toString();
        List<LinkedHashMap> paramData = (List) param.get("params");

        AccountDto accountInfo = tokenService.getAccountInfoByToken(token);
        if (accountInfo == null) {
            return getFailureResult(401, "请先登录");
        }

        SubMessageDto subMessageDto = new SubMessageDto();
        subMessageDto.setKey(key);
        subMessageDto.setTemplateId(templateId);
        subMessageDto.setTid(tid);
        subMessageDto.setStatus(StatusEnum.ENABLED.getKey());

        try {
            String tplConfigJson = env.getProperty("weixin.subMessage." + key);
            JSONArray jsonArray = (JSONArray) JSONObject.parse(tplConfigJson);
            List<ParamDto> params = new ArrayList<>();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String oKey = obj.get("key").toString();
                String value = "";
                for (LinkedHashMap paraItem : paramData) {
                     String pKey = paraItem.get("key").toString();
                     if (pKey.equals(oKey)) {
                         value = paraItem.get("value").toString();
                         break;
                     }
                }

                ParamDto para = new ParamDto();
                String name = obj.get("name").toString();
                name = new String(name.getBytes("ISO8859-1"), "UTF-8");
                para.setName(name);
                para.setKey(obj.get("key").toString());
                para.setValue(value);
                params.add(para);
            }

            subMessageDto.setParams(params);
            String json = JSONObject.toJSONString(subMessageDto);

            // 保存
            settingService.removeSetting(key);
            MtSetting info = new MtSetting();
            info.setType(SettingTypeEnum.SUB_MESSAGE.getKey());
            info.setName(key);
            info.setValue(json);

            String description = WxMessageEnum.getValue(key);
            info.setDescription(description);
            info.setOperator(accountInfo.getAccountName());
            info.setUpdateTime(new Date());
            settingService.saveSetting(info);
        }  catch (Exception e) {
            return getFailureResult(201, "操作失败");
        }

        return getSuccessResult(true);
    }
}
