# FlowStock

SaaS multiempresa para controle de estoque, producao, vendas e gestao de pequenos negocios, com area Super Admin para administrar empresas, planos, logs e status da plataforma.

## Stack

- Backend: Java 21, Spring Boot 3, Spring Security, JWT, JPA/Hibernate, PostgreSQL, Flyway, Maven, Bean Validation.
- Frontend: React, TypeScript, Vite, Tailwind CSS, Axios, React Router e componentes locais no estilo Shadcn.
- Banco local: PostgreSQL em `localhost:5432`, database `flowstock`, usuario `postgres`, senha `postdba`.

## Como criar o banco no pgAdmin

1. Abra o pgAdmin.
2. Crie ou selecione um servidor PostgreSQL local.
3. Crie um banco chamado `flowstock`.
4. Garanta que o usuario `postgres` tenha acesso e senha `postdba` no ambiente local.
5. Ao iniciar o backend, o Flyway cria as tabelas, planos e o usuario inicial.

Tambem e possivel subir apenas o banco com Docker:

```bash
docker compose up -d postgres
```

## Backend

```bash
cd flowstock/backend
mvn spring-boot:run
```

Configuracao padrao em `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/flowstock}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postdba}
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
    show-sql: false
  flyway:
    enabled: true
```

Variaveis uteis:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MINUTES`
- `CORS_ALLOWED_ORIGINS`
- `INITIAL_ADMIN_PASSWORD`

## Frontend

```bash
cd flowstock/frontend
npm install
npm run dev
```

Acesse `http://localhost:5173`.

Rotas principais do frontend:

- Super Admin: `http://localhost:5173/admin/dashboard`
- Area da empresa: `http://localhost:5173/app/dashboard`

## Docker Compose

Banco somente:

```bash
docker compose up -d postgres
```

Banco, backend e frontend:

```bash
docker compose --profile app up --build
```

## Super Admin inicial

- Email: `admin@flowstock.local`
- Senha: `Admin@123456`

A migration cria o usuario Super Admin com senha BCrypt. Na primeira subida do backend, um seeder valida e atualiza esse hash caso `INITIAL_ADMIN_PASSWORD` seja alterada.

## Endpoints principais

Autenticacao:

```bash
POST /auth/login
GET /auth/me
```

Super Admin:

```bash
GET /admin/dashboard
GET /admin/companies
POST /admin/companies
PATCH /admin/companies/{id}/status?status=ACTIVE
PATCH /admin/companies/{id}/plan?planId=2
PATCH /admin/companies/{id}/extend-trial?days=7
GET /admin/companies/{id}/users
POST /admin/companies/{id}/users
PATCH /admin/companies/{id}/users/{userId}/active?active=false
GET /admin/logs
GET /admin/health
```

O `POST /admin/companies` cria a empresa e o administrador inicial:

```json
{
  "name": "Pellati Drinks",
  "email": "contato@pellati.local",
  "phone": "17999999999",
  "planId": 3,
  "status": "ACTIVE",
  "adminName": "Admin Pellati",
  "adminEmail": "admin@pellati.local",
  "adminPassword": "Empresa@123456"
}
```

Cliente:

```bash
GET /api/app/dashboard
GET /api/app/products
POST /api/app/products
PUT /api/app/products/{id}
PATCH /api/app/products/{id}/toggle-active
GET /api/app/products/low-stock
GET /api/app/stock
GET /api/app/stock/movements
POST /api/app/stock/movements
GET /api/app/production
POST /api/app/production
PATCH /api/app/production/{id}/finish
PATCH /api/app/production/{id}/cancel
GET /api/app/sales
POST /api/app/sales
PATCH /api/app/sales/{id}/pay
PATCH /api/app/sales/{id}/cancel
GET /api/app/customers
POST /api/app/customers
PUT /api/app/customers/{id}
DELETE /api/app/customers/{id}
GET /api/app/reports/summary
```

## Fluxo de teste do MVP

1. Entre como Super Admin com `admin@flowstock.local` / `Admin@123456`.
2. Acesse `Empresas`.
3. Crie a empresa:
   - Nome: `Pellati Drinks`
   - Email: `contato@pellati.local`
   - Telefone: `17999999999`
   - Plano: `Premium`
   - Status: `ACTIVE`
   - Admin nome: `Admin Pellati`
   - Admin email: `admin@pellati.local`
   - Admin senha: `Empresa@123456`
4. Saia do Super Admin.
5. Entre com `admin@pellati.local` / `Empresa@123456`.
6. Acesse `/app/dashboard`.
7. Crie o produto:
   - Nome: `Drink Maracuja`
   - Categoria: `Drinks`
   - Unidade: `Garrafa 500ml`
   - Preco custo: `5.00`
   - Preco venda: `12.00`
   - Estoque minimo: `10`
   - Estoque atual: `0`
8. Em Estoque, registre entrada de `20`.
9. Crie producao de `10` unidades e finalize.
10. Crie venda paga no PIX com `5` unidades.
11. Confira estoque atual como `25`, dashboard, relatorios e logs de auditoria.

## Estrutura

```text
flowstock/
  backend/
    src/main/java/br/com/flowstock/
    src/main/resources/db/migration/
    pom.xml
    Dockerfile
  frontend/
    src/
    package.json
    Dockerfile
  docker-compose.yml
  README.md
```

## Observacoes de seguranca

- Usuarios comuns nunca enviam `company_id` para filtrar dados operacionais.
- A empresa vem do usuario autenticado.
- O Super Admin tem rotas separadas em `/admin`.
- Empresas `SUSPENDED` e `CANCELED` nao conseguem autenticar usuarios comuns.
- Acoes sensiveis geram registros em `audit_logs`.
- A senha local de banco fica centralizada por variavel de ambiente com fallback apenas para desenvolvimento.

## O que ainda falta para producao

- Testes automatizados de servico, controller e integracao com Testcontainers.
- Fluxo completo de assinatura/pagamento.
- Permissoes granulares por funcionario.
- Modo suporte com troca de contexto real, `SupportSession` ativa no header e trilha detalhada.
- Importacao/exportacao, backup real, WhatsApp e IA.
- Observabilidade com metricas, tracing e alertas.

## Validacao de build

Backend:

```bash
cd flowstock/backend
mvn clean package
```

Frontend:

```bash
cd flowstock/frontend
npm install
npm run build
```
