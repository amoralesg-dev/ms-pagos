package com.rassini.pagos.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rassini.pagos.entity.PagosArchivo;

public interface PagosArchivoRepository extends JpaRepository<PagosArchivo, Long> {

    
    List<PagosArchivo> findByNombreArchivoEnvioIsNull();

    @Query("SELECT p FROM PagosArchivo p WHERE p.nombreArchivoEnvio IS NULL OR p.nombreArchivoEnvio = '' ORDER BY p.nombreArchivo")
    List<PagosArchivo> findPendientesParaValidar();

    long countByTipoPagoIsNull();

    @Query("""
    SELECT p FROM PagosArchivo p
    WHERE ( p.nombreArchivoEnvio IS NULL OR p.nombreArchivoEnvio = '' )
    AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
    AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
    """)
    List<PagosArchivo> filtrar(
            @Param("codigoProveedor") String codigoProveedor,
            @Param("rfcBeneficiario") String rfcBeneficiario);

    @Query("""
    SELECT p FROM PagosArchivo p
    WHERE ( p.nombreArchivoEnvio IS NULL OR p.nombreArchivoEnvio = '' )
    AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
    AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
    """)
    Page<PagosArchivo> filtrarPaginado(
            @Param("codigoProveedor") String codigoProveedor,
            @Param("rfcBeneficiario") String rfcBeneficiario,
            Pageable pageable);
}