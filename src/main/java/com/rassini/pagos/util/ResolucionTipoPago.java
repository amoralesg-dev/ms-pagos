package com.rassini.pagos.util;

import java.util.Collections;
import java.util.List;

/**
 * Resultado de la resolución centralizada de tipo de pago para una transferencia bancaria.
 * ABA y SWIFT no alteran la clasificación de negocio (ACH, SPID, SPEI, WIRE);
 * únicamente determinan la viabilidad técnica bancaria.
 */
public class ResolucionTipoPago {

    public enum Clasificacion {
        VALIDA,
        FALTA_ABA,
        FALTA_SWIFT,
        AUTOMATICA_UNICA,
        AMBIGUA,
        SIN_OPCIONES,
        INCOMPATIBLE_PERSISTIDA
    }

    private final List<String> opcionesValidas;
    private final String tipoSugerido;
    private final String tipoAutomatico;
    private final Clasificacion clasificacion;
    private final String motivo;

    public ResolucionTipoPago(List<String> opcionesValidas, String tipoSugerido, String tipoSeleccionado, Clasificacion clasificacion, String motivo) {
        this.opcionesValidas = opcionesValidas != null ? Collections.unmodifiableList(opcionesValidas) : Collections.emptyList();
        this.tipoSugerido = tipoSugerido;
        this.tipoAutomatico = tipoSeleccionado != null ? tipoSeleccionado : tipoSugerido;
        this.clasificacion = clasificacion;
        this.motivo = motivo;
    }

    public ResolucionTipoPago(List<String> opcionesValidas, String tipoAutomatico, Clasificacion clasificacion, String motivo) {
        this(opcionesValidas, tipoAutomatico, tipoAutomatico, clasificacion, motivo);
    }

    public List<String> getOpcionesValidas() {
        return opcionesValidas;
    }

    public String getTipoSugerido() {
        return tipoSugerido != null ? tipoSugerido : tipoAutomatico;
    }

    public String getTipoAutomatico() {
        return tipoAutomatico;
    }

    public String getTipoResuelto() {
        return tipoAutomatico;
    }

    public String getTipoSeleccionado() {
        return tipoAutomatico;
    }

    public Clasificacion getClasificacion() {
        return clasificacion;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getAdvertencia() {
        return motivo;
    }

    public boolean isValida() {
        return clasificacion == Clasificacion.VALIDA || clasificacion == Clasificacion.AUTOMATICA_UNICA;
    }

    public boolean isFaltaAba() {
        return clasificacion == Clasificacion.FALTA_ABA;
    }

    public boolean isFaltaSwift() {
        return clasificacion == Clasificacion.FALTA_SWIFT;
    }

    public boolean isErrorDatosBancarios() {
        return isFaltaAba() || isFaltaSwift();
    }

    public boolean isAutomaticaUnica() {
        return clasificacion == Clasificacion.AUTOMATICA_UNICA || clasificacion == Clasificacion.VALIDA;
    }

    public boolean isAmbiguo() {
        return clasificacion == Clasificacion.AMBIGUA;
    }

    public boolean isSinOpciones() {
        return clasificacion == Clasificacion.SIN_OPCIONES;
    }

    public boolean isIncompatible() {
        return isErrorDatosBancarios() || clasificacion == Clasificacion.INCOMPATIBLE_PERSISTIDA;
    }
}

