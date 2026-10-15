# Conferência do Projeto 1

Materiais de referência:

- **Aula:** `Sistema de Vendas (1).pdf`, do professor André Roberto da Silva.
- **Atividade:** `ORIENTAÇÕES PROJETO 1(1).pdf`.
- **Deploy:** `DEPLOY-RENDER.pdf`.
- **Segurança:** `Java e Spring Security.pdf`, do professor André Roberto da Silva.
- Print do quadro Trello `sistema_vendas`, com 19 cartões.

“Atende” indica correspondência no código e nas verificações locais. As etapas
que dependem do painel e do banco do Render precisam ser conferidas após o deploy.

| Requisito | Referência | Implementação ou evidência | Situação |
| --- | --- | --- | --- |
| Spring MVC, Thymeleaf, JPA, PostgreSQL e Validation | Aula, p. 3 | Dependências no `pom.xml` | Atende |
| PostgreSQL local por Docker Compose | Aula, pp. 3–4; Deploy, p. 1 | `docker-compose.yml` validado por `docker compose config`; PostgreSQL 15 testado em Docker | Atende na configuração local |
| CRUD Produto | Aula, p. 6 | `Produto`, repository, service, controller e três templates | Atende |
| CRUD Cliente | Aula, p. 8 | `Cliente`, repository, service, controller e três templates | Atende |
| Modelo físico para dbdiagram.io | Aula, pp. 10–11 | `docs/modelo-banco.dbml`, com as tabelas e chaves da implementação | Disponível |
| Cliente 1:N Pedidos, Pedido 1:N Itens e Produto 1:N Itens | Aula, pp. 11–16 e 22–23 | Chaves estrangeiras e relacionamentos nas entidades | Atende |
| Preço do produto e valores monetários | Aula, p. 11 | `BigDecimal`; preço e totais com precisão 10 e escala 2 | Atende |
| Campo version em Produto | Aula, p. 11 | `@Version`; valor padrão no banco para os registros existentes quando a coluna é criada | Atende |
| Campo version em Pedido | Aula, pp. 17–21 | `@Version`; incremento nas alterações do pedido | Atende |
| Status ATIVO e CANCELADO | Aula, pp. 19–21 | `StatusPedido`, persistido como texto | Atende |
| cascade, orphanRemoval e comentário explicativo | Aula, p. 19 | Lista de itens de `Pedido`; comentário mantido junto ao mapeamento | Atende |
| Construtores de Pedido e ItemPedido | Aula, pp. 19–23 | Pedido recebe cliente; item recebe produto e quantidade e copia o preço | Atende |
| Consulta de pedidos por status | Aula, pp. 24 e 38–39 | `findByStatus` e `listarPorStatus` | Atende |
| Criar pedido | Aula, pp. 25–26 e 43–45 | Cliente obrigatório e redirecionamento para a edição | Atende |
| Adicionar e remover itens | Aula, pp. 27–35 e 48–51 | Validação de IDs, status, quantidade, estoque e recálculo do total | Atende |
| Cancelar pedido | Aula, pp. 36–37 e 54–55 | Devolução de estoque e status CANCELADO; cancelamento repetido bloqueado | Atende |
| Finalizar pedido apenas retorna à lista | Aula, pp. 52–53 | POST `/pedidos/{id}/finalizar` redireciona para `/pedidos` | Atende |
| Listagem, formulário, edição, cancelamento e detalhes | Aula, pp. 40–56 | Cinco templates em `templates/pedido` | Atende |
| Teste com cinco produtos, três itens e quantidades diferentes | Aula, p. 57 | Teste `roteiroDaAulaComCincoProdutosTresItensEdicaoECancelamento` | Coberto pelos testes |
| Spring Security e classe de configuração | Atividade, p. 1, item 1 | Dependência, pacote `security`, `@Configuration`, `@EnableWebSecurity` e filtro | Atende |
| Login personalizado | Atividade, item 2; Segurança, pp. 18–23 | `login.html`, campos `username` e `password`, GET `exibirLogin` e POST processado pelo Spring | Atende |
| Logout personalizado | Atividade, item 3; Segurança, pp. 27–29 | Página de confirmação, link no menu, POST com CSRF e `/login?logout` | Atende |
| Proteção CSRF | Segurança, pp. 41–43 | Tokens incluídos pelo Thymeleaf; POST sem token é bloqueado nos testes | Atende |
| Usuários persistidos e autenticação por e-mail | Atividade, item 4; Segurança, pp. 44–48 | `Usuario` implementa `UserDetails`; busca ignora caixa; serviço implementa `UserDetailsService` | Atende |
| Hash BCrypt e bean do encoder | Atividade, item 4; Segurança, p. 50 | `BCryptPasswordEncoder`; gravação pelo `UsuarioService` | Atende |
| Perfis ADMIN e PADRAO | Atividade, item 5; Segurança, pp. 51–53 | Enum e authorities com prefixo ROLE_; ADMIN gerencia tudo e PADRAO opera pedidos | Atende |
| Restrição visual e por URL | Atividade, item 5; Segurança, pp. 53–56 | Dependência Thymeleaf + Security, namespace `sec`, `sec:authorize` e `requestMatchers` antes da regra geral | Atende |
| Variáveis do banco e porta do serviço | Deploy, p. 1; Atividade, item 6 | `application.properties` lê `DB_URL`, `DB_USER`, `DB_PASSWORD` e `PORT` | Atende no código |
| Dockerfile e .dockerignore | Deploy, pp. 1–2 | Build da imagem e execução com Java 21 e PostgreSQL 15 concluídos | Atende localmente |
| Limite de memória da JVM no Render | Deploy, p. 2 | Definir `JAVA_TOOL_OPTIONS=-Xmx400m` em Environment | Conferir no Render |
| Banco e Web Service na mesma região | Deploy, pp. 1–2; Atividade, item 6 | Print do painel mostra ambos em Oregon | Confirmado no print |
| Web Service Docker na branch main | Deploy, p. 2 | Print confirma Docker; log informa checkout de `af5db64` na branch `main` | Confirmado no print e nos logs |
| Caminho do Dockerfile no painel | Deploy, pp. 1–2 | Log procura `dockerfile`; no GitHub o nome é `Dockerfile` | Corrigir para `./Dockerfile` |
| Dois usuários de teste já cadastrados | Atividade, p. 2, entrega 3 | `UsuariosIniciais` cria as contas pelas variáveis; criação e hash testados localmente | Conferir no banco do Render |
| Aplicação publicada e funcional | Atividade, p. 2, entrega 1 e 4 | Serviço informado no README; último print mostra Failed deploy | Pendente |
| Link do GitHub e credenciais para entrega | Atividade, p. 2, entrega 2–3 | Link no README; credenciais entregues separadamente | Credenciais pendentes de conferência |

O print do Trello permite conferir os títulos e etapas dos cartões. As descrições
internas dos cartões não estão visíveis; o link de convite exige autenticação.
Detalhes que existam apenas nessas descrições ainda precisam ser comparados.

O arquivo DBML representa o esquema implementado e pode ser aberto no dbdiagram.io.
As entidades usam IDs `Long` (bigint). Os campos de texto de Cliente mantêm os
tamanhos padrão do mapeamento existente, em vez dos limites menores ilustrados
no diagrama da p. 11. A conferência cobre atributos e relacionamentos, não uma
reprodução literal de todos os tipos e tamanhos daquele exemplo.

Os usuários em memória com `{noop}` são uma etapa inicial da aula de segurança
(pp. 12–17), substituída pela persistência com BCrypt nas pp. 44–53. A aplicação
utiliza a etapa final. Remember Me e suas variações aparecem como exercício
nas pp. 38–40; não estão implementados e não são exigidos no enunciado do Projeto 1.

## Etapas do Trello

As sugestões abaixo consideram a implementação local e os requisitos dos PDFs.
Não significam que o serviço já foi atualizado no Render.

| Cartão | Etapa no print | Etapa sugerida | Evidência |
| --- | --- | --- | --- |
| Controle de Permissões | TODO | Done | ADMIN/PADRAO, menus e bloqueios por URL |
| Autenticação de Usuário | TODO | Done | Login e logout com banco e BCrypt |
| Registro de Pedidos | TODO | Done | Criação e inclusão de itens |
| Listagem de Pedidos | TODO | Done | Lista e detalhes renderizados |
| Atualização de Pedidos | TODO | Done | Adição/remoção, total e estoque |
| Cancelamento de Pedidos | TODO | Done | Status e devolução de estoque |
| Cadastro de Usuários | TODO | Done para o enunciado | Dois perfis persistidos; a aula usa inserção no banco, sem exigir tela de cadastro |
| Frontend com Bootstrap | TODO | Done | Templates com Bootstrap local |
| Banco de Dados PostgreSQL em Docker | TODO | Done | PostgreSQL local e configuração Compose |
| Estrutura Spring Boot MVC | Doing | Done | Model, repository, service, controller e templates |
| Aplicação Spring Boot em Docker | Doing | Done | Dockerfile com build e execução Java 21 |
| Cadastro de Produtos | Done | Done | Formulário e persistência |
| Listagem de Produtos | Done | Done | Lista renderizada |
| Alteração de Produtos | Done | Done | Edição pelo ID da URL |
| Exclusão de Produtos | Done | Done | Confirmação e proteção de vínculos com pedidos |
| Cadastro de Clientes | Done | Done | Formulário e validação |
| Listagem de Clientes | Done | Done | Lista renderizada |
| Alteração de Clientes | Done | Done | Edição e validação de duplicidade |
| Exclusão de Clientes | Done | Done | Confirmação e proteção de vínculos com pedidos |

Adicionar um cartão **Deploy no Render** em Doing até corrigir a configuração,
publicar a versão final e testar as duas contas online. Conferir a descrição do
cartão Cadastro de Usuários antes de encerrá-lo, caso inclua requisitos além dos PDFs.

## Verificações locais

- `mvnw.cmd clean verify`: 20 testes aprovados, sem falhas ou erros.
- Build do Dockerfile concluído, usando Maven/Temurin 21 e JRE 21.
- Aplicação executada em contêiner com PostgreSQL 15 e `JAVA_TOOL_OPTIONS=-Xmx400m`.
- Banco de conferência isolado: criação de ADMIN/PADRAO com BCrypt, login, logout,
  cadastro de cliente, edição de produto e operações de pedidos verificadas por HTTP.
- PADRAO bloqueado nas URLs administrativas; menus restritos ocultos e POST sem
  token CSRF recusado.
- Pedido de teste cancelado com total preservado e estoque devolvido. Campos de
  versão de Pedido e Produto incrementados.
- Atualização de uma tabela de produtos que já continha registro e ainda não
  possuía a coluna `version`: registro preservado e versão inicial zero.

Essas verificações não substituem os testes das duas contas na publicação do Render.

## Publicação no Render

O projeto já possui banco e Web Service. Utilize esses serviços existentes.

1. No GitHub, colocar o código final na branch `main`, usada no roteiro da aula.
2. Em Settings do Web Service, conferir repositório, branch `main`, runtime Docker,
   região Oregon e Dockerfile `./Dockerfile`. Deixar Docker Command vazio.
3. Em Environment, conferir as variáveis:

| Nome | Valor |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://HOST_INTERNO:5432/NOME_DO_BANCO` |
| `DB_USER` | Username de Connections do PostgreSQL |
| `DB_PASSWORD` | Password de Connections do PostgreSQL |
| `JAVA_TOOL_OPTIONS` | `-Xmx400m` |
| `ADMIN_EMAIL` | E-mail da conta ADMIN |
| `ADMIN_PASSWORD` | Senha da conta ADMIN |
| `PADRAO_EMAIL` | Outro e-mail para a conta PADRAO |
| `PADRAO_PASSWORD` | Senha da conta PADRAO |

4. Se usar Health Check Path, definir `/login`.
5. Abrir o deploy com falha em Events/Deploys e conferir o erro nos logs antes de
   tentar novamente. Com a configuração corrigida, publicar o commit atualizado.
6. Conferir `Started SistemaVendasApplication` nos logs e o status Live.
7. Em uma janela anônima, acessar `/login` e verificar que `/pedidos`, `/produtos`
   e `/clientes` exigem autenticação. Testar cada um dos dois perfis.

As variáveis de usuário criam as contas apenas quando elas ainda não existem.
Alterar a variável de senha não troca a senha de uma conta já cadastrada.

### Erro informado nos logs

O deploy da branch `main`, commit `af5db64`, terminou com:

```text
failed to read dockerfile: open dockerfile: no such file or directory
```

O arquivo existente no repositório é `Dockerfile`, com D maiúsculo. Em Settings,
usar Dockerfile Path `./Dockerfile` e deixar Root Directory vazio, pois o projeto
está na raiz do repositório. O caminho é relativo ao Root Directory configurado.
Não é necessário criar outro banco ou Web Service para corrigir esse erro.

## Entrega ao professor

- Aplicação: https://sistema-vendas-8b0n.onrender.com/
- Repositório: https://github.com/FelipeAugust0/sistema-vendas
- E-mail, senha e perfil das duas contas cadastradas no banco publicado.
- Aplicação e banco disponíveis até o encerramento da avaliação.

Conferir no painel a data de expiração do PostgreSQL, se estiver no plano Free,
para que o banco continue disponível durante toda a avaliação.
O plano Free atualmente expira o banco após 30 dias da criação, conforme a
[documentação do Render](https://render.com/docs/free#30-day-limit).
