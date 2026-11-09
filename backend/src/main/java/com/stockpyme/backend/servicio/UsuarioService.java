package com.stockpyme.backend.servicio;

import com.stockpyme.backend.dto.LoginRequest;
import com.stockpyme.backend.dto.LoginResponse;
import com.stockpyme.backend.dto.UsuarioDto;
import com.stockpyme.backend.dto.UsuarioRequest;
import com.stockpyme.backend.error.ApiException;
import com.stockpyme.backend.modelo.Rol;
import com.stockpyme.backend.modelo.Usuario;
import com.stockpyme.backend.repositorio.UsuarioRepository;
import com.stockpyme.backend.seguridad.JwtService;
import com.stockpyme.backend.seguridad.UsuarioAutenticado;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inicio de sesión (CU-01) y gestión de empleados (CU-13). */
@Service
public class UsuarioService {

    private static final int LONGITUD_MINIMA_PASSWORD = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * El mismo mensaje sirve para email inexistente y contraseña incorrecta:
     * así nadie puede averiguar qué emails están registrados.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest datos) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(datos.email().trim())
                .filter(u -> passwordEncoder.matches(datos.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email o contraseña incorrectos"));
        if (!usuario.isActivo()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Tu cuenta está desactivada");
        }
        return new LoginResponse(jwtService.generarToken(usuario), UsuarioDto.de(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioDto yo(UsuarioAutenticado actual) {
        return usuarioRepository.findById(actual.id()).map(UsuarioDto::de)
                .orElseThrow(() -> ApiException.noEncontrado("El usuario no existe"));
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return usuarioRepository.findAllByOrderByNombreAsc().stream().map(UsuarioDto::de).toList();
    }

    @Transactional
    public UsuarioDto crear(UsuarioRequest datos) {
        String email = datos.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflicto("Ya hay un usuario con el email " + email);
        }
        comprobarPassword(datos.password(), true);
        Usuario usuario = new Usuario();
        usuario.setNombre(datos.nombre().trim());
        usuario.setEmail(email);
        usuario.setRol(datos.rol());
        usuario.setPasswordHash(passwordEncoder.encode(datos.password()));
        return UsuarioDto.de(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioDto editar(Long id, UsuarioRequest datos, UsuarioAutenticado actual) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("El usuario no existe"));
        String email = datos.email().trim().toLowerCase();
        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw ApiException.conflicto("Ya hay un usuario con el email " + email);
                });

        boolean esUnoMismo = id.equals(actual.id());
        boolean quitaPropietario = datos.rol() != Rol.PROPIETARIO || Boolean.FALSE.equals(datos.activo());
        if (esUnoMismo && quitaPropietario) {
            // Evita que el comercio se quede sin nadie que pueda administrarlo.
            throw ApiException.conflicto("No puedes quitarte el rol de propietario ni desactivarte");
        }

        usuario.setNombre(datos.nombre().trim());
        usuario.setEmail(email);
        usuario.setRol(datos.rol());
        if (datos.activo() != null) {
            usuario.setActivo(datos.activo());
        }
        if (datos.password() != null && !datos.password().isBlank()) {
            comprobarPassword(datos.password(), false);
            usuario.setPasswordHash(passwordEncoder.encode(datos.password()));
        }
        return UsuarioDto.de(usuarioRepository.save(usuario));
    }

    private static void comprobarPassword(String password, boolean obligatoria) {
        if (password == null || password.isBlank()) {
            if (obligatoria) {
                throw ApiException.peticionIncorrecta("La contraseña es obligatoria");
            }
            return;
        }
        if (password.length() < LONGITUD_MINIMA_PASSWORD) {
            throw ApiException.peticionIncorrecta(
                    "La contraseña debe tener al menos " + LONGITUD_MINIMA_PASSWORD + " caracteres");
        }
    }
}
