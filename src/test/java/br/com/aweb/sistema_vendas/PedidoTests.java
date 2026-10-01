package br.com.aweb.sistema_vendas;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.aweb.sistema_vendas.model.*;
import br.com.aweb.sistema_vendas.repository.*;
import br.com.aweb.sistema_vendas.service.PedidoService;
import jakarta.persistence.EntityManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PedidoTests {

    @Autowired PedidoService pedidoService;
    @Autowired PedidoRepository pedidoRepository;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager entityManager;
    @LocalServerPort int port;

    private Cliente cliente;
    private Produto produto;
    private Pedido pedido;
    private HttpClient http;

    @BeforeEach
    void preparar() {
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
        clienteRepository.deleteAll();

        cliente = new Cliente();
        cliente.setNome("Cliente Teste");
        cliente.setEmail("cliente@example.com");
        cliente.setCpf("52998224725");
        cliente.setTelefone("16999999999");
        cliente.setLogradouro("Rua Teste");
        cliente.setBairro("Centro");
        cliente.setCidade("São Carlos");
        cliente.setUf("SP");
        cliente.setCep("13560-000");
        cliente = clienteRepository.save(cliente);

        produto = new Produto();
        produto.setNome("Produto Teste");
        produto.setDescricao("Produto para teste");
        produto.setPreco(new BigDecimal("12.50"));
        produto.setQuantidadeEmEstoque(10);
        produto = produtoRepository.save(produto);
        pedido = pedidoService.criarPedido(cliente);
        http = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    @Test
    void adicionarERemoverAtualizaEstoqueTotalVersaoEExcluiOrfao() {
        Long versaoInicial = pedido.getVersion();
        pedidoService.adicionarItem(pedido.getId(), produto.getId(), 3);
        assertEquals(7, estoque());
        assertEquals(new BigDecimal("37.50"), buscarPedido().getValorTotal());
        assertTrue(buscarPedido().getVersion() > versaoInicial);

        Long itemId = primeiroItemId();
        Long versaoComItem = buscarPedido().getVersion();
        pedidoService.removerItem(pedido.getId(), itemId);
        assertEquals(10, estoque());
        assertEquals(new BigDecimal("0.00"), buscarPedido().getValorTotal());
        assertTrue(buscarPedido().getVersion() > versaoComItem);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            assertTrue(buscarPedido().getItens().isEmpty());
            assertNull(entityManager.find(ItemPedido.class, itemId));
        });
    }

    @Test
    void precoDoItemPermaneceIgualAposAlterarProduto() {
        pedidoService.adicionarItem(pedido.getId(), produto.getId(), 2);
        produto = produtoRepository.findById(produto.getId()).orElseThrow();
        produto.setPreco(new BigDecimal("20.00"));
        produtoRepository.save(produto);
        pedidoService.adicionarItem(pedido.getId(), produto.getId(), 1);
        assertEquals(new BigDecimal("45.00"), buscarPedido().getValorTotal());
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            ItemPedido item = buscarPedido().getItens().getFirst();
            assertEquals(new BigDecimal("25.00"), item.getSubtotal());
            assertDoesNotThrow(item::toString);
            assertDoesNotThrow(item::hashCode);
        });
    }

    @Test
    void entradasInvalidasNaoAlteramEstoqueNemTotal() {
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.adicionarItem(pedido.getId(), produto.getId(), 0));
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.adicionarItem(pedido.getId(), produto.getId(), -1));
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.adicionarItem(pedido.getId(), produto.getId(), null));
        assertThrows(IllegalStateException.class,
                () -> pedidoService.adicionarItem(pedido.getId(), produto.getId(), 11));
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.adicionarItem(pedido.getId(), Long.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.adicionarItem(Long.MAX_VALUE, produto.getId(), 1));
        assertEquals(10, estoque());
        assertEquals(new BigDecimal("0.00"), buscarPedido().getValorTotal());
    }

    @Test
    void itemDeOutroPedidoNaoPodeSerRemovido() {
        pedidoService.adicionarItem(pedido.getId(), produto.getId(), 2);
        Long itemId = primeiroItemId();
        Pedido outro = pedidoService.criarPedido(cliente);
        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.removerItem(outro.getId(), itemId));
        assertEquals(8, estoque());
        assertEquals(new BigDecimal("25.00"), buscarPedido().getValorTotal());
    }

    @Test
    void cancelarDevolveEstoqueUmaVezEPreservaHistorico() {
        pedidoService.adicionarItem(pedido.getId(), produto.getId(), 2);
        pedidoService.adicionarItem(pedido.getId(), produto.getId(), 3);
        Long itemId = primeiroItemId();
        Long versao = buscarPedido().getVersion();
        pedidoService.cancelarPedido(pedido.getId());
        assertEquals(10, estoque());
        assertEquals(StatusPedido.CANCELADO, buscarPedido().getStatus());
        assertEquals(new BigDecimal("62.50"), buscarPedido().getValorTotal());
        assertTrue(buscarPedido().getVersion() > versao);
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                assertEquals(2, buscarPedido().getItens().size()));
        assertThrows(IllegalStateException.class,
                () -> pedidoService.cancelarPedido(pedido.getId()));
        assertThrows(IllegalStateException.class,
                () -> pedidoService.adicionarItem(pedido.getId(), produto.getId(), 1));
        assertThrows(IllegalStateException.class,
                () -> pedidoService.removerItem(pedido.getId(), itemId));
        assertEquals(10, estoque());
    }

    @Test
    void viewsEFormulariosFuncionamComItens() throws Exception {
        assertEquals(200, get("/pedidos/novo").statusCode());
        HttpResponse<String> criado = post("/pedidos/novo", "clienteId=" + cliente.getId());
        assertEquals(302, criado.statusCode());
        assertTrue(criado.headers().firstValue("location").orElseThrow().contains("/pedidos/edit/"));
        assertEquals(200, get("/pedidos/edit/" + pedido.getId()).statusCode());
        assertEquals(302, post("/pedidos/" + pedido.getId() + "/adicionar-item",
                "produtoId=" + produto.getId() + "&quantidade=3").statusCode());
        HttpResponse<String> edit = get("/pedidos/edit/" + pedido.getId());
        assertEquals(200, edit.statusCode());
        assertTrue(edit.body().contains("R$ 37.50"));
        Long itemId = primeiroItemId();
        assertTrue(edit.body().contains("/pedidos/" + pedido.getId() + "/remover-item/" + itemId));
        assertEquals(200, get("/pedidos/detalhes/" + pedido.getId()).statusCode());
        assertEquals(200, get("/pedidos").statusCode());
        assertEquals(200, get("/pedidos/cancelar/" + pedido.getId()).statusCode());
        assertEquals(302, post("/pedidos/" + pedido.getId() + "/remover-item/" + itemId, "").statusCode());
        assertEquals(10, estoque());
        assertEquals(302, post("/pedidos/" + pedido.getId() + "/finalizar", "").statusCode());
    }

    @Test
    void errosAparecemNaTelaECancelamentoRepetidoNaoAlteraEstoque() throws Exception {
        HttpResponse<String> erro = post("/pedidos/" + pedido.getId() + "/adicionar-item",
                "produtoId=" + produto.getId() + "&quantidade=11");
        assertEquals(302, erro.statusCode());
        assertTrue(get("/pedidos/edit/" + pedido.getId()).body().contains("Estoque insuficiente"));
        pedidoService.adicionarItem(pedido.getId(), produto.getId(), 2);
        Long itemId = primeiroItemId();
        assertEquals(302, post("/pedidos/" + pedido.getId() + "/remover-item/" + Long.MAX_VALUE, "").statusCode());
        assertTrue(get("/pedidos/edit/" + pedido.getId()).body().contains("Item não encontrado"));
        assertEquals(302, post("/pedidos/cancelar/" + pedido.getId(), "").statusCode());
        assertEquals(302, post("/pedidos/cancelar/" + pedido.getId(), "").statusCode());
        assertTrue(get("/pedidos").body().contains("Pedido já está cancelado"));
        HttpResponse<String> cancelado = get("/pedidos/cancelar/" + pedido.getId());
        assertEquals(200, cancelado.statusCode());
        assertFalse(cancelado.body().contains("Confirmar Cancelamento"));
        assertEquals(400, get("/pedidos/edit/" + pedido.getId()).statusCode());
        assertEquals(302, post("/pedidos/" + pedido.getId() + "/adicionar-item",
                "produtoId=" + produto.getId() + "&quantidade=1").statusCode());
        assertTrue(get("/pedidos").body().contains("Não é possível alterar"));
        assertEquals(302, post("/pedidos/" + pedido.getId() + "/remover-item/" + itemId, "").statusCode());
        assertEquals(10, estoque());
        assertEquals(404, get("/pedidos/detalhes/" + Long.MAX_VALUE).statusCode());
        assertEquals(404, get("/pedidos/cancelar/" + Long.MAX_VALUE).statusCode());
    }

    private Pedido buscarPedido() {
        return pedidoService.buscarPorId(pedido.getId()).orElseThrow();
    }

    private int estoque() {
        return produtoRepository.findById(produto.getId()).orElseThrow().getQuantidadeEmEstoque();
    }

    private Long primeiroItemId() {
        return new TransactionTemplate(transactionManager).execute(status ->
                buscarPedido().getItens().getFirst().getId());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
