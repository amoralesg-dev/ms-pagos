package com.rassini.pagos.service.impl;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.rassini.pagos.dto.LoginRequest;
import com.rassini.pagos.dto.LoginResponse;
import com.rassini.pagos.entity.Usuario;
import com.rassini.pagos.exception.BusinessException;
import com.rassini.pagos.repository.UsuarioRepository;
import com.rassini.pagos.service.UsuarioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsuarioAndActivoTrue(request.getUsuario())
                .orElseThrow(() -> new BusinessException("Credenciales inválidas"));

        String pass = passwordEncoder.encode(request.getPassword());
        log.info("Usuario pass: {}", pass);
        // Validamos que el password almacenado en BD esté encriptado (formato BCrypt)
        if (usuario.getPassword() == null || !usuario.getPassword().startsWith("$2")) {
            throw new BusinessException("La contraseña no tiene el formato encriptado correcto");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new BusinessException("Credenciales inválidas");
        }

        return LoginResponse.builder()
                .id(usuario.getId())
                .usuario(usuario.getUsuario())
                .nombre(usuario.getNombre())
                .bu(usuario.getBu())
                .rol(usuario.getRol())
                .fechaCreacion(usuario.getFechaCreacion())
                .activo(usuario.getActivo())
                .build();
    }

    @Override
    public java.util.List<com.rassini.pagos.dto.BuDTO> obtenerBusUsuario(String username) {
        Usuario usuario = usuarioRepository.findByUsuarioAndActivoTrue(username)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado o inactivo"));

        String buStr = usuario.getBu();
        if (buStr == null || buStr.isBlank()) {
            return java.util.List.of();
        }

        java.util.List<com.rassini.pagos.dto.BuDTO> list = new java.util.ArrayList<>();
        if ("ALL".equalsIgnoreCase(buStr.trim())) {
            list.add(new com.rassini.pagos.dto.BuDTO("ALL", "ALL"));
            for (String p : com.rassini.pagos.util.EmpresaUtils.obtenerTodasEmpresasReales()) {
                list.add(new com.rassini.pagos.dto.BuDTO(p, p));
            }
        } else {
            for (String b : com.rassini.pagos.util.EmpresaUtils.obtenerEmpresasBusqueda(buStr)) {
                list.add(new com.rassini.pagos.dto.BuDTO(b, b));
            }
        }
        return list;
    }
}
