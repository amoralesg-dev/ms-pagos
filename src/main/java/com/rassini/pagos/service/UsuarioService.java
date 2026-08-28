package com.rassini.pagos.service;

import com.rassini.pagos.dto.LoginRequest;
import com.rassini.pagos.dto.LoginResponse;

import com.rassini.pagos.dto.BuDTO;
import java.util.List;

public interface UsuarioService {
    LoginResponse login(LoginRequest request);
    List<BuDTO> obtenerBusUsuario(String usuario);
}
