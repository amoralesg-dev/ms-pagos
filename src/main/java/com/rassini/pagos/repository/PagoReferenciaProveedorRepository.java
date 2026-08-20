package com.rassini.pagos.repository;

import com.rassini.pagos.entity.PagoReferenciaProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagoReferenciaProveedorRepository
        extends JpaRepository<PagoReferenciaProveedor, Long> {

}