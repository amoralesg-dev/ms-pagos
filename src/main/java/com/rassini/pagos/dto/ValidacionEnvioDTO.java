package com.rassini.pagos.dto;

import java.util.List;

public class ValidacionEnvioDTO {

    private boolean permitido;

    private List<String> errores;

    public ValidacionEnvioDTO() {
    }

    public ValidacionEnvioDTO(
            boolean permitido,
            List<String> errores) {

        this.permitido = permitido;
        this.errores = errores;
    }

    public boolean isPermitido() {
        return permitido;
    }

    public void setPermitido(boolean permitido) {
        this.permitido = permitido;
    }

    public List<String> getErrores() {
        return errores;
    }

    public void setErrores(List<String> errores) {
        this.errores = errores;
    }
}
