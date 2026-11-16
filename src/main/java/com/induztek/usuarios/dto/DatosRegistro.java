package com.induztek.usuarios.dto;

public record DatosRegistro(
        String nombre,
        String apellido,
        String correo,
        String contrasena,
        String rut,
        String telefono,
        Long empresaId,
        Long rolId
) {
}