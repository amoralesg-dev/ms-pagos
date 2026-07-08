package com.rassini.pagos.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rassini.pagos.entity.PagosArchivo;

public interface PagosArchivoRepository extends JpaRepository<PagosArchivo, Long> {

        boolean existsByNombreArchivoAndMontoAndCodigoProveedorAndFechaEnvio(
                        String nombreArchivo,
                        String monto,
                        String codigoProveedor,
                        String fechaEnvio);

        List<PagosArchivo> findByEmpresaAndEstatus(
                        String empresa,
                        String estatus);

        List<PagosArchivo> findByEmpresaInAndEstatus(
                        List<String> empresas,
                        String estatus);

        @Query("""
                        SELECT p
                        FROM PagosArchivo p
                        WHERE p.estatus = 'PENDIENTE'
                        AND p.empresa = :empresa
                        ORDER BY p.nombreArchivo
                        """)
        List<PagosArchivo> findPendientesParaValidar(
                        @Param("empresa") String empresa);

        @Query("""
        SELECT p
        FROM PagosArchivo p
        WHERE p.estatus = 'PENDIENTE'
        AND p.empresa IN :empresas
        ORDER BY p.nombreArchivo
        """)
        List<PagosArchivo> findPendientesParaValidarMultiBu(
                @Param("empresas") List<String> empresas);

        @Query("""
        SELECT p
        FROM PagosArchivo p
        WHERE p.estatus = 'PENDIENTE'
        ORDER BY p.nombreArchivo
        """)
        List<PagosArchivo> findPendientesParaValidarAll();

        long countByEmpresaAndTipoPagoIsNull(String empresa);

        @Query("""
                        SELECT p
                        FROM PagosArchivo p
                        WHERE p.estatus = 'PENDIENTE'
                        AND p.empresa = :empresa
                        AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
                        AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
                        """)
        List<PagosArchivo> filtrar(
                        @Param("empresa") String empresa,
                        @Param("codigoProveedor") String codigoProveedor,
                        @Param("rfcBeneficiario") String rfcBeneficiario);

        @Query("""
                        SELECT p
                        FROM PagosArchivo p
                        WHERE p.estatus = 'PENDIENTE'
                        AND p.empresa = :empresa
                        AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
                        AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
                        AND (:tipoPago IS NULL OR :tipoPago = '' OR p.tipoPago.dealType = :tipoPago)
                        AND (
                            :estatus IS NULL
                            OR :estatus = ''
                            OR :estatus = 'Todos'
                            OR p.estatus = :estatus
                        )
                        """)
        Page<PagosArchivo> filtrarPaginado(
                        @Param("empresa") String empresa,
                        @Param("codigoProveedor") String codigoProveedor,
                        @Param("rfcBeneficiario") String rfcBeneficiario,
                        @Param("tipoPago") String tipoPago,
                        @Param("estatus") String estatus,
                        Pageable pageable);

        @Query("""
        SELECT p
        FROM PagosArchivo p
        WHERE p.estatus = 'PENDIENTE'
        AND p.empresa IN :empresas
        AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
        AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
        AND (:tipoPago IS NULL OR :tipoPago = '' OR p.tipoPago.dealType = :tipoPago)
        AND (
            :estatus IS NULL
            OR :estatus = ''
            OR :estatus = 'Todos'
            OR p.estatus = :estatus
        )
        """)
        Page<PagosArchivo> filtrarPaginadoMultiBu(
                @Param("empresas") List<String> empresas,
                @Param("codigoProveedor") String codigoProveedor,
                @Param("rfcBeneficiario") String rfcBeneficiario,
                @Param("tipoPago") String tipoPago,
                @Param("estatus") String estatus,
                Pageable pageable);
        
        @Query("""
        SELECT p
        FROM PagosArchivo p
        WHERE p.estatus = 'PENDIENTE'
        AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
        AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
        AND (:tipoPago IS NULL OR :tipoPago = '' OR p.tipoPago.dealType = :tipoPago)
        AND (
            :estatus IS NULL
            OR :estatus = ''
            OR :estatus = 'Todos'
            OR p.estatus = :estatus
        )
        """)
        Page<PagosArchivo> filtrarPaginadoAll(
                @Param("codigoProveedor") String codigoProveedor,
                @Param("rfcBeneficiario") String rfcBeneficiario,
                @Param("tipoPago") String tipoPago,
                @Param("estatus") String estatus,
                Pageable pageable);

        @Query("""
                        SELECT p
                        FROM PagosArchivo p
                        WHERE p.estatus = 'ENVIADO'
                        AND p.empresa = :empresa
                        AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
                        AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
                        """)
        Page<PagosArchivo> filtrarEnviadosPaginado(
                        @Param("empresa") String empresa,
                        @Param("codigoProveedor") String codigoProveedor,
                        @Param("rfcBeneficiario") String rfcBeneficiario,
                        Pageable pageable);
        
        @Query("""
            SELECT p
            FROM PagosArchivo p
            WHERE p.estatus = 'ENVIADO'
            AND p.empresa IN :empresas
            AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
            AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
            """)
    Page<PagosArchivo> filtrarEnviadosPaginadoMultiBu(
            @Param("empresas") List<String> empresas,
            @Param("codigoProveedor") String codigoProveedor,
            @Param("rfcBeneficiario") String rfcBeneficiario,
            Pageable pageable);

    @Query("""
            SELECT p
            FROM PagosArchivo p
            WHERE p.estatus = 'ENVIADO'
            AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
            AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
            """)
    Page<PagosArchivo> filtrarEnviadosPaginadoAll(
            @Param("codigoProveedor") String codigoProveedor,
            @Param("rfcBeneficiario") String rfcBeneficiario,
            Pageable pageable);

        @Query("""
                        SELECT p
                        FROM PagosArchivo p
                        WHERE p.estatus = 'ERROR'
                        AND p.empresa = :empresa
                        AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
                        AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
                        """)
        Page<PagosArchivo> filtrarErroresPaginado(
                        @Param("empresa") String empresa,
                        @Param("codigoProveedor") String codigoProveedor,
                        @Param("rfcBeneficiario") String rfcBeneficiario,
                        Pageable pageable);

        @Query("""
        SELECT p
        FROM PagosArchivo p
        WHERE p.estatus = 'ERROR'
        AND (
            :search IS NULL
            OR :search = ''
            OR LOWER(p.codigoProveedor) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.rfcBeneficiario) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.nombreBeneficiario) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.monto) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.moneda) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.referencia) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.nombreArchivo) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
        Page<PagosArchivo> filtrarErroresPaginadoAll(
                @Param("search") String search,
                Pageable pageable);
        

        @Query("""
        SELECT p
        FROM PagosArchivo p
        WHERE p.estatus = 'ERROR'
        AND p.empresa IN :empresas
        AND (
            :search IS NULL
            OR :search = ''
            OR LOWER(p.codigoProveedor) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.rfcBeneficiario) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.nombreBeneficiario) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.monto) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.moneda) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.referencia) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.nombreArchivo) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
        Page<PagosArchivo> filtrarErroresPaginadoMultiBu(
                @Param("empresas") List<String> empresas,
                @Param("search") String search,
                Pageable pageable);

        @Query("""
                        SELECT p
                        FROM PagosArchivo p
                        WHERE p.estatus = 'PENDIENTE'
                        AND p.empresa = :empresa
                        AND p.tipoPago IS NOT NULL
                        ORDER BY p.nombreArchivo, p.tipoPago.dealType
                        """)
        List<PagosArchivo> findPendientesPorEnviar(
                        @Param("empresa") String empresa);
        
        @Query("""
        SELECT p
        FROM PagosArchivo p
        WHERE p.estatus = 'PENDIENTE'
        AND p.empresa IN :empresas
        AND p.tipoPago IS NOT NULL
        ORDER BY p.nombreArchivo, p.tipoPago.dealType
        """)
        List<PagosArchivo> findPendientesPorEnviarMultiBu(
                @Param("empresas") List<String> empresas);

        
        @Query("""
        SELECT p
        FROM PagosArchivo p
        WHERE p.estatus = 'PENDIENTE'
        AND p.tipoPago IS NOT NULL
        ORDER BY p.nombreArchivo, p.tipoPago.dealType
        """)
        List<PagosArchivo> findPendientesPorEnviarAll();
        

        List<PagosArchivo> findByNombreArchivo(String nombreArchivo);

        List<PagosArchivo> findByNombreArchivoAndEstatus(String nombreArchivo,String estatus);
        
        List<PagosArchivo> findByNombreArchivoInAndEstatus(List<String> nombresArchivo,String estatus);
        
}