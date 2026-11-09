package com.stockpyme.backend.controlador;

import com.stockpyme.backend.dto.LoginRequest;
import com.stockpyme.backend.dto.LoginResponse;
import com.stockpyme.backend.dto.UsuarioDto;
import com.stockpyme.backend.seguridad.UsuarioAutenticado;
import com.stockpyme.backend.servicio.UsuarioService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Público. La app lo usa para comprobar que la dirección del servidor es correcta. */
    @GetMapping("/salud")
    public Map<String, String> salud() {
        return Map.of("estado", "ok", "servicio", "StockPyme API");
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest datos) {
        return usuarioService.login(datos);
    }

    @GetMapping("/auth/yo")
    public UsuarioDto yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuarioService.yo(usuario);
    }
}
