package br.com.aweb.sistema_vendas.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aweb.sistema_vendas.model.Cliente;
import br.com.aweb.sistema_vendas.model.ItemPedido;
import br.com.aweb.sistema_vendas.model.Pedido;
import br.com.aweb.sistema_vendas.model.Produto;
import br.com.aweb.sistema_vendas.model.StatusPedido;
import br.com.aweb.sistema_vendas.repository.PedidoRepository;
import br.com.aweb.sistema_vendas.repository.ProdutoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ProdutoRepository produtoRepository) {

        this.pedidoRepository = pedidoRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public Pedido criarPedido(Cliente cliente) {

        Pedido pedido = new Pedido(cliente);

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public void adicionarItem(
            Long pedidoId,
            Long produtoId,
            Integer quantidade) {

        Optional<Pedido> optionalPedido =
                pedidoRepository.findById(pedidoId);

        Optional<Produto> optionalProduto =
                produtoRepository.findById(produtoId);

        if (optionalPedido.isEmpty()) {
            throw new IllegalArgumentException(
                    "Pedido não encontrado"
            );
        }

        if (optionalProduto.isEmpty()) {
            throw new IllegalArgumentException(
                    "Produto não encontrado"
            );
        }

        Pedido pedido = optionalPedido.get();
        Produto produto = optionalProduto.get();

        if (pedido.getStatus() != StatusPedido.ATIVO) {
            throw new IllegalStateException(
                    "Não é possível alterar um pedido cancelado"
            );
        }

        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException(
                    "Quantidade deve ser maior que zero"
            );
        }

        if (produto.getQuantidadeEmEstoque() < quantidade) {
            throw new IllegalStateException(
                    "Estoque insuficiente para o produto: "
                    + produto.getNome()
            );
        }

        ItemPedido item =
                new ItemPedido(produto, quantidade);

        item.setPedido(pedido);

        pedido.getItens().add(item);

        produto.setQuantidadeEmEstoque(
                produto.getQuantidadeEmEstoque() - quantidade
        );

        calcularValorTotal(pedido);

        pedidoRepository.save(pedido);
        produtoRepository.save(produto);
    }

    @Transactional
    public void removerItem(
            Long pedidoId,
            Long itemId) {

        Optional<Pedido> optionalPedido =
                pedidoRepository.findById(pedidoId);

        if (optionalPedido.isEmpty()) {
            throw new IllegalArgumentException(
                    "Pedido não encontrado"
            );
        }

        Pedido pedido = optionalPedido.get();

        if (pedido.getStatus() != StatusPedido.ATIVO) {
            throw new IllegalStateException(
                    "Não é possível alterar um pedido cancelado"
            );
        }

        ItemPedido itemParaRemover = null;

        for (ItemPedido item : pedido.getItens()) {

            if (item.getId().equals(itemId)) {
                itemParaRemover = item;
                break;
            }
        }

        if (itemParaRemover == null) {
            throw new IllegalArgumentException(
                    "Item não encontrado no pedido"
            );
        }

        Produto produto =
                itemParaRemover.getProduto();

        produto.setQuantidadeEmEstoque(
                produto.getQuantidadeEmEstoque()
                + itemParaRemover.getQuantidade()
        );

        pedido.getItens().remove(itemParaRemover);

        calcularValorTotal(pedido);

        pedidoRepository.save(pedido);
        produtoRepository.save(produto);
    }

    @Transactional
    public void cancelarPedido(Long pedidoId) {

        Optional<Pedido> optionalPedido =
                pedidoRepository.findById(pedidoId);

        if (optionalPedido.isEmpty()) {
            throw new IllegalArgumentException(
                    "Pedido não encontrado"
            );
        }

        Pedido pedido = optionalPedido.get();

        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new IllegalStateException(
                    "Pedido já está cancelado"
            );
        }

        for (ItemPedido item : pedido.getItens()) {

            Produto produto = item.getProduto();

            produto.setQuantidadeEmEstoque(
                    produto.getQuantidadeEmEstoque()
                    + item.getQuantidade()
            );

            produtoRepository.save(produto);
        }

        pedido.setStatus(StatusPedido.CANCELADO);

        pedidoRepository.save(pedido);
    }

    private void calcularValorTotal(Pedido pedido) {

        BigDecimal total = BigDecimal.ZERO;

        for (ItemPedido item : pedido.getItens()) {

            total = total.add(item.getSubtotal());
        }

        pedido.setValorTotal(total);
    }

    public Optional<Pedido> buscarPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    public List<Pedido> listarPorStatus(StatusPedido status) {
        return pedidoRepository.findByStatus(status);
    }
}
