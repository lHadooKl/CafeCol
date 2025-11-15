package com.cafecol.entities;

import jakarta.persistence.*;
import jakarta.json.bind.annotation.JsonbTransient;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idUsuario;

    @Column(unique = true, nullable = false, length = 50)
    private String nombreUsuario;

    @Column(nullable = false, length = 100)
    private String contrasena;

    @Column(nullable = false, length = 20)
    private String rol;


    public Usuario() {}

    public Usuario(String nombreUsuario, String contraseña, String rol) {
        this.nombreUsuario = nombreUsuario;
        this.contrasena = contraseña;
        this.rol = rol;
    }

    // Getters y Setters
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
