package br.com.aweb.sistema_vendas.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.repository.ClienteRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Cliente salvar(Cliente cliente) {

        // CADASTRO
        if (cliente.getId() == null) {

            if (clienteRepository.existsByEmail(cliente.getEmail())) {
                throw new IllegalArgumentException(
                    "Já existe um cliente cadastrado com este e-mail."
                );
            }

            if (clienteRepository.existsByCpf(cliente.getCpf())) {
                throw new IllegalArgumentException(
                    "Já existe um cliente cadastrado com este CPF."
                );
            }

        }

        // EDIÇÃO
        else {

            if (clienteRepository.existsByEmailAndIdNot(
                    cliente.getEmail(),
                    cliente.getId())) {

                throw new IllegalArgumentException(
                    "Já existe outro cliente cadastrado com este e-mail."
                );
            }

            if (clienteRepository.existsByCpfAndIdNot(
                    cliente.getCpf(),
                    cliente.getId())) {

                throw new IllegalArgumentException(
                    "Já existe outro cliente cadastrado com este CPF."
                );
            }
        }

        return clienteRepository.save(cliente);
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Cliente buscarPorId(Long id) {

        return clienteRepository.findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Cliente não encontrado."
                )
            );
    }

    @Transactional
    public void excluir(Long id) {

        Cliente cliente = buscarPorId(id);

        clienteRepository.delete(cliente);
    }
}