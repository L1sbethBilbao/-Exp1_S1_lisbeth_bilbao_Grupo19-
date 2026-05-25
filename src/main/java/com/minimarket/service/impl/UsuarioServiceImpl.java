package com.minimarket.service.impl;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    @Override
    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public Optional<Usuario> findByUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @Override
    public Usuario save(Usuario usuario) {
        if (usuario.getId() == null) {
            validateNewUser(usuario);
        } else {
            preservePasswordIfMissing(usuario);
        }

        encodePasswordIfNeeded(usuario);
        usuario.setRoles(resolveRolesFromDatabase(usuario.getRoles()));

        return usuarioRepository.save(usuario);
    }

    @Override
    public void deleteById(Long id) {
        usuarioRepository.deleteById(id);
    }

    private void validateNewUser(Usuario usuario) {
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contrasena es obligatoria al crear un usuario");
        }
    }

    private void preservePasswordIfMissing(Usuario usuario) {
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            usuarioRepository.findById(usuario.getId()).ifPresent(existing ->
                    usuario.setPassword(existing.getPassword()));
        }
    }

    private void encodePasswordIfNeeded(Usuario usuario) {
        String password = usuario.getPassword();
        if (password != null && !password.isBlank() && !password.startsWith("$2")) {
            usuario.setPassword(passwordEncoder.encode(password));
        }
    }

    /**
     * Mitiga mass assignment: solo se asignan roles que existen en la base de datos.
     */
    private Set<Rol> resolveRolesFromDatabase(Set<Rol> rolesRequest) {
        if (rolesRequest == null || rolesRequest.isEmpty()) {
            return new HashSet<>();
        }
        return rolesRequest.stream()
                .map(Rol::getNombre)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .map(rolRepository::findByNombre)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }
}
