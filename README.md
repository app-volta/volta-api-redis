# volta-api-redis

API secundária do VOLTA para ranking de empresas e índice geoespacial de cooperativas, feita em Java 21 + Spring Boot 4 usando Redis.

O ranking usa um Sorted Set (`ranking:companies`). A proximidade usa Redis GEO (`geo:cooperatives`), com UUIDs de cooperativas como membros. O PostgreSQL da `volta-api` continua sendo a fonte da verdade; este serviço guarda índices derivados. A `volta-api` fornece o ponto de origem da empresa e deve buscar os detalhes oficiais das cooperativas pelos UUIDs retornados.

## Endpoints

Base: `/api/v1`

| Método | Rota | Papel mínimo | Redis |
|---|---|---|---|
| POST | `/auth/login` | perfil `local` | — |
| GET | `/ranking` | FUNCIONARIO | `ZREVRANGE 0 -1 WITHSCORES` |
| GET | `/ranking/top?limit=10` | FUNCIONARIO | `ZREVRANGE 0 N-1 WITHSCORES` |
| GET | `/ranking/{companyUuid}/posicao` | FUNCIONARIO | `ZREVRANK` + `ZSCORE` + `ZCARD` |
| GET | `/ranking/{companyUuid}/pontuacao` | FUNCIONARIO | `ZSCORE` |
| GET | `/ranking/total` | FUNCIONARIO | `ZCARD` |
| POST | `/ranking/score` | GESTOR | `ZADD` |
| DELETE | `/ranking/{companyUuid}` | ADMIN | `ZREM` |
| PUT | `/cooperatives/{cooperativeUuid}/location` | ADMIN | `GEOADD` |
| DELETE | `/cooperatives/{cooperativeUuid}/location` | ADMIN | `ZREM` GEO |
| GET | `/cooperatives/nearby?latitude=...&longitude=...&radiusKm=...&limit=20` | FUNCIONARIO | `GEOSEARCH` |

Papéis: `FUNCIONARIO` (leitura), `GESTOR` (leitura + atualizar score), `ADMIN` (administração). A autenticação aceita JWTs da `volta-api` com `sub`, `exp`, `userId`, `companyId` e `role` (`EMPLOYEE`, `OPERATOR`, `MANAGER` ou `ADMIN`). Tokens locais de demonstração só são aceitos com o perfil `local`. Envie `Authorization: Bearer <token>`.

A busca GEO retorna somente UUID e distância em linha reta em quilômetros, ordenada pela menor distância. A `volta-api` deve fornecer latitude/longitude da empresa autenticada e completar os dados a partir do PostgreSQL. Latitude aceita entre `-85.051128` e `85.051128`; coordenadas salvas aceitam até seis casas decimais.

## Validação de entrada

Requisições inválidas são recusadas com **`400 Bad Request`** antes de chegar ao Redis:

| Campo | Regra |
|---|---|
| `score` (`POST /ranking/score`) | obrigatório, entre 0 e 1000 |
| `companyUuid` (corpo e rota) | UUID no formato `8-4-4-4-12` em hexadecimal |
| `motivo` | no máximo 255 caracteres |
| `limit` (`GET /ranking/top` e `/cooperatives/nearby`) | entre 1 e 100 |
| latitude | entre -85.051128 e 85.051128; o PUT de localização aceita até seis casas decimais |
| longitude | entre -180 e 180; o PUT de localização aceita até seis casas decimais |
| `radiusKm` (`GET /cooperatives/nearby`) | maior que 0 e no máximo 20.000 km |
| `username` / `password` (`POST /auth/login`) | obrigatórios, no máximo 50 e 100 caracteres |

## Tratamento de erros

Toda resposta de erro segue o mesmo envelope (`success`, `message`, `timestamp`, e `data` quando há detalhe por campo):

| Status | Quando |
|---|---|
| `400` | corpo malformado, tipo de parâmetro inválido ou alguma regra da seção acima |
| `401` | sem token, token inválido/expirado, ou login com credenciais erradas |
| `403` | autenticado, mas sem o papel exigido pela rota |
| `404` | rota inexistente, ou empresa não encontrada no ranking |
| `405` | método HTTP não suportado na rota |
| `500` | erro inesperado (inclui falha de conexão com o Redis) |

## Documentação (Swagger)

Com a API rodando: `http://localhost:8081/swagger-ui.html` (JSON em `/api-docs`). Use **Authorize** com o token do `/auth/login` para testar os endpoints protegidos.

## Estrutura

```
src/main/java/com/volta/ranking/
├── controller/   endpoints HTTP
├── service/      regras de negócio
├── repository/   acesso ao Redis (RedisTemplate / ZSET)
├── dto/          objetos de entrada e saída
├── config/       Redis, segurança e senha
├── security/     JWT (geração, validação e filtro), usuários de demonstração e
│                 tratamento de erros do filtro de segurança (401/403)
├── validation/   constantes de validação compartilhadas (ex.: regex de UUID)
└── exception/    exceções de domínio e tratamento centralizado de erros
```

## Rodando localmente

Requisitos: JDK 21, Maven 3.9+ e Docker. O login de demonstração só fica disponível com `SPRING_PROFILES_ACTIVE=local` e nunca deve ser ativado em produção.

```bash
# PowerShell: inicie um Redis local descartável
docker run --rm -d --name volta-redis-local -p 6379:6379 redis:7-alpine
```

Defina a chave do JWT (mínimo de 256 bits; deve ser a mesma da `volta-api`) e rode:

```bash
# Linux/macOS
export JWT_KEY="<chave-com-256-bits-ou-mais>"
# PowerShell
$env:JWT_KEY = "<chave-com-256-bits-ou-mais>"
$env:SPRING_PROFILES_ACTIVE = "local"

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
| `VOLTA_REDIS_KEYS_COOPERATIVES` | `geo:cooperatives` | Chave do índice GEO |
| `SPRING_PROFILES_ACTIVE` | — | Use `local` apenas para habilitar login demo |

## Usuários de demonstração

Somente no perfil `local`, existem três usuários em memória (`funcionario`, `gestor` e `admin`) com senha de demonstração `senha123`. O padrão é não carregar esses usuários nem oferecer o endpoint `/auth/login`. Em integração, os tokens assinados pela `volta-api` são validados com o mesmo `JWT_KEY`.
