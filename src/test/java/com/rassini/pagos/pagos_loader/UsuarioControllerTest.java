package com.rassini.pagos.pagos_loader;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.rassini.pagos.entity.Usuario;
import com.rassini.pagos.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    public void testObtenerBusUsuario_UnaBu() throws Exception {
        Usuario usuario = Usuario.builder()
                .usuario("usuario1")
                .nombre("Usuario Uno")
                .bu("0111")
                .rol("FINANZAS")
                .activo(true)
                .build();

        when(usuarioRepository.findByUsuarioAndActivoTrue("usuario1")).thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/usuarios/usuario1/bus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("0111"))
                .andExpect(jsonPath("$[0].descripcion").value("0111"));
    }

    @Test
    public void testObtenerBusUsuario_MultiplesBus() throws Exception {
        Usuario usuario = Usuario.builder()
                .usuario("usuario2")
                .nombre("Usuario Dos")
                .bu("0111, 09, 1000")
                .rol("FINANZAS")
                .activo(true)
                .build();

        when(usuarioRepository.findByUsuarioAndActivoTrue("usuario2")).thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/usuarios/usuario2/bus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("0111"))
                .andExpect(jsonPath("$[1].codigo").value("09"))
                .andExpect(jsonPath("$[2].codigo").value("1000"));
    }

    @Test
    public void testObtenerBusUsuario_TodasBus() throws Exception {
        Usuario usuario = Usuario.builder()
                .usuario("usuarioAll")
                .nombre("Usuario All")
                .bu("ALL")
                .rol("ADMIN")
                .activo(true)
                .build();

        when(usuarioRepository.findByUsuarioAndActivoTrue("usuarioAll")).thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/usuarios/usuarioAll/bus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("ALL"))
                .andExpect(jsonPath("$[0].descripcion").value("ALL"))
                .andExpect(jsonPath("$.length()").value(7)); // ALL, plus 6 BUs: 0111, 0301, 09, 1000, 1850, 99
    }
}
