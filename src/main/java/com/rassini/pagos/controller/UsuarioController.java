package com.rassini.pagos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rassini.pagos.dto.LoginRequest;
import com.rassini.pagos.dto.LoginResponse;
import com.rassini.pagos.service.UsuarioService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.rassini.pagos.dto.BuDTO;
import java.util.List;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = usuarioService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{usuario}/bus")
    public ResponseEntity<List<BuDTO>> obtenerBus(@PathVariable String usuario) {
        return ResponseEntity.ok(usuarioService.obtenerBusUsuario(usuario));
    }
}
