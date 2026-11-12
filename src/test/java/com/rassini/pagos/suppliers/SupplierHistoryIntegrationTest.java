package com.rassini.pagos.suppliers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.rassini.pagos.entity.Supplier;
import com.rassini.pagos.repository.SupplierRepository;

/** Integracion con H2 (perfil test, ddl create-drop). Datos ficticios. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SupplierHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private Long supplierId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM suppliers_ver");
        supplierRepository.deleteAll();
        Supplier s = new Supplier();
        s.setSupplierCode("777001");
        s.setErpIdQad("777001");
        s.setSupplierName("PROVEEDOR PRUEBA");
        s.setRfc("PPR010101AAA");
        s.setBusinessUnitCode("0111");
        s.setAccountNumber("TESTACCT00009999");
        s.setBeneficiaryBankName("BANCO NUEVO");
        s.setBankCurrency("MXN");
        s.setRoutingCodeSwift("NEWSWIFTXX");
        supplierRepository.save(s);
        supplierId = s.getId();
        for (int n = 1; n <= 3; n++) {
            jdbc.update("INSERT INTO suppliers_ver (id, supplier_id, version_number, supplier_code, supplier_name, "
                    + "account_number, beneficiary_bank_name, routing_code_swift, changed_fields, version_source, created_at) "
                    + "VALUES (?, ?, ?, '777001', 'PROVEEDOR PRUEBA', 'TESTACCT00009999', ?, 'OLDSWIFTXX', "
                    + "'beneficiary_bank_name,routing_code_swift', 'GRAPHITE', CURRENT_TIMESTAMP)",
                    n, supplierId, n, "BANCO V" + n);
        }
    }

    @Test
    void flujoCompleto() throws Exception {
        mockMvc.perform(get("/suppliers").param("supplierName", "prueba"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].versionCount").value(3))
                .andExpect(jsonPath("$.content[0].bankCurrency").value("MXN"))
                .andExpect(jsonPath("$.content[0].accountNumber").value("************9999"));

        mockMvc.perform(get("/suppliers").param("erpIdQad", "777001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].erpIdQad").value("777001"));

        mockMvc.perform(get("/suppliers").param("erpIdQad", "NO_EXISTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/suppliers/" + supplierId + "/versions").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.current").value(true))
                .andExpect(jsonPath("$.versions.content.length()").value(2))
                .andExpect(jsonPath("$.versions.content[0].versionNumber").value(3))
                .andExpect(jsonPath("$.versions.totalElements").value(3));

        mockMvc.perform(get("/suppliers/" + supplierId + "/versions/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versionBank.beneficiaryBankName").value("BANCO V2"))
                .andExpect(jsonPath("$.currentBank.beneficiaryBankName").value("BANCO NUEVO"))
                .andExpect(jsonPath("$.versionBank.accountNumber").value("TESTACCT00009999"))
                .andExpect(jsonPath("$.diffs[?(@.field=='routing_code_swift')].differsFromCurrent").value(true));

        mockMvc.perform(get("/suppliers/" + supplierId + "/versions/9")).andExpect(status().isNotFound());
        mockMvc.perform(get("/suppliers/987654/versions")).andExpect(status().isNotFound());
        mockMvc.perform(get("/suppliers/" + supplierId + "/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(true));
    }
}
