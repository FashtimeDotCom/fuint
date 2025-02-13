package com.fuint.handler;

import javax.servlet.http.HttpServletResponse;

import com.fuint.exception.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;


/**
 * 全局异常处理类
 *
 * Created by FSQ
 * Contact wx fsq_better
 * Site https://www.fuint.cn
 */
@ControllerAdvice
public class GlobalErrorHandler {

    public static final String DEFAULT_ERROR_VIEW = "error";
    /**
     * 日志
     */
    private static final Logger LOG = LoggerFactory
            .getLogger(GlobalErrorHandler.class);

    /**
     * 默认系统异常拦截
     *
     * @param exception
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView exception(Exception exception, HttpServletResponse httpServletResponse) {
        ModelAndView mav = new ModelAndView(DEFAULT_ERROR_VIEW);
        LOG.error("====发生异常==== {}", exception);
        mav.addObject("error", this.exceptionMesssage(exception, httpServletResponse));
        exception.printStackTrace();
        return mav;
    }

    /**
     * 异常信息
     *
     * @param e 异常
     * @return
     */
    private String exceptionMesssage(Exception e, HttpServletResponse httpServletResponse) {
        httpServletResponse.setStatus(555);
        StringBuffer errorMsg = new StringBuffer();
        errorMsg.append("出错信息：");
        if (e instanceof NullPointerException) {
            errorMsg.append("空指针异常");
        } else if (e instanceof ForbiddenException) {
            errorMsg.append("登录超时，请重新登录");
            httpServletResponse.setStatus(501);
        } else {
            errorMsg.append(e.getMessage());
        }
        errorMsg.append("!");

        LOG.error("====发生错误啦==== {}", e.getMessage());

        return errorMsg.toString();
    }

}
