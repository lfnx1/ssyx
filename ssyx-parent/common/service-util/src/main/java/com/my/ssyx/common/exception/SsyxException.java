package com.my.ssyx.common.exception;

import com.my.ssyx.common.result.ResultCodeEnum;
import lombok.Data;

@Data
public class SsyxException extends  RuntimeException{
    private Integer code;

    public SsyxException(String message,Integer code){
        super(message);
        this.code=code;
    }

    public SsyxException(ResultCodeEnum resultCodeEnum){
        super(resultCodeEnum.getMessage());
        this.code =resultCodeEnum.getCode();
    }

}
