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
| GET | `/incidents/{incidentId}/cooperative-scores?radiusKm=100&limit=10` | qualquer perfil | `200` | — (consulta a `volta-api`) |

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

## Score de cooperativas para uma ocorrência

`GET /api/v1/incidents/{incidentId}/cooperative-scores` calcula, aqui, o quanto cada cooperativa é adequada para receber uma ocorrência. Os dados vêm da `volta-api`: a ocorrência com a análise de IA do `volta-chatbot` (que a `volta-api` já guarda no relatório da ocorrência), a origem da ocorrência e as cooperativas, com latitude e longitude. A distância é calculada aqui (fórmula de Haversine, em linha reta). O resultado não é gravado: cada chamada recalcula.

O token do usuário é repassado à `volta-api`, que continua aplicando o isolamento por empresa. Uma ocorrência de outra empresa responde `404`.

### Quem entra na lista

Uma cooperativa só é avaliada se atender às mesmas regras que a `volta-api` aplica ao solicitar a coleta:

- a categoria do resíduo está nas especialidades da cooperativa;
- quando o tipo de resíduo tem risco padrão `HIGH`, a cooperativa tem a especialidade `PERIGOSO`;
- está dentro do raio informado (padrão 100 km, máximo 20.000 km).

Resíduo `RADIOATIVO` responde `422`, porque não pode ser destinado a cooperativas. Ocorrência sem coordenadas de origem também responde `422`.

### Como o score é calculado

O score vai de 0 a 100 e é a média ponderada de três critérios, cada um de 0 a 1. Esta API não guarda cópia dos enums da `volta-api`: o contrato usa os valores como texto e só compara os que o cálculo exige (`PERIGOSO`, `RADIOATIVO`, `HIGH` e `CRITICAL`).

| Critério | Cálculo |
|---|---|
| Proximidade | `1 - distância / raio`: 1 na origem, 0 na borda do raio |
| Nota | `nota média / 5`. Cooperativa sem avaliação (nota 0 ou ausente) recebe 0,5 |
| Adequação ao risco | 1 quando o risco não é alto ou a cooperativa tem `PERIGOSO`; 0,4 quando o risco é alto e ela não tem |

A ocorrência tem risco alto (`highRisk` na resposta) quando o risco padrão do tipo de resíduo, a contaminação informada ou o `aiContaminationLevel` do relatório de IA é `HIGH`. É por aqui que a análise do `volta-chatbot` influencia o score: se a IA estimar contaminação `HIGH`, cooperativas sem `PERIGOSO` perdem pontos, sem serem excluídas.

| Prioridade da ocorrência | Proximidade | Nota | Adequação |
|---|---|---|---|
| `HIGH` ou `CRITICAL` | 0,6 | 0,2 | 0,2 |
| `LOW` ou `MEDIUM` | 0,4 | 0,3 | 0,3 |

Os pesos, o valor neutro da nota, a penalidade de adequação e o raio padrão ficam em `volta.scoring.*` no `application.yml`. O empate é desfeito pela menor distância e depois pelo nome. A resposta traz o `breakdown` (0 a 100) de cada critério e o `highRisk` usado.

### Contrato que a `volta-api` precisa expor

Esta API consome dois endpoints de leitura da `volta-api`, ambos com `Authorization: Bearer <token do usuário>` e acessíveis a `ADMIN`, `MANAGER` e `EMPLOYEE`. Campos extras são ignorados.

`GET /incidents/{id}/scoring-context`, restrito à empresa do usuário (`404` fora dela):

```json
{
  "incidentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "priority": "HIGH",
  "wasteType": { "category": "PERIGOSO", "defaultRiskLevel": "HIGH" },
  "contaminationLevel": "MEDIUM",
  "origin": { "latitude": -23.561684, "longitude": -46.655981 },
  "aiReport": {
    "detectedWasteType": "Óleo lubrificante",
    "aiContaminationLevel": "HIGH",
    "recommendations": "Isolar a área e acionar coleta especializada"
  }
}
```

- `priority` (`LOW`, `MEDIUM`, `HIGH` ou `CRITICAL`), `category`, `defaultRiskLevel` e `contaminationLevel` (`LOW`, `MEDIUM` ou `HIGH`) vão como texto, nos nomes canônicos da `volta-api`. `priority`, `wasteType.category` e `wasteType.defaultRiskLevel` são obrigatórios; sem eles a resposta é `502`.
- `contaminationLevel` deve ser o nível já resolvido (informado ou, na falta, o risco padrão do tipo de resíduo).
- `origin` é o ponto de onde a distância é medida. Hoje a empresa não tem latitude e longitude na `volta-api`; é preciso definir de onde esse ponto vem (sede da empresa ou da área).
- `aiReport` pode ser `null` quando a ocorrência não tem análise de IA. Só o `aiContaminationLevel` entra no cálculo.

`GET /cooperatives/scoring-data`, lista com os dados de todas as cooperativas (hoje `GET /cooperatives` é exclusivo do `ADMIN`):

```json
[
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "name": "Cooperativa Recicla Sul",
    "latitude": -23.561684,
    "longitude": -46.655981,
    "averageRating": 4.5,
    "specialties": ["PAPEL", "PLASTICO", "PERIGOSO"]
  }
]
```

`specialties` é a lista de categorias já normalizadas pela `volta-api`, que é quem conhece o enum `WasteCategory`. Esta API compara os nomes exatamente, sem normalizar texto.

### Como a `volta-api` deve montar essas respostas

Os dois endpoints só leem e reorganizam dados que a `volta-api` já tem. O mapeamento é:

| Campo do contrato | Origem na `volta-api` |
|---|---|
| `incidentId` | `Incident.id` |
| `priority` | `Incident.priority` |
| `wasteType.category` | `Incident.wasteType.category` |
| `wasteType.defaultRiskLevel` | `Incident.wasteType.defaultRiskLevel` |
| `contaminationLevel` | `Incident.contaminationLevel`; se for nulo, `wasteType.defaultRiskLevel` |
| `origin.latitude` e `origin.longitude` | **ainda não existe**: `Company` só tem `address` e `Area` só tem `locationDescription` |
| `aiReport.*` | `AiReport` ligado à ocorrência (`detectedWasteType`, `aiContaminationLevel`, `recommendations`); `null` se não houver |
| `id`, `name`, `latitude`, `longitude`, `averageRating` | `Cooperative` |
| `specialties` | `Cooperative.specialties`, que é um texto separado por vírgula; o endpoint deve devolvê-lo como lista de categorias |

Pontos a resolver na `volta-api`:

- **Origem da ocorrência:** adicionar `latitude` e `longitude` (`DECIMAL(9,6)`, como em `Cooperative`) a `Area` ou a `Company`, com a migração correspondente, e preenchê-las no cadastro. Se a origem faltar, a `volta-api` deve devolver `origin: null` e esta API responde `422`.
- **Isolamento:** `scoring-context` busca a ocorrência pelo `id` **e** pela `companyId` do token. Ocorrência de outra empresa responde `404`.
- **Permissão:** `scoring-data` é uma rota nova com `@PreAuthorize` para `ADMIN`, `MANAGER` e `EMPLOYEE`. O `GET /cooperatives` atual continua só para `ADMIN`, porque devolve CNPJ e outros dados que o score não precisa.
- **Resposta enxuta:** nenhum dos dois devolve CNPJ, fotos, descrição do funcionário ou texto completo do relatório de IA.

Erros da `volta-api`: `404` vira `404`, `403` vira `403` e qualquer outra falha (incluindo timeout) vira `502`. A URL e o timeout são `VOLTA_API_URL` e `VOLTA_API_TIMEOUT_MILLIS`.

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
├── config/        Redis, segurança, OpenAPI, cliente da volta-api, pesos do score e usuários locais
├── controller/    endpoints HTTP
│   └── docs/      interfaces com as anotações do Swagger
├── usecase/       contratos dos casos de uso consumidos pelos controllers
├── service/       regras de negócio, cálculo do score e isolamento por empresa
├── client/        cliente HTTP da volta-api
├── mapper/        conversão das estruturas do Redis em DTOs
├── repository/    acesso ao Redis (ZSET, lista e GEO)
├── dto/           DTOs por domínio (auth, ranking, cooperative, scoring e voltaapi)
├── security/      usuário autenticado, filtro e provedor de JWT
├── handler/       tratamento global de erros e respostas 401/403
├── exception/     exceções de negócio
└── utils/         cálculo de distância
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
| `REDIS_USERNAME` | vazio | Usuário do Redis (ACL); o Redis Cloud usa `default` |
| `REDIS_PASSWORD` | vazio | Senha do Redis; fora do local, configure em secret manager |
| `SPRING_DATA_REDIS_SSL_ENABLED` | `false` | Ative (`true`) TLS quando o provedor Redis exigir conexão criptografada |
| `VOLTA_API_URL` | `http://localhost:8080` | URL da `volta-api`, usada no score de cooperativas |
| `VOLTA_API_TIMEOUT_MILLIS` | `3000` | Timeout de conexão e leitura das chamadas à `volta-api` |
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
