package com.rassini.pagos.suppliers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.rassini.pagos.util.BankDataMasker;

class BankDataMaskerTest {

    @Test
    void mantieneUltimos4() {
        assertEquals("************5678", BankDataMasker.mask("1234123412345678"));
        assertEquals("*****0123", BankDataMasker.mask("021000123"));
    }

    @Test
    void valoresCortosYNulos() {
        assertNull(BankDataMasker.mask(null));
        assertEquals("", BankDataMasker.mask(""));
        assertEquals("***", BankDataMasker.mask("123"));
        assertEquals("****", BankDataMasker.mask("1234"));
        assertEquals("*2345", BankDataMasker.mask(" 12345 "));
    }
}
