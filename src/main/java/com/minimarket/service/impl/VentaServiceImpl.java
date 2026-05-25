package com.minimarket.service.impl;

import com.minimarket.entity.Usuario;
import com.minimarket.entity.Venta;
import com.minimarket.repository.UsuarioRepository;
import com.minimarket.repository.VentaRepository;
import com.minimarket.security.util.SecurityUtils;
import com.minimarket.service.VentaService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;

    public VentaServiceImpl(VentaRepository ventaRepository, UsuarioRepository usuarioRepository) {
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<Venta> findAll() {
        if (SecurityUtils.isCliente()) {
            return findByCurrentUsuario();
        }
        return ventaRepository.findAll();
    }

    @Override
    public Venta findById(Long id) {
        Venta venta = ventaRepository.findById(id).orElse(null);
        if (venta == null) {
            return null;
        }
        assertOwnership(venta);
        return venta;
    }

    @Override
    public Venta save(Venta venta) {
        if (SecurityUtils.isCliente()) {
            venta.setUsuario(getCurrentUsuario());
        }
        return ventaRepository.save(venta);
    }

    @Override
    public List<Venta> findByUsuarioId(Long usuarioId) {
        return ventaRepository.findByUsuarioId(usuarioId);
    }

    private List<Venta> findByCurrentUsuario() {
        Usuario usuario = getCurrentUsuario();
        return ventaRepository.findByUsuarioId(usuario.getId());
    }

    private Usuario getCurrentUsuario() {
        String username = SecurityUtils.getCurrentUsername();
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no encontrado"));
    }

    private void assertOwnership(Venta venta) {
        if (SecurityUtils.isCliente()) {
            String username = SecurityUtils.getCurrentUsername();
            if (!venta.getUsuario().getUsername().equals(username)) {
                throw new AccessDeniedException("No puede acceder a la venta de otro usuario");
            }
        }
    }
}
