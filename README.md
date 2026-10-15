# Sistema de Vendas

Projeto desenvolvido em Java com Spring Boot para gerenciamento de vendas.

Aplicação: https://sistema-vendas-8b0n.onrender.com/

Repositório: https://github.com/FelipeAugust0/sistema-vendas

## Funcionalidades

- Cadastro de produtos
- Cadastro de clientes
- Autenticação de usuários com senhas BCrypt
- Perfis ADMIN (acesso total) e PADRAO (operações de pedidos)
- Registro de pedidos
- Edição e exclusão de registros
- Validação de dados

## Tecnologias

- Java
- Spring Boot
- Spring MVC
- Spring Data JPA
- Thymeleaf
- PostgreSQL
- Bootstrap
- Docker

## Estrutura

O projeto utiliza arquitetura MVC, separando:

- Model
- Repository
- Service
- Controller
- Templates

## Objetivo

Projeto desenvolvido durante as aulas para praticar criação de aplicações web com Spring Boot, banco de dados e Thymeleaf.

## Executar e testar

Use Java 21 ou superior e mantenha o Docker Desktop em execução.
O `docker-compose.yml` inicia o PostgreSQL local:

```bash
docker compose up -d
```

Antes da primeira execução, configure os dois usuários de teste. No PowerShell,
substitua as senhas de exemplo pelas que você vai usar localmente:

```powershell
$env:ADMIN_EMAIL = 'admin@example.com'
$env:ADMIN_PASSWORD = 'substitua-por-uma-senha-admin'
$env:PADRAO_EMAIL = 'padrao@example.com'
$env:PADRAO_PASSWORD = 'substitua-por-uma-senha-padrao'
.\mvnw.cmd spring-boot:run
```

No Linux ou Git Bash, configure as mesmas variáveis com `export` e execute
`bash mvnw spring-boot:run`. Acesse `http://localhost:8080/login` e entre com
um dos e-mails e senhas configurados. Sem essas variáveis, a aplicação não cria
usuários automaticamente e não existe uma senha padrão.

O banco local usa `usuario`, `senha123` e `sistema_vendas`, conforme o Compose.
As variáveis `DB_URL`, `DB_USER` e `DB_PASSWORD` sobrescrevem essa configuração.
Os dados permanecem no volume Docker entre as execuções.

Para testar e gerar o JAR no Windows:

```powershell
.\mvnw.cmd clean verify
```

No Linux ou Git Bash:

```bash
bash mvnw clean verify
docker build -t sistema-vendas .
```

Os testes usam H2 em memória, sem acessar o PostgreSQL local ou de produção.
Cobrem adição e remoção de itens, total, estoque, versão, cancelamento repetido
e renderização dos formulários Thymeleaf, além de login, logout, CSRF,
restrições de URL e de menu, cadastro/edição/exclusão de produtos e clientes,
mensagens de validação e criação dos dois usuários iniciais.

## Deploy no Render

Crie um Web Service conectado a este repositório, com runtime Docker,
diretório raiz do repositório e Dockerfile Path `./Dockerfile`.
Deixe Docker Command vazio para usar o `ENTRYPOINT` da imagem.
Configure o Health Check Path como `/login`, que pode ser acessado sem autenticação.
Esse caminho verifica a resposta HTTP da aplicação; não consulta o banco.

Crie o banco Render Postgres e o Web Service na mesma região e configure no Web Service:

| Variável | Valor |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://HOST_INTERNO:5432/NOME_DO_BANCO` |
| `DB_USER` | Usuário do banco Render Postgres |
| `DB_PASSWORD` | Senha do banco Render Postgres |
| `JAVA_TOOL_OPTIONS` | `-Xmx400m`, conforme o roteiro de deploy da aula |
| `ADMIN_EMAIL` | E-mail do usuário de teste ADMIN |
| `ADMIN_PASSWORD` | Senha forte do usuário de teste ADMIN |
| `PADRAO_EMAIL` | E-mail do usuário de teste PADRAO |
| `PADRAO_PASSWORD` | Senha forte do usuário de teste PADRAO |

Use o host, usuário e nome do banco informados no painel do Render.
`DB_URL` precisa estar no formato JDBC acima; não cole diretamente uma URL
`postgresql://usuario:senha@host/banco` nessa variável.
O Render fornece `PORT` e a aplicação já a utiliza em `server.port`.
O `docker-compose.yml` inicia somente o banco local; ele não é utilizado nesse deploy.
Mantenha as credenciais nas variáveis do serviço, fora do repositório.
Na primeira inicialização, a aplicação cria os dois usuários configurados acima
com senha BCrypt. Inicializações posteriores preservam os usuários existentes.
Não publique essas senhas no repositório ou no pull request; entregue-as apenas
pelo canal definido para a avaliação.

### Conferência para entrega

- Publique no Render o commit que contém as correções finais.
- Confirme as oito variáveis da tabela acima e a região do banco e do serviço.
- Entre com ADMIN, cadastre um cliente e um produto e crie um pedido com itens.
- Saia e entre com PADRAO. Confirme que consegue operar pedidos e que acessar
  `/produtos` e `/clientes` diretamente retorna acesso negado.
- Entregue os links da aplicação e do GitHub e as credenciais dos dois perfis
  pelo canal da avaliação.
- Mantenha o serviço e o PostgreSQL disponíveis até o encerramento da avaliação.

A relação entre os requisitos da aula, os arquivos do projeto e as etapas de
entrega está em [docs/conferencia-projeto.md](docs/conferencia-projeto.md).
