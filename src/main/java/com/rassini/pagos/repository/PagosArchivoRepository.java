package com.rassini.pagos.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rassini.pagos.entity.PagosArchivo;

public interface PagosArchivoRepository extends JpaRepository<PagosArchivo, Long> {

    boolean existsByNombreArchivoAndMontoAndCodigoProveedorAndFechaEnvio(String nombreArchivo, String monto, String codigoProveedor, String fechaEnvio);

    List<PagosArchivo> findByEmpresaAndNombreArchivoEnvioIsNull(String empresa);

    @Query("SELECT p FROM PagosArchivo p WHERE (p.nombreArchivoEnvio IS NULL OR p.nombreArchivoEnvio = '') AND p.empresa = :empresa ORDER BY p.nombreArchivo")
    List<PagosArchivo> findPendientesParaValidar(@Param("empresa") String empresa);

    long countByEmpresaAndTipoPagoIsNull(String empresa);

    @Query("""
    SELECT p FROM PagosArchivo p
    WHERE ( p.nombreArchivoEnvio IS NULL OR p.nombreArchivoEnvio = '' )
    AND p.empresa = :empresa
    AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
    AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
    """)
    List<PagosArchivo> filtrar(
            @Param("empresa") String empresa,
            @Param("codigoProveedor") String codigoProveedor,
            @Param("rfcBeneficiario") String rfcBeneficiario);

    @Query("""
    SELECT p FROM PagosArchivo p
    WHERE ( p.nombreArchivoEnvio IS NULL OR p.nombreArchivoEnvio = '' )
    AND p.empresa = :empresa
    AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
    AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
    """)
    Page<PagosArchivo> filtrarPaginado(
            @Param("empresa") String empresa,
            @Param("codigoProveedor") String codigoProveedor,
            @Param("rfcBeneficiario") String rfcBeneficiario,
            Pageable pageable);
    
    @Query("""
    SELECT p FROM PagosArchivo p
    WHERE ( p.nombreArchivoEnvio IS NOT NULL AND p.nombreArchivoEnvio <> '' )
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
    SELECT p FROM PagosArchivo p
    WHERE (p.nombreArchivoEnvio IS NULL OR p.nombreArchivoEnvio = '')
    AND p.empresa = :empresa
    AND p.tipoPago IS NOT NULL
    ORDER BY p.nombreArchivo, p.tipoPago.dealType
    """)
    List<PagosArchivo> findPendientesPorEnviar(@Param("empresa") String empresa);

}
