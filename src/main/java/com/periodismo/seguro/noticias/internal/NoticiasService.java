package com.periodismo.seguro.noticias.internal;

import com.periodismo.seguro.noticias.NoticiasApi;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
class NoticiasService implements NoticiasApi {
    private final ApplicationEventPublisher eventos;

    NoticiasService(ApplicationEventPublisher eventos) { this.eventos = eventos; }

    /** Pendiente HU-09: al guardar, publicar new NoticiaCreada(id, categoria) con eventos.publishEvent. */
    @Override
    public String crear(String autorCuentaId, String autorSeudonimo, String titulo, String cuerpo, String categoria) {
        throw new UnsupportedOperationException("HU-09 pendiente");
    }

    @Override public List<String> listar(int pagina, int tamano) { return List.of(); }
    @Override public List<String> buscar(String texto, String categoria) { return List.of(); }
    @Override public Optional<String> obtener(String id) { return Optional.empty(); }
    @Override public boolean editar(String autorCuentaId, String id, String cuerpo) { return false; }
    @Override public boolean eliminar(String autorCuentaId, String id, boolean confirmado) { return false; }
    @Override public List<String> categorias() { return List.of(); }
}
