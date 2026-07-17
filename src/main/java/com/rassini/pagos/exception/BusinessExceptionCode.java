package com.rassini.pagos.exception;

public class BusinessExceptionCode  extends BusinessException {

    public BusinessExceptionCode (String message) {
        super(message);
        this.codigo="0";
    }


    private final String codigo;

    public BusinessExceptionCode(String codigo, String message) {
        super(message);
        this.codigo=codigo;
    }

    
    public String getCodigo() {
        return codigo;
    }

}