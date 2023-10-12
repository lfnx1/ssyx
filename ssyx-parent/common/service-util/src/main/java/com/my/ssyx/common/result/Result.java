package com.my.ssyx.common.result;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private  T  data;

    private Result(){

    }
    //返回对象的方法
    public  static<T> Result<T> build(T data,Integer code,String message){
        //创建Result 对象 设置值 返回对象
        Result<T> result =new Result<>();
        //判断返回结果中是否需要数据
        if(data!=null){
            //设置数据到result对象当中
            result.setData(data);
        }
        //设置其他值
        result.setCode(code);
        result.setMsg(message);
        //返回设置值的对象
        return  result;
    }

    //成功的方法
    public  static<T> Result<T> ok (T data){
        Result<T> result = build(data, ResultCodeEnum.SUCCESS.getCode(),ResultCodeEnum.SUCCESS.getMessage());
        return  result;
    }

    //失败的方法
    public  static<T> Result<T> fail(T data){
        Result<T> result = build(data, ResultCodeEnum.SUCCESS.getCode(),ResultCodeEnum.SUCCESS.getMessage());
        return  result;
    }

}
