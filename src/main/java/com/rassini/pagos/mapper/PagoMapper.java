package com.rassini.pagos.mapper;

import com.rassini.pagos.dto.PagoPendienteDTO;
import com.rassini.pagos.entity.PagosArchivo;

public class PagoMapper {

    public static PagoPendienteDTO toDTO(PagosArchivo entity) {

        PagoPendienteDTO dto = new PagoPendienteDTO();

        dto.setId(entity.getId());
        dto.setEmpresa(entity.getEmpresa());
        dto.setReferencia(entity.getReferencia());
        dto.setFechaEnvio(entity.getFechaEnvio());
        dto.setFechaValor(entity.getFechaValor());
        dto.setCodigoProveedor(entity.getCodigoProveedor());
        dto.setRfcBeneficiario(entity.getRfcBeneficiario());
        dto.setInformacionAdicional(entity.getInformacionAdicional());
        dto.setMonto(entity.getMonto());
        dto.setMoneda(entity.getMoneda());
        dto.setTipoCambio(entity.getTipoCambio());
        dto.setNombreBeneficiario(entity.getNombreBeneficiario());
        dto.setCuentaBeneficiario(entity.getCuentaBeneficiario());

        return dto;
    }
}