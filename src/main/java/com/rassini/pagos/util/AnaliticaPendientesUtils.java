package com.rassini.pagos.util;

import java.math.BigDecimal;
import java.util.List;

import com.rassini.pagos.entity.PagosArchivo;

public final class AnaliticaPendientesUtils {

    private static final String NO_TYPE = "NO_TYPE";
    private static final String NO_FILE = "NO_FILE";
    private static final String NO_COMPANY = "NO_COMPANY";
    private static final String NO_CURRENCY = "NO_CURRENCY";

    private AnaliticaPendientesUtils() {
    }

    public static String obtenerNombreArchivo(
            PagosArchivo pago) {

        if (pago.getNombreArchivo() == null
                || pago.getNombreArchivo().isBlank()) {
            return NO_FILE;
        }

        return limpiarTexto(pago.getNombreArchivo());
    }

    public static String obtenerEmpresa(
        PagosArchivo pago) {

        if (pago.getEmpresa() == null
                || pago.getEmpresa().isBlank()) {
            return NO_COMPANY;
        }

        return limpiarTexto(
                pago.getEmpresa());
    }

    public static String obtenerTipoPago(
            PagosArchivo pago) {

        if (pago.getTipoPago() == null
                || pago.getTipoPago().getDealType() == null
                || pago.getTipoPago().getDealType().isBlank()) {
            return NO_TYPE;
        }

        return limpiarTexto(pago.getTipoPago().getDealType());
    }

    public static String obtenerMoneda(
            PagosArchivo pago) {

        if (pago.getMoneda() == null
                || pago.getMoneda().isBlank()) {
            return NO_CURRENCY;
        }

        return limpiarTexto(pago.getMoneda());
    }

    public static BigDecimal calcularMontoTotal(
            List<PagosArchivo> pagos) {

        return pagos.stream()
                .map(PagosArchivo::getMonto)
                .map(AnaliticaPendientesUtils::parsearMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal parsearMonto(
            String monto) {

        if (monto == null
                || monto.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {

            return new BigDecimal(
                    monto.trim().replace(",", ""));

        } catch (NumberFormatException ex) {

            return BigDecimal.ZERO;
        }
    }

    private static String limpiarTexto(
        String valor) {

        if (valor == null) {
            return "";
        }

        return valor
                .replace("\uFEFF", "")
                .trim();
    }
}
