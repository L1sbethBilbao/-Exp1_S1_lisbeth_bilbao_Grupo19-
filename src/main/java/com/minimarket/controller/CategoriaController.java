package com.minimarket.controller;

import com.minimarket.entity.Categoria;
import com.minimarket.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public List<Categoria> listarCategorias() {
        return categoriaService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Categoria> obtenerCategoriaPorId(@PathVariable Long id) {
        Categoria categoria = categoriaService.findById(id);
        return (categoria != null) ? ResponseEntity.ok(categoria) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Categoria guardarCategoria(@Valid @RequestBody Categoria categoria) {
        return categoriaService.save(categoria);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Categoria> actualizarCategoria(@PathVariable Long id, @Valid @RequestBody Categoria categoria) {
        Categoria categoriaExistente = categoriaService.findById(id);
        if (categoriaExistente != null) {
            categoriaExistente.setNombre(categoria.getNombre());
            return ResponseEntity.ok(categoriaService.save(categoriaExistente));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Long id) {
        Categoria categoria = categoriaService.findById(id);
        if (categoria != null) {
            categoriaService.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
