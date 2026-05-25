package com.minimarket.service.impl;

import com.minimarket.entity.Categoria;
import com.minimarket.repository.CategoriaRepository;
import com.minimarket.repository.ProductoRepository;
import com.minimarket.service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaServiceImpl implements CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Override
    public List<Categoria> findAll() {
        return categoriaRepository.findAll();
    }

    @Override
    public Categoria findById(Long id) {
        return categoriaRepository.findById(id).orElse(null);
    }

    @Override
    public Categoria save(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    @Override
    public void deleteById(Long id) {
        if (!productoRepository.findByCategoriaId(id).isEmpty()) {
            throw new IllegalArgumentException(
                    "No se puede eliminar la categoria porque tiene productos asociados");
        }
        categoriaRepository.deleteById(id);
    }
}
