package com.my.ssyx.common.exception;

import com.my.ssyx.common.result.Result;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

//采用Aop方式
@ControllerAdvice
public class GlobalExceptionHandler {

    //异常处理器
    @ExceptionHandler(Exception.class)
    @ResponseBody //返回json数据
    public Result error(Exception e){
        e.printStackTrace();
        return  Result.fail(null);
    }

    //自定义异常处理
    @ExceptionHandler(SsyxException.class)
    @ResponseBody
    public  Result error(SsyxException exception){
        return Result.build(null,exception.getCode(),exception.getMessage());
    }

}
