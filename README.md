# volta-api-redis

API secundária do VOLTA para o **ranking de empresas**, feita em Java 21 + Spring Boot 4 e usando **Redis** (Sorted Set) como armazenamento.

O ranking é calculado a partir dos indicadores VOLTA (pontuação de 0 a 1000). A fonte da verdade continua sendo o PostgreSQL da `volta-api`; o Redis serve como cache rápido de leitura ordenada. A chave do Redis é `ranking:companies`, e o membro é o UUID da empresa (o mesmo UUID do PostgreSQL).

## Endpoints

Base: `/api/v1`

| Método | Rota | Papel mínimo | Redis |
|---|---|---|---|
| POST | `/auth/login` | público | — |
| GET | `/ranking` | FUNCIONARIO | `ZREVRANGE 0 -1 WITHSCORES` |
| GET | `/ranking/top?limit=10` | FUNCIONARIO | `ZREVRANGE 0 N-1 WITHSCORES` |
| GET | `/ranking/{companyUuid}/posicao` | FUNCIONARIO | `ZREVRANK` + `ZSCORE` + `ZCARD` |
| GET | `/ranking/{companyUuid}/pontuacao` | FUNCIONARIO | `ZSCORE` |
| GET | `/ranking/total` | FUNCIONARIO | `ZCARD` |
| POST | `/ranking/score` | GESTOR | `ZADD` |
| DELETE | `/ranking/{companyUuid}` | ADMIN | `ZREM` |

Papéis: `FUNCIONARIO` (leitura) < `GESTOR` (leitura + atualizar score) < `ADMIN` (tudo). A autenticação é JWT (`Authorization: Bearer <token>`).

## Estrutura

```
src/main/java/com/volta/ranking/
├── controller/   endpoints HTTP
├── service/      regras de negócio
├── repository/   acesso ao Redis (RedisTemplate / ZSET)
├── dto/          objetos de entrada e saída
├── config/       Redis, segurança e senha
├── security/     JWT (geração, validação e filtro) e usuários de demonstração
└── exception/    exceções de domínio
```

## Rodando localmente

Requisitos: JDK 21, Maven 3.9+ e Docker.

```bash
docker compose up -d        # sobe o Redis (porta 6379)
```

Defina a chave do JWT (mínimo de 256 bits; deve ser a mesma da `volta-api`) e rode:

```bash
# Linux/macOS
export JWT_KEY="<chave-com-256-bits-ou-mais>"
# PowerShell
$env:JWT_KEY = "<chave-com-256-bits-ou-mais>"

mvn spring-boot:run
```

A API sobe em `http://localhost:8081`. Não há valor padrão para `JWT_KEY`: o repositório é público e nenhum segredo deve ser versionado.

### Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `JWT_KEY` | — (obrigatória) | Chave de assinatura do JWT |
| `JWT_EXPIRATION` | `86400000` | Validade do token em ms |
| `REDIS_HOST` | `localhost` | Host do Redis |
| `REDIS_PORT` | `6379` | Porta do Redis |
| `SERVER_PORT` | `8081` | Porta HTTP |

## Usuários de demonstração

Existem três usuários em memória (`funcionario`, `gestor` e `admin`) para testes locais. Devem ser substituídos pela autenticação real (PostgreSQL da `volta-api`) antes de qualquer uso em produção.
