package com.minimarket.service.impl;

import com.minimarket.entity.Carrito;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.CarritoRepository;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.security.util.SecurityUtils;
import com.minimarket.service.CarritoService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarritoServiceImpl implements CarritoService {

    private final CarritoRepository carritoRepository;
    private final UsuarioRepository usuarioRepository;

    public CarritoServiceImpl(CarritoRepository carritoRepository, UsuarioRepository usuarioRepository) {
        this.carritoRepository = carritoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<Carrito> findAll() {
        if (SecurityUtils.isCliente()) {
            return findByCurrentUsuario();
        }
        return carritoRepository.findAll();
    }

    @Override
    public Carrito findById(Long id) {
        Carrito carrito = carritoRepository.findById(id).orElse(null);
        if (carrito == null) {
            return null;
        }
        assertOwnership(carrito);
        return carrito;
    }

    @Override
    public Carrito save(Carrito carrito) {
        if (SecurityUtils.isCliente()) {
            carrito.setUsuario(getCurrentUsuario());
        }
        return carritoRepository.save(carrito);
    }

    @Override
    public void deleteById(Long id) {
        Carrito carrito = findById(id);
        if (carrito != null) {
            carritoRepository.deleteById(id);
        }
    }

    @Override
    public List<Carrito> findByUsuarioId(Long usuarioId) {
        return carritoRepository.findByUsuarioId(usuarioId);
    }

    private List<Carrito> findByCurrentUsuario() {
        Usuario usuario = getCurrentUsuario();
        return carritoRepository.findByUsuarioId(usuario.getId());
    }

    private Usuario getCurrentUsuario() {
        String username = SecurityUtils.getCurrentUsername();
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no encontrado"));
    }

    private void assertOwnership(Carrito carrito) {
        if (SecurityUtils.isCliente()) {
            String username = SecurityUtils.getCurrentUsername();
            if (!carrito.getUsuario().getUsername().equals(username)) {
                throw new AccessDeniedException("No puede acceder al carrito de otro usuario");
            }
        }
    }
}
