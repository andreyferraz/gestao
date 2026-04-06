package com.andreyferraz.gestao.module.usuario;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends CrudRepository<Usuario, UUID> {

	Optional<Usuario> findByUsername(String username);
}
