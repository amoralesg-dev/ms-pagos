package com.rassini.pagos.suppliers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.rassini.pagos.dto.SupplierFieldDiffDTO;
import com.rassini.pagos.dto.SupplierSummaryDTO;
import com.rassini.pagos.dto.SupplierVersionDetailDTO;
import com.rassini.pagos.dto.SupplierVersionHistoryDTO;
import com.rassini.pagos.entity.Supplier;
import com.rassini.pagos.entity.SupplierVersion;
import com.rassini.pagos.exception.ResourceNotFoundException;
import com.rassini.pagos.repository.SupplierRepository;
import com.rassini.pagos.repository.SupplierVersionRepository;
import com.rassini.pagos.service.impl.SupplierHistoryServiceImpl;

class SupplierHistoryServiceImplTest {

    private SupplierRepository supplierRepo;
    private SupplierVersionRepository versionRepo;
    private SupplierHistoryServiceImpl service;

    @BeforeEach
    void setUp() {
        supplierRepo = mock(SupplierRepository.class);
        versionRepo = mock(SupplierVersionRepository.class);
        service = new SupplierHistoryServiceImpl(supplierRepo, versionRepo);
    }

    static Supplier supplier() {
        Supplier s = new Supplier();
        s.setId(10L);
        s.setSupplierCode("1001");
        s.setErpIdQad("1001");
        s.setSupplierName("ACME SA");
        s.setRfc("ACM010101AAA");
        s.setBusinessUnitCode("0111");
        s.setAccountNumber("TESTACCT00001234");
        s.setBeneficiaryBankName("BANK NEW");
        s.setRoutingCodeSwift("NEWSWIFT");
        s.setRoutingCodeAba("000000001");
        s.setBankCurrency("USD");
        return s;
    }

    static SupplierVersion version(int n) {
        SupplierVersion v = new SupplierVersion();
        v.setId((long) n);
        v.setSupplierId(10L);
        v.setVersionNumber(n);
        v.setSupplierCode("1001");
        v.setSupplierName("ACME SA");
        v.setAccountNumber("TESTACCT00001234");
        v.setBeneficiaryBankName("BANK OLD");
        v.setRoutingCodeSwift("OLDSWIFT");
        v.setRoutingCodeAba("000000001");
        v.setBankCurrency("USD");
        v.setChangedFields("beneficiary_bank_name, routing_code_swift");
        v.setVersionSource("GRAPHITE");
        v.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        return v;
    }

    @Test
    void buscarProveedores_enmascaraYCuentaVersiones() {
        PageRequest pr = PageRequest.of(0, 20);
        when(supplierRepo.buscarParaHistorial("1001", "1001", null, null, pr))
                .thenReturn(new PageImpl<>(List.of(supplier()), pr, 1));
        when(versionRepo.countBySupplierIds(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[] { 10L, 3L }));

        SupplierSummaryDTO dto = service.buscarProveedores(" 1001 ", " 1001 ", "", null, pr).getContent().get(0);
        assertEquals("************1234", dto.getAccountNumber());
        assertEquals("USD", dto.getBankCurrency());
        assertEquals(3L, dto.getVersionCount());
        assertEquals("BANK NEW", dto.getBankName());
    }

    @Test
    void obtenerVersiones_incluyeVigenteSeparado() {
        PageRequest pr = PageRequest.of(0, 20);
        when(supplierRepo.findById(10L)).thenReturn(Optional.of(supplier()));
        when(versionRepo.findBySupplierId(10L, pr)).thenReturn(new PageImpl<>(List.of(version(2), version(1)), pr, 2));

        SupplierVersionHistoryDTO h = service.obtenerVersiones(10L, pr);
        assertTrue(h.getCurrent().isCurrent());
        assertEquals(2, h.getVersions().getTotalElements());
        assertFalse(h.getVersions().getContent().get(0).isCurrent());
        assertEquals(List.of("beneficiary_bank_name", "routing_code_swift"),
                h.getVersions().getContent().get(0).getChangedFields());
    }

    @Test
    void obtenerVersiones_sinVersiones() {
        PageRequest pr = PageRequest.of(0, 20);
        when(supplierRepo.findById(10L)).thenReturn(Optional.of(supplier()));
        when(versionRepo.findBySupplierId(10L, pr)).thenReturn(new PageImpl<>(List.of(), pr, 0));
        assertTrue(service.obtenerVersiones(10L, pr).getVersions().getContent().isEmpty());
    }

    @Test
    void proveedorInexistente_404() {
        when(supplierRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerVersiones(99L, PageRequest.of(0, 1)));
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerDetalleVersion(99L, 1));
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerVigente(99L));
    }

    @Test
    void versionInexistente_404() {
        when(supplierRepo.findById(10L)).thenReturn(Optional.of(supplier()));
        when(versionRepo.findBySupplierIdAndVersionNumber(any(), any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerDetalleVersion(10L, 7));
    }

    @Test
    void detalle_diffYValoresCompletos() {
        when(supplierRepo.findById(10L)).thenReturn(Optional.of(supplier()));
        when(versionRepo.findBySupplierIdAndVersionNumber(10L, 1)).thenReturn(Optional.of(version(1)));

        SupplierVersionDetailDTO d = service.obtenerDetalleVersion(10L, 1);
        assertEquals("BANK OLD", d.getVersionBank().getBeneficiaryBankName());
        assertEquals("BANK NEW", d.getCurrentBank().getBeneficiaryBankName());
        assertEquals("TESTACCT00001234", d.getVersionBank().getAccountNumber());
        assertEquals("000000001", d.getCurrentBank().getRoutingCodeAba());
        assertEquals(12, d.getDiffs().size());
        SupplierFieldDiffDTO bank = d.getDiffs().stream().filter(x -> x.getField().equals("beneficiary_bank_name")).findFirst().orElseThrow();
        assertTrue(bank.isDiffersFromCurrent());
        assertTrue(bank.isChangedInVersion());
        SupplierFieldDiffDTO acct = d.getDiffs().stream().filter(x -> x.getField().equals("account_number")).findFirst().orElseThrow();
        assertFalse(acct.isDiffersFromCurrent());
        assertFalse(acct.isChangedInVersion());
    }
}
