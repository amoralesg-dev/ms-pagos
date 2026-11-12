package com.rassini.pagos.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rassini.pagos.entity.SupplierVersion;

public interface SupplierVersionRepository extends JpaRepository<SupplierVersion, Long> {

    Page<SupplierVersion> findBySupplierId(Long supplierId, Pageable pageable);

    Optional<SupplierVersion> findBySupplierIdAndVersionNumber(Long supplierId, Integer versionNumber);

    long countBySupplierId(Long supplierId);

    @Query("SELECT v.supplierId, COUNT(v) FROM SupplierVersion v WHERE v.supplierId IN :ids GROUP BY v.supplierId")
    List<Object[]> countBySupplierIds(@Param("ids") Collection<Long> ids);
}
