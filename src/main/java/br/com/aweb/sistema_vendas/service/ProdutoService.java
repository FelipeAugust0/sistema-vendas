package br.com.aweb.sistema_vendas.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.aweb.sistema_vendas.model.Produto;
import br.com.aweb.sistema_vendas.repository.ProdutoRepository;
import jakarta.transaction.Transactional;

@Service
public class ProdutoService {

    @Autowired
    ProdutoRepository produtoRepository;

    // CREATE
    @Transactional
    public Produto salvar(Produto produto) {
        return produtoRepository.save(produto);
    }

    // READ
    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    public Optional<Produto> buscarPorId(Long id) {
        return produtoRepository.findById(id);
    }

    // UPDATE
    @Transactional
    public Produto atualizar(Long id, Produto produtoAtualizado) {
        var optionalProduto = buscarPorId(id);
        if (!optionalProduto.isPresent())
            throw new IllegalArgumentException("Produto não encontrado.");

        var produtoExistente = optionalProduto.get();

        produtoExistente.setNome(produtoAtualizado.getNome());
        produtoExistente.setDescricao(produtoAtualizado.getDescricao());
        produtoExistente.setPreco(produtoAtualizado.getPreco());
        produtoExistente.setQuantidadeEmEstoque(produtoAtualizado.getQuantidadeEmEstoque());

        var produtoSalvo = produtoRepository.save(produtoExistente);
        return produtoSalvo;

    }

    // DELETE
    @Transactional
    public void excluir(Long id) {
        var optionalProduto = buscarPorId(id);
        if (!optionalProduto.isPresent())
            throw new IllegalArgumentException("Produto não encontrado.");

        produtoRepository.deleteById(id);
    }

}
