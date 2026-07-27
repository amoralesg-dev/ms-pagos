package com.rassini.pagos.util;

import java.util.List;
import java.util.Map;
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

        // Grupo 0111
        mapa.put("0112", "0111");
        mapa.put("0103", "0111");
        mapa.put("0109", "0111");
        mapa.put("0110", "0111");
        mapa.put("0111", "0111");
        mapa.put("0114", "0111");
        mapa.put("0115", "0111");
        mapa.put("0117", "0111");
        mapa.put("0120", "0111");

        // Grupo 09
        mapa.put("02", "09");
        mapa.put("72", "09");
        mapa.put("09", "09");
        mapa.put("10", "09");

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
}