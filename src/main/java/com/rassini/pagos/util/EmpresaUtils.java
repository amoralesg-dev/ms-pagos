package com.rassini.pagos.util;

import java.util.HashMap;
import java.util.Map;

public final class EmpresaUtils {

    private EmpresaUtils() {
    }

    private static final Map<String, String> EMPRESA_PADRE_MAP = construirMapaEmpresas();

    private static Map<String, String> construirMapaEmpresas() {

        Map<String, String> mapa = new HashMap<>();

        // Grupo 0111
        mapa.put("0111", "0111");
        mapa.put("0112", "0111");
        mapa.put("0114", "0111");

        // Grupo 09
        mapa.put("09", "09");
        mapa.put("02", "09");
        mapa.put("72", "09");

        // Empresas independientes
        mapa.put("0301", "0301");
        mapa.put("1000", "1000");
        mapa.put("1850", "1850");

        return mapa;
    }
}
