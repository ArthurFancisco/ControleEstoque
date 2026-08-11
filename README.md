# FlowStock

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=0B1220)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)

SaaS multiempresa para controle de estoque, produção, vendas e gestão de pequenos negócios. O projeto reúne uma API segura em Spring Boot e uma interface administrativa em React.

> **Status:** MVP funcional em evolução. Projeto de estudo e portfólio, ainda não preparado para uso em produção.

## Destaques

- separação de dados por empresa;
- autenticação com JWT e Spring Security;
- área de Super Admin para empresas, planos e auditoria;
- cadastro de produtos e clientes;
- movimentações de estoque e alerta de estoque baixo;
- controle de produção e vendas;
- relatórios operacionais;
- migrations versionadas com Flyway;
- execução local com Docker Compose.

## Arquitetura

| Camada | Tecnologias |
|---|---|
| Backend | Java 21, Spring Boot, Spring Security, JPA/Hibernate, JWT e Flyway |
| Frontend | React, TypeScript, Vite, Tailwind CSS, Axios e React Router |
| Banco | PostgreSQL |
| Infraestrutura | Docker e Docker Compose |

## Como executar

~~~bash
git clone https://github.com/ArthurFancisco/ControleEstoque.git
cd ControleEstoque
docker compose --profile app up --build
~~~

Acesse:

- frontend: http://localhost:5173
- API: http://localhost:8080

### Desenvolvimento separado

Banco:

~~~bash
docker compose up -d postgres
~~~

Backend:

~~~bash
cd backend
mvn spring-boot:run
~~~

Frontend:

~~~bash
cd frontend
npm install
npm run dev
~~~

## Configuração

As informações sensíveis devem ser fornecidas por variáveis de ambiente:

- DB_URL
- DB_USERNAME
- DB_PASSWORD
- JWT_SECRET
- JWT_EXPIRATION_MINUTES
- CORS_ALLOWED_ORIGINS
- INITIAL_ADMIN_PASSWORD
- VITE_API_URL

Use credenciais exclusivas em cada ambiente e nunca publique segredos reais.

## Estrutura

~~~text
ControleEstoque/
├── backend/
├── frontend/
├── docker-compose.yml
└── README.md
~~~

## Próximas evoluções

- ampliar a cobertura de testes automatizados;
- adicionar permissões granulares por funcionário;
- implementar assinatura e pagamento;
- incluir importação e exportação de dados;
- preparar observabilidade e deploy de produção.

## Autor

Desenvolvido por [Arthur Amancio Francisco](https://www.linkedin.com/in/arthur-amancio-francisco/) como projeto de estudo e portfólio.
