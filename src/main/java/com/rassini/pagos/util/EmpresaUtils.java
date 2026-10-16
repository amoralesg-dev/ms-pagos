package com.rassini.pagos.util;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.rassini.pagos.exception.BusinessException;
import org.apache.logging.log4j.Logger;

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
     * Determina de forma centralizada si una empresa (o su empresa padre)
     * participa en la funcionalidad de selección ACH/WIRE.
     */
    public static boolean aplicaTransferenciaAchWire(String empresa) {
        return true;
    }

    // Constantes de Business Unit oficiales
    public static final String FRENOS = "1000";
    public static final String OC = "0111";
    public static final String BYPASA = "0301";
    public static final String BREAKES = "1850";
    public static final String PN = "09";
    public static final String PN99 = "99";

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
     * Normaliza un tipo de pago a su valor canónico o null si no es permitido.
     */
    public static String normalizarTipoPago(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return null;
        }
        String trim = tipo.trim();
        for (String perm : TIPOS_PAGO_SELECCIONADOS_PERMITIDOS) {
            if (perm.equalsIgnoreCase(trim)) {
                return perm;
            }
        }
        return null;
    }

    /**
     * Formatea el tipo de pago a su etiqueta descriptiva con el routing code asociado:
     * - ACH   -> "ACH - ABA"
     * - WIRE  -> "WIRE - SWIFT"
     * - SPID  -> "SPID - SWIFT"
     * - SPEI  -> "SPEI - SWIFT"
     * Si es nulo o vacío retorna cadena vacía o el valor original si no coincide.
     */
    public static String formatearTipoTransferencia(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "";
        }
        String norm = normalizarTipoPago(tipo);
        if (TIPO_PAGO_ACH.equals(norm)) {
            return "ACH - ABA";
        }
        if (TIPO_PAGO_WIRE.equals(norm)) {
            return "WIRE - SWIFT";
        }
        if (TIPO_PAGO_SPID.equals(norm)) {
            return "SPID - SWIFT";
        }
        if (TIPO_PAGO_SPEI.equals(norm)) {
            return "SPEI - SWIFT";
        }
        return tipo.trim();
    }

    /**
     * Calcula automáticamente el tipo de pago según las reglas de negocio específicas por Business Unit:
     * 1. BRAKES (1850):
     *    Todos los pagos se calculan automáticamente como ACH (los pocos WIRE se modifican manualmente).
     * 2. BYPASA (0301), SUSPENSIONES (09), FRENOS (1000) y CORPO (0111):
     *    - Moneda MXN -> SPEI.
     *    - Moneda USD:
     *        * Proveedor MX + Banco MX + SWIFT -> SPID.
     *        * Proveedor distinto de MX + Banco distinto de MX + SWIFT -> WIRE.
     *        * Casos mixtos o sin condiciones específicas -> WIRE (fallback seguro existente).
     *        * No se asigna ACH automáticamente (excepciones se ajustan manualmente).
     * 3. Negocios fuera del alcance o regla general:
     *    - Moneda USD + país banco US -> ACH.
     *    - Moneda USD + país banco MX -> SPID.
     *    - Moneda MXN + país banco MX -> SPEI.
     *    - Fallback seguro general: WIRE.
     */
    public static String calcularTipoPagoAutomatico(
            String empresa,
            String moneda,
            String paisProveedor,
            String paisBanco,
            boolean tieneSwift) {

        String buPadre = obtenerEmpresaPadreSeguro(empresa);
        String m = moneda != null ? moneda.trim().toUpperCase(java.util.Locale.ROOT) : "";
        String pProv = paisProveedor != null ? paisProveedor.trim().toUpperCase(java.util.Locale.ROOT) : "";
        String pBanco = paisBanco != null ? paisBanco.trim().toUpperCase(java.util.Locale.ROOT) : "";

        // 1. Regla especial BRAKES (1850)
        if (BREAKES.equals(buPadre)) {
            if ("MXN".equals(m)) {
                return TIPO_PAGO_SPEI;
            }
            return TIPO_PAGO_ACH;
        }

        // 2. Reglas específicas para BYPASA, SUSPENSIONES, FRENOS y CORPO
        if (BYPASA.equals(buPadre) || PN.equals(buPadre) || FRENOS.equals(buPadre) || OC.equals(buPadre)) {
            if ("MXN".equals(m)) {
                return TIPO_PAGO_SPEI;
            }
            if ("USD".equals(m)) {
                if ("MX".equals(pProv) && "MX".equals(pBanco) && tieneSwift) {
                    return TIPO_PAGO_SPID;
                }
                if (!pProv.isEmpty() && !pBanco.isEmpty() && !"MX".equals(pProv) && !"MX".equals(pBanco) && tieneSwift) {
                    return TIPO_PAGO_WIRE;
                }
                // Casos mixtos, sin SWIFT o no definidos -> Fallback seguro WIRE
                return TIPO_PAGO_WIRE;
            }
            return TIPO_PAGO_WIRE;
        }

        // 3. Reglas generales preexistentes para empresas fuera del alcance
        if ("USD".equals(m) && "US".equals(pBanco)) {
            return TIPO_PAGO_ACH;
        }
        if ("USD".equals(m) && "MX".equals(pBanco)) {
            return TIPO_PAGO_SPID;
        }
        if ("MXN".equals(m) && "MX".equals(pBanco)) {
            return TIPO_PAGO_SPEI;
        }
        return TIPO_PAGO_WIRE;
    }

    /**
     * Determina las opciones de transferencia disponibles (capacidad operativa)
     * según los datos bancarios del proveedor (ABA y SWIFT).
     *
     * No modifica el tipo sugerido por regla de negocio.
     */
    public static List<String> determinarOpcionesTipoPago(String moneda, String pais, boolean tieneAba, boolean tieneSwift) {
        String m = moneda != null ? moneda.trim().toUpperCase(java.util.Locale.ROOT) : "";
        String p = pais != null ? pais.trim().toUpperCase(java.util.Locale.ROOT) : "";

        List<String> opciones = new java.util.ArrayList<>();

        if ("MXN".equals(m)) {
            if (tieneSwift) {
                opciones.add(TIPO_PAGO_SPEI);
            }
            return opciones;
        }

        if ("USD".equals(m)) {
            if ("US".equals(p)) {
                // Caso US: si tiene ABA -> ACH; si tiene SWIFT -> WIRE, SPID
                if (tieneAba) {
                    opciones.add(TIPO_PAGO_ACH);
                }
                if (tieneSwift) {
                    opciones.add(TIPO_PAGO_WIRE);
                    opciones.add(TIPO_PAGO_SPID);
                }
            } else if ("MX".equals(p)) {
                // Caso MX: si tiene SWIFT -> SPID, WIRE; si tiene ABA -> ACH
                if (tieneSwift) {
                    opciones.add(TIPO_PAGO_SPID);
                    opciones.add(TIPO_PAGO_WIRE);
                }
                if (tieneAba) {
                    opciones.add(TIPO_PAGO_ACH);
                }
            } else {
                // Terceros países (ej. CA): si tiene SWIFT -> WIRE, SPID; si tiene ABA -> ACH
                if (tieneSwift) {
                    opciones.add(TIPO_PAGO_WIRE);
                    opciones.add(TIPO_PAGO_SPID);
                }
                if (tieneAba) {
                    opciones.add(TIPO_PAGO_ACH);
                }
            }
            return opciones;
        }

        // Fallback para otras monedas
        if (tieneSwift) {
            opciones.add(TIPO_PAGO_WIRE);
        }
        if (tieneAba) {
            opciones.add(TIPO_PAGO_ACH);
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

    /**
     * Resuelve de forma centralizada los dos conceptos separados:
     * 1. Tipo sugerido por regla oficial de negocio de la Business Unit.
     * 2. Opciones disponibles por capacidad bancaria (ABA y SWIFT).
     *
     * Valida si el método seleccionado/sugerido cuenta con el routing code requerido,
     * emitiendo la advertencia correspondiente en caso de faltar.
     */
    public static ResolucionTipoPago resolverTipoPago(
            String empresa,
            String moneda,
            String paisProveedor,
            String paisBanco,
            boolean tieneAba,
            boolean tieneSwift,
            String tipoPersistido) {

        String tipoSugerido = calcularTipoPagoAutomatico(empresa, moneda, paisProveedor, paisBanco, tieneSwift);
        List<String> opciones = determinarOpcionesTipoPago(moneda, paisBanco, tieneAba, tieneSwift);

        String persistidoNorm = normalizarTipoPago(tipoPersistido);
        String tipoEfectivo = persistidoNorm != null ? persistidoNorm : tipoSugerido;

        // Validación técnica de datos bancarios:
        if (TIPO_PAGO_ACH.equals(tipoEfectivo)) {
            if (!tieneAba) {
                String adv = tipoEfectivo.equals(tipoSugerido)
                        ? "El tipo sugerido ACH requiere Routing Code ABA."
                        : "El tipo seleccionado ACH requiere Routing Code ABA.";
                return new ResolucionTipoPago(
                        opciones,
                        tipoSugerido,
                        tipoEfectivo,
                        ResolucionTipoPago.Clasificacion.FALTA_ABA,
                        adv
                );
            }
        } else {
            // WIRE, SPID, SPEI requieren SWIFT
            if (!tieneSwift) {
                String adv = tipoEfectivo.equals(tipoSugerido)
                        ? "El tipo sugerido " + tipoEfectivo + " requiere Routing Code SWIFT."
                        : "El tipo seleccionado " + tipoEfectivo + " requiere Routing Code SWIFT.";
                return new ResolucionTipoPago(
                        opciones,
                        tipoSugerido,
                        tipoEfectivo,
                        ResolucionTipoPago.Clasificacion.FALTA_SWIFT,
                        adv
                );
            }
        }

        return new ResolucionTipoPago(
                opciones,
                tipoSugerido,
                tipoEfectivo,
                ResolucionTipoPago.Clasificacion.VALIDA,
                "Método " + tipoEfectivo + " válido con datos bancarios completos."
        );
    }

    /**
     * Sobrecarga de compatibilidad para resolverTipoPago.
     */
    public static ResolucionTipoPago resolverTipoPago(
            String moneda,
            String pais,
            boolean tieneAba,
            boolean tieneSwift,
            String tipoPersistido) {
        return resolverTipoPago(null, moneda, pais, pais, tieneAba, tieneSwift, tipoPersistido);
    }


    /**
     * Emite un log estructurado sin exponer información bancaria sensible (cuentas o códigos completos).
     */
    public static void logResolucion(
            Logger log,
            Object idPago,
            String archivo,
            String proveedor,
            String empresa,
            String moneda,
            String pais,
            boolean tieneAba,
            boolean tieneSwift,
            String tipoPersistido,
            ResolucionTipoPago resolucion) {

        if (log != null && log.isInfoEnabled()) {
            log.info("[RESOLUCION-TIPO-PAGO] idPago={} archivo={} proveedor={} empresa={} moneda={} pais={} tieneAba={} tieneSwift={} tipoPersistido={} opcionesValidas={} clasificacion={} motivo={}",
                    idPago != null ? idPago : "NUEVO",
                    archivo != null ? archivo : "",
                    proveedor != null ? proveedor : "",
                    empresa != null ? empresa : "",
                    moneda != null ? moneda : "",
                    pais != null ? pais : "",
                    tieneAba,
                    tieneSwift,
                    tipoPersistido != null ? tipoPersistido : "NULL",
                    resolucion != null ? resolucion.getOpcionesValidas() : "[]",
                    resolucion != null ? resolucion.getClasificacion() : "DESCONOCIDA",
                    resolucion != null ? resolucion.getMotivo() : "");
        }
    }
}