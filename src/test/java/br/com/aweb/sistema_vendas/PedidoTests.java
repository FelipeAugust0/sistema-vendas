package br.com.aweb.sistema_vendas;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import br.com.aweb.sistema_vendas.service.UsuarioService;
import jakarta.persistence.EntityManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PedidoTests {

    @Autowired PedidoService pedidoService;
    @Autowired PedidoRepository pedidoRepository;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired UsuarioService usuarioService;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager entityManager;
    @LocalServerPort int port;

    private Cliente cliente;
    private Produto produto;
    private Pedido pedido;
    private HttpClient http;
    private String csrfToken;

    @BeforeEach
    void preparar() {
        pedidoRepository.deleteAll();
        produtoRepository.deleteAll();
        clienteRepository.deleteAll();
        usuarioRepository.deleteAll();

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
        Usuario admin = new Usuario();
        admin.setNome("Administrador Teste");
        admin.setEmail("admin@example.com");
        admin.setSenha("senha-teste");
        admin.setRole(UsuarioRole.ADMIN);
        usuarioService.salvar(admin);

        try {
            LoginSession sessao = login("admin@example.com", "senha-teste");
            http = sessao.http();
            csrfToken = sessao.csrfToken();
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível autenticar o usuário de teste", e);
        }
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

    @Test
    void usuarioPadraoAcessaPedidosMasNaoCadastrosAdministrativos() throws Exception {
        Usuario padrao = new Usuario();
        padrao.setNome("Usuário Padrão");
        padrao.setEmail("padrao@example.com");
        padrao.setSenha("senha-padrao");
        padrao.setRole(UsuarioRole.PADRAO);
        usuarioService.salvar(padrao);

        LoginSession sessao = login("padrao@example.com", "senha-padrao");
        HttpResponse<String> lista = get(sessao.http(), "/pedidos");
        assertEquals(200, lista.statusCode());
        assertFalse(lista.body().contains("href=\"/produtos\""));
        assertFalse(lista.body().contains("href=\"/clientes\""));
        assertTrue(lista.body().contains("href=\"/logout\""));
        for (String path : new String[]{"/produtos", "/produtos/novo",
                "/produtos/edit/" + produto.getId(), "/produtos/delete/" + produto.getId(),
                "/clientes", "/clientes/novo", "/clientes/" + cliente.getId() + "/editar",
                "/clientes/" + cliente.getId() + "/excluir"}) {
            assertEquals(403, get(sessao.http(), path).statusCode(), path);
        }

        http = sessao.http();
        csrfToken = sessao.csrfToken();
        for (String path : new String[]{"/produtos/novo", "/produtos/edit/" + produto.getId(),
                "/produtos/delete/" + produto.getId(), "/clientes", "/clientes/" + cliente.getId() + "/excluir"}) {
            assertEquals(403, post(path, "").statusCode(), path);
        }
        assertEquals(200, get("/pedidos/novo").statusCode());
        assertEquals(302, post("/pedidos/novo", "clienteId=" + cliente.getId()).statusCode());
        assertEquals(200, get("/pedidos/edit/" + pedido.getId()).statusCode());
        assertEquals(302, post("/pedidos/" + pedido.getId() + "/adicionar-item",
                "produtoId=" + produto.getId() + "&quantidade=2").statusCode());
        assertEquals(8, estoque());
        assertEquals(200, get("/pedidos/detalhes/" + pedido.getId()).statusCode());
        assertEquals(302, post("/pedidos/" + pedido.getId() + "/remover-item/" + primeiroItemId(), "").statusCode());
        assertEquals(10, estoque());
        assertEquals(302, post("/pedidos/cancelar/" + pedido.getId(), "").statusCode());
        assertEquals(StatusPedido.CANCELADO, buscarPedido().getStatus());
    }

    @Test
    void loginIgnoraCaixaSenhaEhCriptografadaELogoutRedireciona() throws Exception {
        Usuario admin = (Usuario) usuarioService.loadUserByUsername("ADMIN@EXAMPLE.COM");
        assertNotEquals("senha-teste", admin.getPassword());
        assertTrue(admin.getPassword().startsWith("$2"));
        assertNotNull(login("ADMIN@EXAMPLE.COM", "senha-teste"));

        assertEquals(200, get("/logout").statusCode());
        assertEquals(200, get("/pedidos").statusCode(), "GET /logout apenas exibe a confirmação");
        HttpResponse<String> logout = post("/logout", "");
        assertEquals(302, logout.statusCode());
        assertTrue(logout.headers().firstValue("location").orElse("").contains("/login?logout"));
        assertTrue(get("/login?logout").body().contains("Sessão encerrada com sucesso."));
        assertEquals(302, get("/pedidos").statusCode());
    }

    @Test
    void visitanteAcessaLoginERecursosMasPrecisaAutenticarParaOperar() throws Exception {
        HttpClient visitante = HttpClient.newHttpClient();
        assertEquals(200, get(visitante, "/login").statusCode());
        assertEquals(200, get(visitante, "/css/bootstrap.min.css").statusCode());
        assertEquals(200, get(visitante, "/js/bootstrap.bundle.min.js").statusCode());
        for (String path : new String[]{"/", "/pedidos", "/produtos", "/clientes", "/logout"}) {
            HttpResponse<String> resposta = get(visitante, path);
            assertEquals(302, resposta.statusCode(), path);
            assertTrue(resposta.headers().firstValue("location").orElseThrow().endsWith("/login"));
        }
    }

    @Test
    void senhaInvalidaMostraErroEPostSemCsrfNaoAlteraDados() throws Exception {
        HttpClient visitante = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        String token = extrairCsrf(get(visitante, "/login").body());
        HttpResponse<String> falha = enviarPost(visitante, "/login",
                "username=admin%40example.com&password=incorreta&_csrf=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
        assertEquals(302, falha.statusCode());
        assertTrue(falha.headers().firstValue("location").orElseThrow().contains("/login?erro"));
        assertTrue(get(visitante, "/login?erro").body().contains("E-mail ou senha inválidos."));
        assertEquals(302, get(visitante, "/pedidos").statusCode());

        assertEquals(403, enviarPost(http, "/pedidos/" + pedido.getId() + "/adicionar-item",
                "produtoId=" + produto.getId() + "&quantidade=1").statusCode());
        assertEquals(403, enviarPost(http, "/logout", "").statusCode());
        assertEquals(200, get("/pedidos").statusCode());
        assertEquals(10, estoque());
        assertEquals(new BigDecimal("0.00"), buscarPedido().getValorTotal());
    }

    @Test
    void adminCadastraProdutoComCentavosERecebeErrosDeValidacao() throws Exception {
        assertTrue(get("/pedidos").body().contains("href=\"/produtos\""));
        assertTrue(get("/pedidos").body().contains("href=\"/clientes\""));
        assertEquals(200, get("/produtos").statusCode());
        assertTrue(get("/produtos/novo").body().contains("step=\"0.01\""));
        assertEquals(302, post("/produtos/novo",
                "nome=Produto+Novo&descricao=Descricao&preco=19.90&quantidadeEmEstoque=5").statusCode());
        Produto criado = produtoRepository.findAll().stream()
                .filter(p -> p.getNome().equals("Produto Novo")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("19.90"), criado.getPreco());
        HttpResponse<String> invalido = post("/produtos/novo",
                "nome=&descricao=&preco=-1&quantidadeEmEstoque=-1");
        assertEquals(200, invalido.statusCode());
        assertTrue(invalido.body().contains("Nome é obrigatório"));
        assertEquals(2, produtoRepository.count());
    }

    @Test
    void edicaoEExclusaoDeProdutoUsamIdDaUrlEPreservamValidacao() throws Exception {
        Produto outro = new Produto();
        outro.setNome("Outro produto");
        outro.setDescricao("Descricao");
        outro.setPreco(new BigDecimal("10.00"));
        outro.setQuantidadeEmEstoque(4);
        outro = produtoRepository.save(outro);
        String edicao = "/produtos/edit/" + produto.getId();
        assertEquals(200, get(edicao).statusCode());
        assertEquals(302, post(edicao, "id=" + outro.getId()
                + "&nome=Produto+Editado&descricao=Nova&preco=15.75&quantidadeEmEstoque=8").statusCode());
        assertEquals("Produto Editado", produtoRepository.findById(produto.getId()).orElseThrow().getNome());
        assertEquals("Outro produto", produtoRepository.findById(outro.getId()).orElseThrow().getNome());

        HttpResponse<String> invalido = post(edicao,
                "nome=&descricao=Nova&preco=0&quantidadeEmEstoque=8");
        assertEquals(200, invalido.statusCode());
        assertTrue(invalido.body().contains("action=\"" + edicao + "\""));
        assertEquals(new BigDecimal("15.75"), produtoRepository.findById(produto.getId()).orElseThrow().getPreco());
        assertEquals(200, get("/produtos/delete/" + produto.getId()).statusCode());
        assertEquals(302, post("/produtos/delete/" + produto.getId(), "id=" + outro.getId()).statusCode());
        assertFalse(produtoRepository.existsById(produto.getId()));
        assertTrue(produtoRepository.existsById(outro.getId()));
        assertEquals(404, get(edicao).statusCode());
        assertEquals(404, post(edicao,
                "nome=Inexistente&descricao=Nova&preco=1&quantidadeEmEstoque=1").statusCode());
        assertEquals(404, post("/produtos/delete/" + produto.getId(), "").statusCode());
    }

    @Test
    void adminGerenciaClientesComValidacaoDeDuplicidade() throws Exception {
        assertEquals(200, get("/clientes").statusCode());
        assertEquals(200, get("/clientes/novo").statusCode());
        String dados = "nome=Novo+Cliente&email=novo%40example.com&cpf=11144477735&telefone=16999999999"
                + "&logradouro=Rua+Nova&bairro=Centro&cidade=Sao+Carlos&uf=SP&cep=13560-000";
        assertEquals(302, post("/clientes", dados).statusCode());
        Cliente criado = clienteRepository.findByEmail("novo@example.com").orElseThrow();
        assertEquals(200, get("/clientes/" + criado.getId() + "/editar").statusCode());
        assertEquals(302, post("/clientes", dados + "&id=" + criado.getId()).statusCode());
        HttpResponse<String> duplicado = post("/clientes", dados);
        assertEquals(200, duplicado.statusCode());
        assertTrue(duplicado.body().contains("Já existe um cliente cadastrado com este e-mail."));
        HttpResponse<String> invalido = post("/clientes", "nome=&email=errado&cpf=123");
        assertEquals(200, invalido.statusCode());
        assertTrue(invalido.body().contains("CPF inválido"));
        assertEquals(2, clienteRepository.count());
        assertEquals(200, get("/clientes/" + criado.getId() + "/excluir").statusCode());
        assertEquals(302, post("/clientes/" + criado.getId() + "/excluir", "").statusCode());
        assertFalse(clienteRepository.existsById(criado.getId()));
    }

    @Test
    void exclusaoDeCadastrosVinculadosPreservaHistoricoEMostraMensagem() throws Exception {
        pedidoService.adicionarItem(pedido.getId(), produto.getId(), 1);
        assertEquals(302, post("/produtos/delete/" + produto.getId(), "").statusCode());
        assertTrue(get("/produtos").body().contains("Não é possível excluir este produto"));
        assertTrue(produtoRepository.existsById(produto.getId()));
        assertEquals(302, post("/clientes/" + cliente.getId() + "/excluir", "").statusCode());
        assertTrue(get("/clientes").body().contains("Não é possível excluir este cliente"));
        assertTrue(clienteRepository.existsById(cliente.getId()));
        assertEquals(new BigDecimal("12.50"), buscarPedido().getValorTotal());
    }

    @Test
    void clienteInexistenteRetorna404NasOperacoes() throws Exception {
        assertEquals(404, get("/clientes/" + Long.MAX_VALUE + "/editar").statusCode());
        assertEquals(404, get("/clientes/" + Long.MAX_VALUE + "/excluir").statusCode());
        assertEquals(404, post("/clientes/" + Long.MAX_VALUE + "/excluir", "").statusCode());
        assertEquals(404, post("/clientes", "id=" + Long.MAX_VALUE).statusCode());
    }

    @Test
    void roteiroDaAulaComCincoProdutosTresItensEdicaoECancelamento() {
        Produto[] produtos = new Produto[5];
        produtos[0] = produto;
        for (int i = 1; i < produtos.length; i++) {
            Produto novo = new Produto();
            novo.setNome("Produto " + (i + 1));
            novo.setDescricao("Produto do roteiro da aula");
            novo.setPreco(new BigDecimal("10.00").multiply(BigDecimal.valueOf(i)));
            novo.setQuantidadeEmEstoque(20);
            produtos[i] = produtoRepository.save(novo);
        }

        assertEquals(5, produtoRepository.count());
        Long versaoInicial = buscarPedido().getVersion();
        pedidoService.adicionarItem(pedido.getId(), produtos[0].getId(), 2);
        pedidoService.adicionarItem(pedido.getId(), produtos[1].getId(), 3);
        pedidoService.adicionarItem(pedido.getId(), produtos[2].getId(), 4);

        assertEquals(new BigDecimal("135.00"), buscarPedido().getValorTotal());
        assertEquals(8, estoque());
        assertEquals(17, produtoRepository.findById(produtos[1].getId()).orElseThrow().getQuantidadeEmEstoque());
        assertEquals(16, produtoRepository.findById(produtos[2].getId()).orElseThrow().getQuantidadeEmEstoque());
        assertTrue(buscarPedido().getVersion() > versaoInicial);
        assertEquals(1, pedidoService.listarPorStatus(StatusPedido.ATIVO).size());

        pedidoService.removerItem(pedido.getId(), primeiroItemId());
        pedidoService.adicionarItem(pedido.getId(), produtos[3].getId(), 1);
        assertEquals(10, estoque());
        assertEquals(new BigDecimal("140.00"), buscarPedido().getValorTotal());
        Long versaoEditada = buscarPedido().getVersion();

        pedidoService.cancelarPedido(pedido.getId());
        assertEquals(StatusPedido.CANCELADO, buscarPedido().getStatus());
        assertTrue(buscarPedido().getVersion() > versaoEditada);
        assertEquals(0, pedidoService.listarPorStatus(StatusPedido.ATIVO).size());
        assertEquals(1, pedidoService.listarPorStatus(StatusPedido.CANCELADO).size());
        for (int i = 0; i < produtos.length; i++) {
            assertEquals(i == 0 ? 10 : 20,
                    produtoRepository.findById(produtos[i].getId()).orElseThrow().getQuantidadeEmEstoque());
        }
        assertEquals(new BigDecimal("140.00"), buscarPedido().getValorTotal());
    }

    @Test
    void versaoDoProdutoImpedeSobrescreverEstoqueDesatualizado() {
        Produto saldoAntigo = produtoRepository.findById(produto.getId()).orElseThrow();
        Long versaoInicial = saldoAntigo.getVersion();
        pedidoService.adicionarItem(pedido.getId(), produto.getId(), 2);
        Produto saldoAtual = produtoRepository.findById(produto.getId()).orElseThrow();
        assertTrue(saldoAtual.getVersion() > versaoInicial);

        saldoAntigo.setQuantidadeEmEstoque(9);
        assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class,
                () -> produtoRepository.saveAndFlush(saldoAntigo));
        assertEquals(8, estoque());
        assertEquals(new BigDecimal("25.00"), buscarPedido().getValorTotal());
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
        return get(http, path);
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        String csrf = "_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8);
        String bodyComCsrf = body.isBlank() ? csrf : body + "&" + csrf;
        return enviarPost(http, path, bodyComCsrf);
    }

    private HttpResponse<String> enviarPost(HttpClient client, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private String extrairCsrf(String html) {
        Matcher matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Token CSRF não encontrado");
        return matcher.group(1);
    }

    private LoginSession login(String email, String senha) throws Exception {
        HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        HttpResponse<String> paginaLogin = get(client, "/login");
        Matcher matcher = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
                .matcher(paginaLogin.body());
        assertTrue(matcher.find(), "Token CSRF não encontrado no formulário de login");
        String token = matcher.group(1);
        String corpo = "username=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(senha, StandardCharsets.UTF_8)
                + "&_csrf=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        HttpResponse<String> resposta = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(corpo))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(302, resposta.statusCode());
        assertTrue(resposta.headers().firstValue("location").orElse("").contains("/pedidos"));
        HttpResponse<String> paginaLogout = get(client, "/logout");
        Matcher tokenAutenticado = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"")
                .matcher(paginaLogout.body());
        assertTrue(tokenAutenticado.find(), "Token CSRF autenticado não encontrado");
        return new LoginSession(client, tokenAutenticado.group(1));
    }

    private record LoginSession(HttpClient http, String csrfToken) {
    }
}
