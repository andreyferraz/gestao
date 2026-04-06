package com.andreyferraz.gestao.module.cliente;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteService {

	private final ClienteRepository clienteRepository;

	public Cliente criar(Cliente cliente) {
		if (cliente.getId() == null) {
			cliente.setId(UUID.randomUUID());
		}
		return clienteRepository.save(cliente);
	}

	public List<Cliente> listarTodos() {
		return (List<Cliente>) clienteRepository.findAll();
	}

	public Cliente buscarPorId(UUID id) {
		return clienteRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Cliente nao encontrado para o id: " + id));
	}

	public Cliente atualizar(UUID id, Cliente clienteAtualizado) {
		Cliente clienteExistente = buscarPorId(id);

		clienteExistente.setNome(clienteAtualizado.getNome());
		clienteExistente.setDominioAplicacao(clienteAtualizado.getDominioAplicacao());
		clienteExistente.setDataVencimentoDominio(clienteAtualizado.getDataVencimentoDominio());
		clienteExistente.setAtivo(clienteAtualizado.getAtivo());

		return clienteRepository.save(clienteExistente);
	}

	public void remover(UUID id) {
		if (!clienteRepository.existsById(id)) {
			throw new NoSuchElementException("Cliente nao encontrado para o id: " + id);
		}
		clienteRepository.deleteById(id);
	}

}
