package com.rassini.pagos.repository;

import com.rassini.pagos.entity.PagosArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PagosArchivoRepository extends JpaRepository<PagosArchivo, Long> {

    
    List<PagosArchivo> findByTipoPagoIsNull();

    @Query("""
    SELECT p FROM PagosArchivo p
    WHERE p.tipoPago IS NULL
    AND (:codigoProveedor IS NULL OR :codigoProveedor = '' OR p.codigoProveedor = :codigoProveedor)
    AND (:rfcBeneficiario IS NULL OR :rfcBeneficiario = '' OR p.rfcBeneficiario = :rfcBeneficiario)
    """)
    List<PagosArchivo> filtrar(
            @Param("codigoProveedor") String codigoProveedor,
            @Param("rfcBeneficiario") String rfcBeneficiario);

    
}