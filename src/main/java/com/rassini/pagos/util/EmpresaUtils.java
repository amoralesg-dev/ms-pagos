package com.rassini.pagos.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.rassini.pagos.exception.BusinessException;

public final class EmpresaUtils {

    private EmpresaUtils() {
    }

    /**
     * HIJA -> PADRE
     */
    private static final Map<String, String> EMPRESA_PADRE_MAP =
            construirMapaEmpresas();

    private static Map<String, String> construirMapaEmpresas() {

        Map<String, String> mapa = new HashMap<>();

        // Grupo 0111 corporativo
        mapa.put("0112", "0111");
        mapa.put("0103", "0111");
        mapa.put("0109", "0111");
        mapa.put("0110", "0111");
        mapa.put("0111", "0111");
        mapa.put("0114", "0111");
        mapa.put("0115", "0111");
        mapa.put("0117", "0111");
        mapa.put("0120", "0111");

        // Grupo 09 piedras negras
        mapa.put("02", "09");
        mapa.put("72", "09");
        mapa.put("09", "09");
        mapa.put("10", "09");

        // Grupo 99 piedras negras
        mapa.put("99", "99");

        // Independientes
        mapa.put("0301", "0301");
        mapa.put("1000", "1000");
        mapa.put("1001", "1000"); // prueba
        mapa.put("1850", "1850");

        return mapa;
    }

    /**
     * Obtiene la empresa padre de una empresa.
     */
    public static String obtenerEmpresaPadre(String empresa) {

        if (empresa == null || empresa.isBlank()) {
            throw new BusinessException(
                    "La empresa viene vacía");
        }

        String empresaPadre =
                EMPRESA_PADRE_MAP.get(empresa.trim());

        if (empresaPadre == null) {
            throw new BusinessException(
                    "No existe configuración de empresa padre para la empresa "
                            + empresa.trim());
        }

        return empresaPadre;
    }

    /**
     * Obtiene todas las empresas (padre e hijas)
     * pertenecientes a una BU padre.
     *
     * 1000 -> [1000,1001]
     * 0111 -> [0112,0103,0109,0110,0111,0114,0117,0120]
     */
    public static List<String> obtenerEmpresasHijas(
            String empresaPadre) {

        return EMPRESA_PADRE_MAP.entrySet()
                .stream()
                .filter(e -> empresaPadre.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Convierte una selección de BU en todas las empresas
     * que deben consultarse.
     *
     * 1000 -> [1000,1001]
     * 0111 -> [0112,0103,0109,0110,0111,0114,0117,0120]
     * 0111,09 -> [0112,0103,0109,0110,0111,0114,0117,0120,02,72,09]
     */
    public static List<String> obtenerEmpresasBusqueda(
            String bu) {

        return BuUtils.splitBus(bu)
                .stream()
                .map(EmpresaUtils::obtenerEmpresaPadre)
                .distinct()
                .flatMap(
                        padre -> obtenerEmpresasHijas(padre)
                                .stream())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Obtiene de forma dinamica y ordenada todas las empresas padre (BUs) configuradas
     * en el mapa de empresas.
     */
    public static List<String> obtenerTodasEmpresasPadre() {
        return EMPRESA_PADRE_MAP.values()
                 .stream()
                 .filter(val -> val != null && !val.isBlank())
                 .distinct()
                 .sorted()
                 .collect(Collectors.toList());
    }

    /**
     * Obtiene de forma dinamica y ordenada todas las empresas reales (BUs hijas) configuradas
     * en el mapa de empresas.
     */
    public static List<String> obtenerTodasEmpresasReales() {
        return EMPRESA_PADRE_MAP.keySet()
                 .stream()
                 .filter(val -> val != null && !val.isBlank())
                 .distinct()
                 .sorted()
                 .collect(Collectors.toList());
    }

    public static String obtenerEmpresaPadreSeguro(String empresa) {
        if (empresa == null || empresa.isBlank()) {
            return "";
        }
        String padre = EMPRESA_PADRE_MAP.get(empresa.trim());
        return padre != null ? padre : empresa.trim();
    }

    /**
     * Plantas habilitadas por negocio para la funcionalidad ACH/WIRE.
     * Centralizado en un único Set para que agregar nuevas plantas en el futuro sea trivial.
     */
    private static final Set<String> EMPRESAS_ACH_WIRE = Set.of(
            "1850",
            "09"
    );

    /**
     * Determina de forma centralizada si una empresa (o su empresa padre)
     * participa en la funcionalidad de selección ACH/WIRE.
     */
    public static boolean aplicaTransferenciaAchWire(String empresa) {
        return true;
    }

    /**
     * Valores permitidos para tipo_pago_seleccionado:
     * - ACH (usa Routing Code ABA)
     * - WIRE (usa Routing Code SWIFT)
     * - SPID (usa Routing Code SWIFT)
     * - SPEI (usa Routing Code SWIFT)
     */
    public static final String TIPO_PAGO_ACH = "ACH";
    public static final String TIPO_PAGO_WIRE = "WIRE";
    public static final String TIPO_PAGO_SPID = "SPID";
    public static final String TIPO_PAGO_SPEI = "SPEI";

    public static final Set<String> TIPOS_PAGO_SELECCIONADOS_PERMITIDOS = Set.of(
            TIPO_PAGO_ACH,
            TIPO_PAGO_WIRE,
            TIPO_PAGO_SPID,
            TIPO_PAGO_SPEI
    );

    /**
     * Calcula automáticamente el tipo de pago según las reglas de negocio confirmadas:
     * - USD + US              -> ACH
     * - MXN + MX              -> SPEI
     * - USD + MX              -> SPID
     * - USD + país <> MX      -> SPID (cubre país distinto de MX cuando no es US para USD)
     * - Por compatibilidad/default: WIRE
     */
    public static String calcularTipoPagoAutomatico(String moneda, String pais) {
        String m = moneda != null ? moneda.trim().toUpperCase(java.util.Locale.ROOT) : "";
        String p = pais != null ? pais.trim().toUpperCase(java.util.Locale.ROOT) : "";

        if ("USD".equals(m) && "US".equals(p)) {
            return TIPO_PAGO_ACH;
        }
        if ("MXN".equals(m) && "MX".equals(p)) {
            return TIPO_PAGO_SPEI;
        }
        if ("USD".equals(m) && "MX".equals(p)) {
            return TIPO_PAGO_SPID;
        }
        if ("USD".equals(m) && !"MX".equals(p)) {
            return TIPO_PAGO_SPID;
        }
        return TIPO_PAGO_WIRE;
    }

    /**
     * Determina de forma centralizada las opciones disponibles para el selector
     * según la moneda, país del beneficiario y la disponibilidad de códigos ABA y SWIFT.
     * Mapeo de routing:
     * - ACH  -> requiere ABA
     * - WIRE -> requiere SWIFT
     * - SPID -> requiere SWIFT
     * - SPEI -> requiere SWIFT
     */
    public static List<String> determinarOpcionesTipoPago(String moneda, String pais, boolean tieneAba, boolean tieneSwift) {
        List<String> opciones = new java.util.ArrayList<>();
        String m = moneda != null ? moneda.trim().toUpperCase(java.util.Locale.ROOT) : "";
        String p = pais != null ? pais.trim().toUpperCase(java.util.Locale.ROOT) : "";
        String sugerido = calcularTipoPagoAutomatico(m, p);

        // Caso exclusivo MXN + MX: según la regla funcional acordada, la única opción válida es SPEI
        if ("MXN".equals(m) && "MX".equals(p)) {
            if (tieneSwift) {
                opciones.add(TIPO_PAGO_SPEI);
            }
            return opciones;
        }

        // Si el sugerido es ACH y tiene ABA, agregarlo como primera opción
        if (TIPO_PAGO_ACH.equals(sugerido) && tieneAba) {
            opciones.add(TIPO_PAGO_ACH);
        }

        // Si el sugerido requiere SWIFT y el proveedor lo tiene
        if (tieneSwift) {
            if (TIPO_PAGO_SPEI.equals(sugerido)) {
                opciones.add(TIPO_PAGO_SPEI);
            } else if (TIPO_PAGO_SPID.equals(sugerido)) {
                opciones.add(TIPO_PAGO_SPID);
            }
        }

        // Si el sugerido no era ACH pero tiene ABA en USD
        if (!opciones.contains(TIPO_PAGO_ACH) && tieneAba && "USD".equalsIgnoreCase(m)) {
            opciones.add(TIPO_PAGO_ACH);
        }

        // WIRE está disponible para transferencias con SWIFT si no es pago nacional MXN
        if (tieneSwift && !opciones.contains(TIPO_PAGO_WIRE) && !"MXN".equals(m)) {
            opciones.add(TIPO_PAGO_WIRE);
        }

        // Si el proveedor tiene SWIFT y es USD, asegurar que SPID esté disponible
        if (tieneSwift && "USD".equalsIgnoreCase(m) && !opciones.contains(TIPO_PAGO_SPID)) {
            opciones.add(TIPO_PAGO_SPID);
        }

        return opciones;
    }

    /**
     * Sobrecarga de compatibilidad para código/tests que solo envían flags ABA y SWIFT.
     */
    public static List<String> determinarOpcionesTipoPago(boolean tieneAba, boolean tieneSwift) {
        List<String> opciones = new java.util.ArrayList<>();
        if (tieneAba) {
            opciones.add(TIPO_PAGO_ACH);
        }
        if (tieneSwift) {
            opciones.add(TIPO_PAGO_WIRE);
            opciones.add(TIPO_PAGO_SPID);
            opciones.add(TIPO_PAGO_SPEI);
        }
        return opciones;
    }
}