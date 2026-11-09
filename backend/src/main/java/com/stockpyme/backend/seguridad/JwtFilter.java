package com.stockpyme.backend.seguridad;

import com.stockpyme.backend.repositorio.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee la cabecera "Authorization: Bearer <token>" de cada petición.
 * Si el token es válido y el usuario sigue activo, lo marca como autenticado con su rol.
 * El rol se lee de la base de datos, no del token: si el propietario desactiva a un
 * empleado, deja de tener acceso al momento, aunque su token no haya caducado.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            String token = cabecera.substring(7);
            jwtService.leerUsuarioId(token)
                    .flatMap(usuarioRepository::findById)
                    .filter(usuario -> usuario.isActivo())
                    .ifPresent(usuario -> {
                        var principal = new UsuarioAutenticado(usuario.getId(), usuario.getNombre(), usuario.getRol());
                        var permisos = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name()));
                        var autenticacion = new UsernamePasswordAuthenticationToken(principal, null, permisos);
                        SecurityContextHolder.getContext().setAuthentication(autenticacion);
                    });
        }
        chain.doFilter(request, response);
    }
}
