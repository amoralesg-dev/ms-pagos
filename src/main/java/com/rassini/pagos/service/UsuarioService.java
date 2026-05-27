package com.rassini.pagos.service;

import com.rassini.pagos.dto.LoginRequest;
import com.rassini.pagos.dto.LoginResponse;

public interface UsuarioService {
    LoginResponse login(LoginRequest request);
}
