package com.rassini.pagos.repository;

import com.rassini.pagos.entity.EquivalencesDealType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquivalencesDealTypeRepository extends JpaRepository<EquivalencesDealType, Long> {
}
