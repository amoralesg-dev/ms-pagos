package com.rassini.pagos.repository;

import com.rassini.pagos.entity.CatalogoTipoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CatalogoTipoPagoRepository extends JpaRepository<CatalogoTipoPago, Integer> {

    Optional<CatalogoTipoPago> findByDealType(String dealType);
}