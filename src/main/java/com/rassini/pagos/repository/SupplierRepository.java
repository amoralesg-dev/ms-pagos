package com.rassini.pagos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rassini.pagos.entity.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findFirstByErpIdQadAndBusinessUnitCode(String supplierCode, String businessUnitCode);

    

    List<Supplier> findByErpIdQad(String erpIdQad);
    
    List<Supplier> findByBusinessUnitCodeAndAccountNumber(
        String businessUnitCode,
        String accountNumber);

    List<Supplier> findByBusinessUnitCode(String businessUnitCode);

    List<Supplier> findByBusinessUnitCodeAndAccountNumberIsNotNull(
        String businessUnitCode);
    
    @Query("""
        SELECT s
        FROM Supplier s
        WHERE s.businessUnitCode = :empresa
        AND s.accountNumber IS NOT NULL
        AND s.accountNumber LIKE CONCAT('%', :ultimos8)
        """)
    List<Supplier> findByEmpresaAndAccountNumberEndsWith(
        @Param("empresa") String empresa,
        @Param("ultimos8") String ultimos8);



    
    @Query(value = """
        SELECT *
        FROM suppliers s
        WHERE s.business_unit_code = :businessUnitCode
          AND s.account_number IS NOT NULL
          AND TRIM(s.account_number) <> ''
          AND s.account_number LIKE CONCAT('%', RIGHT(:accountNumber, 8), '%')
        """, nativeQuery = true)
    List<Supplier> findByBusinessUnitCodeAndAccountNumberLast8(
            @Param("businessUnitCode") String businessUnitCode,
            @Param("accountNumber") String accountNumber);


}