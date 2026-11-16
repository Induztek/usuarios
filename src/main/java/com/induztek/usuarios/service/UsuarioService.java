package com.induztek.usuarios.service;

import com.induztek.usuarios.dto.DatosActualizacion;
import com.induztek.usuarios.dto.DatosCambioContrasena;
import com.induztek.usuarios.dto.DatosLogin;
import com.induztek.usuarios.dto.DatosRegistro;
import com.induztek.usuarios.dto.UsuarioRespuesta;
import com.induztek.usuarios.model.Usuario;
import com.induztek.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Long ROL_TECNICO = 1L;
    private static final Long ROL_CLIENTE = 2L;
    private static final Long ROL_ADMINISTRADOR = 3L;

    private final UsuarioRepository usuarioRepository;

    public List<UsuarioRespuesta> listar(String buscar) {
        List<Usuario> usuarios = (buscar == null || buscar.isBlank())
                ? usuarioRepository.findAllByOrderByNombreAsc()
                : usuarioRepository.findByNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseOrderByNombreAsc(buscar, buscar);

        return usuarios.stream()
                .map(this::aRespuesta)
                .toList();
    }

    public UsuarioRespuesta obtener(Long id) {
        return aRespuesta(buscar(id));
    }

    public UsuarioRespuesta registrar(DatosRegistro datos) {
        validarDatos(datos.nombre(), datos.apellido(), datos.correo(), datos.rut(), datos.telefono(), datos.rolId());

        if (datos.contrasena() == null || datos.contrasena().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener mínimo 6 caracteres");
        }
        if (usuarioRepository.existsByCorreo(datos.correo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
        }
        if (usuarioRepository.existsByRut(datos.rut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese RUT");
        }

        Usuario nuevo = Usuario.builder()
                .nombre(datos.nombre())
                .apellido(datos.apellido())
                .correo(datos.correo())
                .contrasena(datos.contrasena())
                .rut(datos.rut())
                .telefono(datos.telefono())
                .empresaId(datos.empresaId())
                .rolId(datos.rolId())
                .build();
        return aRespuesta(usuarioRepository.save(nuevo));
    }

    public UsuarioRespuesta iniciarSesion(DatosLogin datos) {
        Usuario usuario = usuarioRepository.findByCorreo(datos.correo())
                .filter(u -> u.getContrasena().equals(datos.contrasena()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));
        return aRespuesta(usuario);
    }

    public UsuarioRespuesta actualizar(Long id, DatosActualizacion datos) {
        Usuario usuario = buscar(id);

        validarDatos(datos.nombre(), datos.apellido(), datos.correo(), datos.rut(), datos.telefono(), datos.rolId());

        if (usuarioRepository.existsByCorreoAndIdNot(datos.correo(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
        }
        if (usuarioRepository.existsByRutAndIdNot(datos.rut(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese RUT");
        }

        usuario.setNombre(datos.nombre());
        usuario.setApellido(datos.apellido());
        usuario.setCorreo(datos.correo());
        usuario.setRut(datos.rut());
        usuario.setTelefono(datos.telefono());
        usuario.setEmpresaId(datos.empresaId());
        usuario.setRolId(datos.rolId());
        return aRespuesta(usuarioRepository.save(usuario));
    }

    public void cambiarContrasena(Long id, DatosCambioContrasena datos) {
        Usuario usuario = buscar(id);

        if (!usuario.getContrasena().equals(datos.contrasenaActual())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contraseña actual es incorrecta");
        }
        if (datos.contrasenaNueva() == null || datos.contrasenaNueva().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener mínimo 6 caracteres");
        }

        usuario.setContrasena(datos.contrasenaNueva());
        usuarioRepository.save(usuario);
    }

    public void eliminar(Long id) {
        usuarioRepository.delete(buscar(id));
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private void validarDatos(String nombre, String apellido, String correo, String rut, String telefono, Long rolId) {
        if (vacio(nombre) || vacio(apellido) || vacio(correo) || vacio(rut) || vacio(telefono)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre, apellido, correo, RUT y teléfono son obligatorios");
        }
        if (!ROL_TECNICO.equals(rolId) && !ROL_CLIENTE.equals(rolId) && !ROL_ADMINISTRADOR.equals(rolId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol indicado no existe");
        }
    }

    private String nombreRol(Long rolId) {
        if (ROL_TECNICO.equals(rolId)) return "Técnico";
        if (ROL_CLIENTE.equals(rolId)) return "Cliente";
        if (ROL_ADMINISTRADOR.equals(rolId)) return "Administrador";
        return "";
    }

    private UsuarioRespuesta aRespuesta(Usuario usuario) {
        return new UsuarioRespuesta(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getRut(),
                usuario.getTelefono(),
                usuario.getEmpresaId(),
                "",
                usuario.getRolId(),
                nombreRol(usuario.getRolId()),
                null
        );
    }

    private boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}