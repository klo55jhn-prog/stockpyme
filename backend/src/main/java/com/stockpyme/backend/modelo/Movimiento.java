package com.stockpyme.backend.modelo;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Registro inmutable de un cambio de stock (D6). No tiene setters:
 * se crea con el constructor y nunca se modifica ni se borra.
 */
@Entity
@Table(name = "movimiento")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoMovimiento tipo;

    /** Con signo (D1): positiva en ENTRADA, negativa en SALIDA, cualquiera en AJUSTE. */
    @Column(nullable = false, updatable = false)
    private int cantidad;

    @Column(updatable = false)
    private String motivo;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id", updatable = false)
    private Producto producto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", updatable = false)
    private Usuario usuario;

    protected Movimiento() {
    }

    public Movimiento(TipoMovimiento tipo, int cantidad, String motivo, Producto producto, Usuario usuario) {
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.producto = producto;
        this.usuario = usuario;
        this.fecha = Instant.now();
    }

    public Long getId() { return id; }
    public TipoMovimiento getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public String getMotivo() { return motivo; }
    public Instant getFecha() { return fecha; }
    public Producto getProducto() { return producto; }
    public Usuario getUsuario() { return usuario; }
}
