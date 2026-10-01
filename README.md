# Sistema de Vendas

Projeto desenvolvido em Java com Spring Boot para gerenciamento de vendas.

## Funcionalidades

- Cadastro de produtos
- Cadastro de clientes
- Cadastro de usuários
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

Use Java 21. Para iniciar o PostgreSQL local e executar a aplicação:

```bash
docker compose up -d
bash mvnw spring-boot:run
```

No Windows, use `mvnw.cmd`. Acesse `http://localhost:8080/pedidos`.

```bash
bash mvnw test
docker build -t sistema-vendas .
```

Os testes usam H2 em memória, sem acessar o PostgreSQL local ou de produção.
Cobrem adição e remoção de itens, total, estoque, versão, cancelamento repetido
e renderização dos formulários Thymeleaf.

## Deploy no Render

Crie um Web Service conectado a este repositório, com runtime Docker,
diretório raiz do repositório e Dockerfile Path `./Dockerfile`.
Deixe Docker Command vazio para usar o `ENTRYPOINT` da imagem.
Configure o Health Check Path como `/pedidos`.

Crie um banco Render Postgres na mesma região e configure no Web Service:

| Variável | Valor |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://HOST_INTERNO:5432/NOME_DO_BANCO` |
| `DB_USER` | Usuário do banco Render Postgres |
| `DB_PASSWORD` | Senha do banco Render Postgres |

Use o host, usuário e nome do banco informados no painel do Render.
`DB_URL` precisa estar no formato JDBC acima; não cole diretamente uma URL
`postgresql://usuario:senha@host/banco` nessa variável.
O Render fornece `PORT` e a aplicação já a utiliza em `server.port`.
O `docker-compose.yml` inicia somente o banco local; ele não é utilizado nesse deploy.
Mantenha as credenciais nas variáveis do serviço, fora do repositório.
