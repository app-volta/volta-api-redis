# volta-api-redis

API auxiliar do Volta para o ranking de empresas e o índice geoespacial de cooperativas, feita em Java 21 + Spring Boot 4 usando Redis.

O ranking usa um Sorted Set (`ranking:companies`) e o histórico de pontuação usa uma lista por empresa (`ranking:history:{companyId}`). A proximidade usa Redis GEO (`geo:cooperatives`), com os IDs das cooperativas como membros. O PostgreSQL da `volta-api` continua sendo a fonte da verdade; este serviço guarda índices derivados. A `volta-api` informa o ponto de origem da empresa e completa os dados das cooperativas a partir dos IDs retornados.

## Endpoints

Base: `/api/v1`. As respostas de sucesso devolvem o DTO diretamente, sem envelope.

| Método | Rota | Acesso | Resposta | Redis |
|---|---|---|---|---|
| POST | `/auth/login` | público, somente perfil `local` | `200` | — |
| GET | `/ranking?page=0&size=100` | qualquer perfil | `200` | `ZREVRANGE` |
| GET | `/ranking/top?limit=10` | qualquer perfil | `200` | `ZREVRANGE` |
| GET | `/ranking/total` | qualquer perfil | `200` | `ZCARD` |
| GET | `/ranking/{companyId}/position` | própria empresa ou ADMIN | `200` | `ZREVRANK`, `ZSCORE`, `ZCARD` |
| GET | `/ranking/{companyId}/score` | própria empresa ou ADMIN | `200` | `ZSCORE` |
| GET | `/ranking/{companyId}/history?limit=20` | própria empresa ou ADMIN | `200` | `LRANGE` |
| POST | `/ranking/score` | MANAGER (própria empresa) ou ADMIN | `204` | `ZADD`, `LPUSH` |
| PUT | `/ranking/scores` | ADMIN | `200` | `ZADD` por item |
| DELETE | `/ranking/{companyId}` | ADMIN | `204` | `ZREM` |
| PUT | `/cooperatives/{cooperativeId}/location` | ADMIN | `204` | `GEOADD` |
| DELETE | `/cooperatives/{cooperativeId}/location` | ADMIN | `204` | `ZREM` na chave GEO |
| GET | `/cooperatives/nearby?latitude=...&longitude=...&radiusKm=...&limit=20` | qualquer perfil | `200` | `GEOSEARCH` |

A busca GEO devolve somente `cooperativeId` e `distanceKm` (distância em linha reta), ordenada pela menor distância. Latitude aceita entre `-85.051128` e `85.051128`; coordenadas salvas aceitam até seis casas decimais.

## Autenticação e perfis

A autenticação aceita os JWTs emitidos pela `volta-api`, com as claims `sub`, `exp`, `userId`, `companyId` e `role`, assinados com o mesmo `JWT_KEY`. Envie `Authorization: Bearer <token>`.

| Perfil | Descrição |
|---|---|
| `ADMIN` | Administra o ranking e o índice GEO e acessa qualquer empresa |
| `MANAGER` | Consulta e atualiza a pontuação da própria empresa |
| `EMPLOYEE` | Consulta o ranking e os dados da própria empresa |
| `OPERATOR` | Mesmo acesso de leitura do `EMPLOYEE` |

### Isolamento por empresa

Quem não é `ADMIN` só consulta (`position`, `score`, `history`) e atualiza (`POST /ranking/score`) a própria empresa, a do `companyId` do token. Outra empresa responde `404`, como na `volta-api`, para não revelar quem está no ranking. A listagem do ranking (`/ranking`, `/ranking/top` e `/ranking/total`) continua visível a qualquer perfil, porque é a classificação pública entre as empresas.

## Validação de entrada

Entradas inválidas são recusadas com `400 Bad Request` antes de chegar ao Redis:

| Campo | Regra |
|---|---|
| `score` | obrigatório, entre 0 e 1000 |
| `companyId` e `cooperativeId` | UUID válido, no corpo e na rota |
| `reason` | no máximo 255 caracteres |
| `scores` (`PUT /ranking/scores`) | de 1 a 500 itens, cada um com as regras acima |
| `page` | de 0 a 100000 |
| `size` e `limit` | de 1 a 100 |
| latitude | entre -85.051128 e 85.051128; o PUT de localização aceita até seis casas decimais |
| longitude | entre -180 e 180; o PUT de localização aceita até seis casas decimais |
| `radiusKm` | maior que 0 e no máximo 20.000 |
| `email` e `password` (`POST /auth/login`) | obrigatórios, no máximo 100 caracteres |

## Tratamento de erros

Todo erro segue o formato **ProblemDetail** (RFC 9457), com `Content-Type: application/problem+json`, o mesmo da `volta-api`:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Ranking entry not found",
  "instance": "/api/v1/ranking/550e8400-e29b-41d4-a716-446655440000/position"
}
```

Erros de validação do corpo incluem o campo `errors`, com a mensagem de cada campo.

| Status | Quando |
|---|---|
| `400` | corpo malformado, tipo de parâmetro inválido ou alguma regra da seção anterior |
| `401` | sem token, token inválido ou expirado, ou login com credenciais erradas |
| `403` | autenticado, mas sem o perfil exigido pela rota |
| `404` | rota inexistente, empresa fora do ranking ou de outra empresa |
| `405` | método HTTP não suportado na rota |
| `500` | erro inesperado, inclui falha de conexão com o Redis |

## Histórico de pontuação

Cada alteração de score (`POST /ranking/score`, carga em lote e remoção) grava uma entrada com o valor, o `reason`, o autor e o momento em UTC. O Redis guarda as últimas 100 alterações de cada empresa, da mais recente para a mais antiga. A remoção do ranking mantém o histórico e registra a própria remoção.

## Persistência e reconstrução do índice

O Redis precisa de persistência ligada para não perder o ranking e o histórico quando reiniciar. No `volta-devops`, o Compose e o Kubernetes sobem o Redis com `appendonly yes` (AOF, gravação a cada segundo) e volume próprio.

Se o Redis for perdido ou limpo, o ranking é reconstruído com a carga em lote, que o PostgreSQL da `volta-api` alimenta:

```bash
curl -X PUT http://localhost:8081/api/v1/ranking/scores \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"scores":[{"companyId":"550e8400-e29b-41d4-a716-446655440000","score":950,"reason":"Reconstrução do ranking"}]}'
```

A carga aceita até 500 empresas por chamada e não remove quem ficou de fora do lote. Quem chama a carga (um job agendado ou a própria `volta-api`) define como converter os indicadores do PostgreSQL na escala de 0 a 1000. Cada cooperativa pode ser gravada de novo com `PUT /api/v1/cooperatives/{cooperativeId}/location`.

O readiness do Actuator inclui a conexão com o Redis; sem Redis, `/actuator/health/readiness` fica indisponível e o Kubernetes pode retirar o pod do tráfego. Se a chave GEO estiver vazia, a busca de proximidade devolve lista vazia com sucesso: o serviço não distingue um índice legitimamente vazio de um ainda não sincronizado.

## Documentação (Swagger)

Swagger UI e `/api-docs` ficam habilitados somente no perfil `local`, em `http://localhost:8081/swagger-ui.html`. Fora dele, a documentação é desabilitada e as rotas são bloqueadas. Use **Authorize** com um token JWT para testar os endpoints protegidos.

## Estrutura

Segue a mesma organização em camadas da `volta-api`:

```
Controller  →  UseCase (interface)  →  Service  →  Repository  →  Redis
    │                                     │
    └── DTO de request/response           └── Mapper (estrutura do Redis ⇄ DTO)
```

```
src/main/java/com/volta/ranking/
├── config/        Redis, segurança, OpenAPI, codificador de senha e usuários locais
├── controller/    endpoints HTTP
│   └── docs/      interfaces com as anotações do Swagger
├── usecase/       contratos dos casos de uso consumidos pelos controllers
├── service/       regras de negócio e isolamento por empresa
├── mapper/        conversão das estruturas do Redis em DTOs
├── repository/    acesso ao Redis (ZSET, lista e GEO)
├── dto/           DTOs de entrada e saída, por domínio (auth, ranking, cooperative)
├── security/      usuário autenticado, filtro e provedor de JWT
├── handler/       tratamento global de erros e respostas 401/403
└── exception/     exceções de negócio
```

## Rodando localmente

Requisitos: JDK 21, Maven 3.9+ e Docker. Copie `.env.example` para `.env` e substitua os valores de exemplo. O arquivo `.env` é ignorado pelo Git. Para executar a aplicação em Maven, exporte as variáveis no shell; para Docker, use `--env-file .env`.

O perfil `local` habilita os usuários de demonstração e o login, e deve ser usado somente na máquina local. Nunca use `SPRING_PROFILES_ACTIVE=local` em QA ou produção. Em ambientes compartilhados, configure os segredos no gerenciador de segredos do ambiente, não em arquivos versionados.

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
| `JWT_EXPIRATION` | `86400000` | Validade do token, em ms |
| `REDIS_HOST` | `localhost` | Host do Redis |
| `REDIS_PORT` | `6379` | Porta do Redis |
| `SPRING_DATA_REDIS_PASSWORD` | — | Senha opcional do Redis; configure em secret manager fora do local |
| `SPRING_DATA_REDIS_SSL_ENABLED` | `false` | Ative (`true`) TLS quando o provedor Redis exigir conexão criptografada |
| `SERVER_PORT` | `8081` | Porta HTTP |
| `VOLTA_REDIS_KEYS_COOPERATIVES` | `geo:cooperatives` | Chave do índice GEO |
| `SPRING_PROFILES_ACTIVE` | — | Use `local` somente na máquina local; habilita o login de demonstração |

O desligamento é gracioso: ao receber o sinal de parada, a aplicação conclui as requisições em andamento por até 20 segundos.

## Usuários de demonstração

Somente no perfil `local`, existem três usuários em memória, todos da mesma empresa de demonstração e com a senha `senha123`:

| E-mail | Perfil |
|---|---|
| `employee@volta.com` | EMPLOYEE |
| `manager@volta.com` | MANAGER |
| `admin@volta.com` | ADMIN |

O padrão é não carregar esses usuários nem oferecer o endpoint `/auth/login`. Em integração, os tokens assinados pela `volta-api` são validados com o mesmo `JWT_KEY`.

## Testes

```bash
mvn verify
```

Os testes de integração precisam de um Redis em `REDIS_HOST` e `REDIS_PORT` (como no CI). O JaCoCo gera o relatório em `target/site/jacoco/index.html` e o build falha se a cobertura de linhas ficar abaixo de 85%.
