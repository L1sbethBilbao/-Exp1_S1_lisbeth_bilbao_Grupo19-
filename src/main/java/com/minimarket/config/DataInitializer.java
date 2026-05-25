package com.minimarket.config;

import com.minimarket.entity.*;
import com.minimarket.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    public static final String ROL_CLIENTE = "ROLE_CLIENTE";
    public static final String ROL_EMPLEADO = "ROLE_EMPLEADO";
    public static final String ROL_GERENTE = "ROLE_GERENTE";

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final CarritoRepository carritoRepository;
    private final VentaRepository ventaRepository;
    private final InventarioRepository inventarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            RolRepository rolRepository,
            UsuarioRepository usuarioRepository,
            CategoriaRepository categoriaRepository,
            ProductoRepository productoRepository,
            CarritoRepository carritoRepository,
            VentaRepository ventaRepository,
            InventarioRepository inventarioRepository,
            PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.carritoRepository = carritoRepository;
        this.ventaRepository = ventaRepository;
        this.inventarioRepository = inventarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByUsername("cliente1").isPresent()) {
            log.info("Datos iniciales ya cargados. Se omite la inicialización.");
            return;
        }

        log.info("Cargando datos iniciales del minimarket...");

        Rol rolCliente = crearRol(ROL_CLIENTE);
        Rol rolEmpleado = crearRol(ROL_EMPLEADO);
        Rol rolGerente = crearRol(ROL_GERENTE);

        Usuario cliente = crearUsuario("cliente1", "cliente123", rolCliente);
        crearUsuario("empleado1", "empleado123", rolEmpleado);
        crearUsuario("gerente1", "gerente123", rolGerente);

        Categoria bebidas = crearCategoria("Bebidas");
        Categoria snacks = crearCategoria("Snacks");

        Producto agua = crearProducto("Agua 500ml", 800.0, 50, bebidas);
        Producto papas = crearProducto("Papas fritas", 1200.0, 30, snacks);
        crearProducto("Jugo natural", 1500.0, 20, bebidas);

        crearCarrito(cliente, agua, 2);
        crearCarrito(cliente, papas, 1);

        crearVentaConDetalle(cliente, agua, 2, 800.0);

        Date hoy = new Date();
        crearInventario(agua, 50, "Entrada", hoy);
        crearInventario(papas, 30, "Entrada", hoy);

        log.info("Datos iniciales cargados correctamente.");
        log.info("Usuarios de prueba: cliente1/cliente123 | empleado1/empleado123 | gerente1/gerente123");
    }

    private Rol crearRol(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseGet(() -> {
                    Rol rol = new Rol();
                    rol.setNombre(nombre);
                    return rolRepository.save(rol);
                });
    }

    private Usuario crearUsuario(String username, String passwordPlano, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(passwordPlano));
        Set<Rol> roles = new HashSet<>();
        roles.add(rol);
        usuario.setRoles(roles);
        return usuarioRepository.save(usuario);
    }

    private Categoria crearCategoria(String nombre) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return categoriaRepository.save(categoria);
    }

    private Producto crearProducto(String nombre, Double precio, Integer stock, Categoria categoria) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setPrecio(precio);
        producto.setStock(stock);
        producto.setCategoria(categoria);
        return productoRepository.save(producto);
    }

    private void crearCarrito(Usuario usuario, Producto producto, int cantidad) {
        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);
        carrito.setProducto(producto);
        carrito.setCantidad(cantidad);
        carritoRepository.save(carrito);
    }

    private void crearVentaConDetalle(Usuario usuario, Producto producto, int cantidad, double precio) {
        Venta venta = new Venta();
        venta.setUsuario(usuario);
        venta.setFecha(new Date());

        DetalleVenta detalle = new DetalleVenta();
        detalle.setVenta(venta);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setPrecio(precio);

        venta.setDetalles(List.of(detalle));
        ventaRepository.save(venta);
    }

    private void crearInventario(Producto producto, int cantidad, String tipo, Date fecha) {
        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setCantidad(cantidad);
        inventario.setTipoMovimiento(tipo);
        inventario.setFechaMovimiento(fecha);
        inventarioRepository.save(inventario);
    }
}
