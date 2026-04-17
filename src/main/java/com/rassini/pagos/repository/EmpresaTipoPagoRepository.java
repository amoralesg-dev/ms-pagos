package com.rassini.pagos.repository;

import com.rassini.pagos.entity.EmpresaTipoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmpresaTipoPagoRepository extends JpaRepository<EmpresaTipoPago, Integer> {

    List<EmpresaTipoPago> findByEmpresa(String empresa);

    boolean existsByEmpresaAndTipoPago_DealType(String empresa, String dealType);
}