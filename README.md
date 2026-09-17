# CodeLong

Backend do **CodeLong**, um jogo de conhecimento voltado para **Programação e Engenharia de Software**.

O nome combina **Code** com **Long** (龙, *lóng* — dragão em chinês), representando evolução, desafio e progressão através dos níveis. O jogador avança por uma sequência de perguntas que cresce em dificuldade, do nível mais fácil ao mais difícil, e compete por posição no ranking.

> Este repositório contém **somente o backend**. Não há frontend, mobile ou interface gráfica.

---

## 1. Stack

| Item | Versão |
| --- | --- |
| Kotlin | 2.4.20 (estável) |
| Spring Boot | 4.1.1 (estável) |
| Java | 21 |
| Build | **Maven** (não usamos Gradle) |
| Banco | MongoDB |
| API | REST + OpenAPI/Swagger (springdoc 3.1.1) |
| Segurança | Spring Security + JWT (JJWT 0.13.0) + BCrypt |
| Testes | JUnit 5, jqwik (disponível), Testcontainers |
| Container | Docker + Docker Compose |

---

## 2. Arquitetura

O projeto segue **Arquitetura Hexagonal (Ports and Adapters)** com as dependências apontando sempre para dentro.

```
                     ┌───────────────────────────────────────────────┐
   HTTP/REST  ─────▶│  infrastructure.web  (Controllers, DTOs)      │
                     └───────────────┬───────────────────────────────┘
                                     │  (usa)
                     ┌───────────────▼───────────────────────────────┐
                     │  application  (Use Cases, Commands, Results)  │
                     └───────────────┬───────────────────────────────┘
                                     │  (depende de interfaces)
                     ┌───────────────▼───────────────────────────────┐
                     │  domain  (Model, Value Objects, Services,     │
                     │           Exceptions, Ports)                  │
                     └───────────────▲───────────────────────────────┘
                                     │  (implementa as Ports)
        ┌────────────────────────────┴────────────────────────────┐
        │  infrastructure.persistence (MongoDB)                   │
        │  infrastructure.security    (JWT, BCrypt)               │
        │  infrastructure.config      (wiring, OpenAPI, Clock)    │
        └─────────────────────────────────────────────────────────┘
```

Regra de dependência: **domain ← application ← infrastructure**. O domínio é independente de Spring, MongoDB, HTTP, JWT e de qualquer detalhe de infraestrutura (as classes do domínio e da aplicação não carregam anotações de framework; o wiring é feito em `infrastructure.config.UseCaseConfig`).

### Estrutura de pacotes

```
com.codelong
├── CodeLongApplication
├── domain
│   ├── model        User, Question, Game
│   ├── valueobject  UserId, Email, Username, Difficulty, Category, GameStatus, RankEntry, ...
│   ├── service      GameSequencer, RankingPolicy
│   ├── exception    DomainException e subtipos
│   └── port         UserRepository, QuestionRepository, GameRepository, RankingRepository,
│                    PasswordEncoder, TokenService
├── application
│   ├── command      Commands / Queries (parameter objects)
│   ├── result       AuthenticationResult, RankingPage
│   ├── service      TokenIssuer, UserFactory, GameFactory
│   └── usecase      Casos de uso (um por operação)
└── infrastructure
    ├── web           Controllers, DTOs, ApiExceptionHandler
    ├── persistence   Documents, mappers, Spring Data repositories, adapters Mongo
    ├── security      JwtTokenService, JwtAuthenticationFilter, SecurityConfig, BCrypt
    ├── bootstrap     AdminBootstrap (admin inicial)
    └── config        UseCaseConfig (wiring), OpenApiConfig, ClockConfig
```

### Princípios seguidos

- **Orientação a Objetos** com entidades que protegem suas invariantes (não são objetos anêmicos).
- **SOLID**, com destaque para Single Responsibility e Dependency Inversion (casos de uso dependem de _Ports_).
- **Lei de Demeter**: objetos expõem operações; nada de cadeias como `game.user.profile.ranking.score`.
- **Máximo de 3 parâmetros por método** (Clean Code). Quando necessário, agrupa-se em Value Object / Command Object com significado.
- **Sem regra de negócio em Controller**: o Controller é apenas adapter de entrada.
- **Sem abstrações artificiais**: interfaces existem para _Ports_ e contratos relevantes, não como ornamento.

---

## 3. Modelo de domínio

### Value Objects

`UserId`, `QuestionId`, `GameId`, `OptionId`, `Email`, `Username`, `PasswordHash`, `QuestionOption`, `Category`, `Difficulty`, `Role`, `AccountStatus`, `QuestionStatus`, `GameStatus`, `GameQuestion` (snapshot), `AnswerRecord`, `RankEntry`, `TokenClaims`.

### Entidades

- **User** — id, username, email, passwordHash, role, status, createdAt, updatedAt. Username e email únicos. Nunca expõe o hash.
- **Question** — id, statement, options, correctOption, explanation, category, difficulty, status. Invariantes: ≥ 2 opções, resposta correta obrigatoriamente entre as opções, ids de opção únicos.
- **Game** — id, userId, username (snapshot), status, startedAt, completedAt, currentQuestionIndex, questions (snapshots), answers, score, correctAnswers, wrongAnswers, version (optimistic lock).

---

## 4. Perguntas e dificuldade

### Categorias

`JAVA`, `KOTLIN`, `SPRING`, `OOP`, `SOLID`, `CLEAN_CODE`, `DESIGN_PATTERNS`, `SOFTWARE_ARCHITECTURE`, `TESTING`, `DATABASE`, `REST`, `MICROSERVICES`, `GIT`, `DOCKER`, `KAFKA`, `SYSTEM_DESIGN`.

Novas categorias podem ser adicionadas como novos valores do enum, sem alterar regras existentes.

### 10 níveis de dificuldade e pontuação

| Nível | Nome | Pontos por acerto |
| --- | --- | --- |
| 1 | `VERY_EASY` | 10 |
| 2 | `EASY` | 20 |
| 3 | `EASY_PLUS` | 30 |
| 4 | `MEDIUM` | 40 |
| 5 | `MEDIUM_PLUS` | 50 |
| 6 | `HARD` | 60 |
| 7 | `HARD_PLUS` | 70 |
| 8 | `VERY_HARD` | 80 |
| 9 | `EXPERT` | 90 |
| 10 | `MASTER` | 100 |

Erro concede **0** pontos.

### Ordenação das perguntas

Todas as perguntas **ativas** participam da partida. A ordem é:

1. todas as perguntas de nível 1;
2. todas de nível 2;
3. ... até o nível 10.

Dentro de cada nível a ordem é **aleatória**. É proibido *shuffle* global — a dificuldade sempre cresce.

A sequência é **persistida em snapshot** no documento da partida no momento da criação. Perguntas editadas, desativadas ou removidas depois não afetam partidas já iniciadas.

---

## 5. Fluxo do jogo

1. `POST /api/games` cria a partida e devolve a primeira pergunta (sem a resposta correta).
2. `GET /api/games/{gameId}/current-question` retorna a pergunta atual (sem resposta correta nem explicação).
3. `POST /api/games/{gameId}/answers` envia **somente a escolha** (`optionId`).
4. O backend valida, confere a resposta, calcula a pontuação, registra e avança.
5. Ao responder a última pergunta, a partida vira `COMPLETED`.

O cliente **nunca** informa nem recebe antecipadamente: `score`, `currentQuestionIndex`, `status`, `userId`, `correctAnswers`, `wrongAnswers`.

### Estados da partida

`IN_PROGRESS`, `COMPLETED`, `ABANDONED`. Partidas finalizadas não aceitam novas respostas.

---

## 6. Ranking

- Mostra **apenas a melhor pontuação de cada usuário** (uma linha por usuário).
- Paginado.
- **Desempate determinístico**, nesta ordem:
  1. maior pontuação;
  2. maior número de acertos;
  3. menor tempo total (duração da partida);
  4. data de obtenção da pontuação (mais antiga primeiro).

`GET /api/rankings/me` retorna a melhor entrada do usuário autenticado e sua posição (ou **204** se ele ainda não concluiu nenhuma partida).

---

## 7. Segurança

- Senhas armazenadas com **BCrypt**; nunca em texto puro e nunca retornadas.
- Autenticação via **JWT** (HS256), secret e expiração por variável de ambiente.
- Endpoints protegidos por padrão; papéis:
  - `USER`: jogar, responder, consultar seus resultados e o ranking;
  - `ADMIN`: gerenciar perguntas.
- O `userId` usado nas operações vem **sempre do token**, nunca do corpo da requisição.
- Acesso a recurso de outro usuário → **403**.
- Consultas Mongo são construídas por critérios tipados (proteção contra NoSQL Injection).
- Erros padronizados, sem stack trace e sem detalhes internos.
- Tokens e senhas não são registrados em log.
- CORS configurável por ambiente. HTTPS esperado em produção.

---

## 8. Concorrência

Respostas usam **optimistic locking** (`@Version` no documento da partida). Duas requisições simultâneas de resposta: apenas uma persiste; a outra recebe **409 Conflict** e pode recarregar o estado. Isso impede:

- responder duas vezes a mesma pergunta;
- duplicar pontuação;
- avançar duas perguntas;
- finalizar incorretamente.

---

## 9. API REST

Cabeçalho autenticado: `Authorization: Bearer <token>`.

| Método | Rota | Acesso | Descrição |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | público | Cria a conta |
| POST | `/api/auth/login` | público | Autentica e emite o JWT |
| GET | `/api/users/me` | autenticado | Dados do próprio usuário |
| POST | `/api/games` | autenticado | Inicia uma partida |
| GET | `/api/games/{gameId}` | dono | Detalhes da partida |
| GET | `/api/games/{gameId}/current-question` | dono | Pergunta atual (sem resposta) |
| POST | `/api/games/{gameId}/answers` | dono | Envia a escolha |
| POST | `/api/games/{gameId}/abandon` | dono | Abandona a partida |
| GET | `/api/rankings` | autenticado | Ranking paginado |
| GET | `/api/rankings/me` | autenticado | Posição do próprio usuário (204 se não ranqueado) |
| POST | `/api/admin/questions` | ADMIN | Cria pergunta |
| GET | `/api/admin/questions` | ADMIN | Lista paginada/filtrada |
| GET | `/api/admin/questions/{id}` | ADMIN | Detalhe da pergunta |
| PUT | `/api/admin/questions/{id}` | ADMIN | Atualiza pergunta |
| PATCH | `/api/admin/questions/{id}/status` | ADMIN | Ativa/inativa pergunta |
| DELETE | `/api/admin/questions/{id}` | ADMIN | Remove pergunta |
| GET | `/api/admin/users` | ADMIN | Lista paginada/filtrada de usuários (`status`, `role`) |
| PATCH | `/api/admin/users/{userId}/status` | ADMIN | Ativa/desativa conta |

Swagger UI: `/swagger-ui.html` · OpenAPI JSON: `/v3/api-docs`.

### Formato de erro

Todos os erros usam o mesmo corpo:

```json
{
  "code": "GAME_FINISHED",
  "message": "This game is already finished and cannot receive new answers",
  "timestamp": "2026-09-17T12:00:00Z"
}
```

Códigos HTTP: `400` entrada inválida · `401` não autenticado **ou** credenciais inválidas · `403` sem permissão / recurso de outro usuário · `404` não encontrado · `409` conflito (username/email duplicado, partida finalizada, escrita concorrente) · `500` erro inesperado.

### Exemplos

Registrar e autenticar:

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"secret123"}'

TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"alice","password":"secret123"}' | jq -r .token)
```

Criar pergunta (ADMIN), iniciar partida e responder:

```bash
curl -s -X POST http://localhost:8080/api/admin/questions \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{
        "statement":"Qual palavra-chave define uma constante em Kotlin?",
        "options":[{"id":"a","text":"val"},{"id":"b","text":"var"},{"id":"c","text":"let"}],
        "correctOption":"a",
        "explanation":"Em Kotlin, val declara uma referencia imutavel.",
        "category":"KOTLIN",
        "difficulty":"EASY"
      }'

curl -s -X POST http://localhost:8080/api/games -H "Authorization: Bearer $TOKEN"

curl -s -X POST http://localhost:8080/api/games/$GAME_ID/answers \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"optionId":"a"}'
```

O cliente envia **apenas** `optionId`; `score`, `correctOption` e `explanation` são calculados/decididos no backend e devolvidos somente depois da resposta.

### Observabilidade

| Rota | Descrição |
| --- | --- |
| `/actuator/health` | Estado da aplicação (com probes de liveness/readiness) |
| `/actuator/info` | Nome, descrição e versão do build |
| `/swagger-ui.html` | Swagger UI (esquema `bearerAuth`) |

---

## 10. Como executar

### Docker Compose (app + MongoDB)

```bash
cp .env.example .env      # preencha CODELONG_JWT_SECRET e CODELONG_ADMIN_PASSWORD
docker compose up --build
```

A API sobe em `http://localhost:8080` e o MongoDB fica exposto em `localhost:27017` (volume `mongo-data`).

Somente o banco (útil para rodar a app pela IDE/Maven):

```bash
docker compose up -d mongodb
```

### Localmente

Pré-requisitos: JDK 21 e Maven 3.9+.

```bash
docker compose up -d mongodb
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

No perfil `dev` já existe um admin de bootstrap (`admin` / `admin12345`) e um secret de JWT apenas para desenvolvimento — **troque em produção**.

### Build e testes

```bash
mvn verify          # compila, roda os testes e empacota o jar
mvn test            # apenas os testes
```

Os testes de integração usam **Testcontainers** e, portanto, exigem um Docker em execução. Sem Docker eles são automaticamente ignorados (`disabledWithoutDocker`), então `mvn test` continua funcionando.

```bash
docker build -t codelong .
docker run --rm -p 8080:8080 --env-file .env -e CODELONG_MONGO_URI=mongodb://host.docker.internal:27017/codelong codelong
```

> Se sua rede usa um mirror Maven corporativo, ajuste o seu `~/.m2/settings.xml`; o projeto não versiona configuração de repositório.

---

## 11. Variáveis de ambiente

| Variável | Descrição | Padrão |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Perfil ativo (`dev`, `test`, `prod`) | `dev` |
| `CODELONG_MONGO_URI` | URI do MongoDB | `mongodb://localhost:27017/codelong` |
| `CODELONG_JWT_SECRET` | Secret HMAC do JWT (≥ 256 bits) | — (obrigatório em prod) |
| `CODELONG_JWT_EXPIRATION` | Expiração do token (ex.: `2h`, `30m`) | `8h` |
| `CODELONG_CORS_ALLOWED_ORIGINS` | Origens permitidas (CSV) | `http://localhost:3000,http://localhost:5173` |
| `CODELONG_ADMIN_USERNAME` | Username do admin inicial (bootstrap) | `admin` no perfil `dev` |
| `CODELONG_ADMIN_PASSWORD` | Senha do admin inicial (bootstrap) | `admin12345` no perfil `dev` |
| `SERVER_PORT` | Porta HTTP | `8080` |

O admin de bootstrap só é criado se **username e password** estiverem preenchidos e se o usuário ainda não existir. Nunca versione secrets reais; em produção use `CODELONG_JWT_SECRET` e `CODELONG_ADMIN_PASSWORD` vindos de um gerenciador de segredos.

---

## 12. Índices MongoDB

| Coleção | Índice | Tipo |
| --- | --- | --- |
| `users` | `username` | único |
| `users` | `email` | único |
| `questions` | `status`, `category`, `difficulty` | composto (`question_search_idx`) |
| `questions` | `createdAt` | simples |
| `games` | `userId`, `status` | composto (`game_user_idx`) |
| `games` | `status`, `score`, `correctAnswers` | ranking (`game_ranking_idx`) |

Os índices são criados automaticamente na inicialização (`spring.data.mongodb.auto-index-creation: true`). Em produção com múltiplas instâncias, avalie desabilitar a criação automática e aplicar os índices por migração.

---

## 13. Testes

Situação atual: **63 testes**, todos verdes (`mvn test`).

- **Domínio (20)** — JUnit 5: pontuação por nível de dificuldade, progressão da pergunta, conclusão/abandono, bloqueio de resposta após o fim, posse da partida, reconstituição do agregado; sequenciador (ordem crescente de dificuldade, embaralhamento apenas dentro do nível, filtro de inativas); política de ranking (os quatro critérios de desempate).
- **Aplicação (29)** — casos de uso com dublês in-memory dos repositórios: cadastro (hash da senha, username/email duplicado, senha fraca), login (por username e por email, senha errada, conta inativa), fluxo de resposta ponta a ponta, início de partida (sem perguntas ativas, usuário inativo), gestão de perguntas e paginação do ranking.
- **Integração (18)** — Testcontainers com MongoDB 7 real:
  - persistência: round-trip de usuário/pergunta/partida, índices únicos, busca filtrada e paginada, exclusão;
  - **optimistic lock verificado de verdade**: duas cópias da mesma partida, a segunda gravação lança `ConcurrentGameModificationException` (409);
  - ranking por agregação do Mongo (melhor partida concluída por usuário, partidas em andamento ignoradas, posição individual);
  - **E2E via HTTP** (porta aleatória): registro/login, 401 sem token, 403 de usuário na área admin, fluxo completo jogar→responder→concluir→ranking, 204 em `/api/rankings/me` sem partidas, abandono com 409 depois, 400 de opção inválida, 404 de partida inexistente e o ciclo de vida da pergunta pelo admin.

jqwik está no classpath para testes property-based; ainda não há testes de propriedade escritos.

---

## 14. Licença

Projeto privado — todos os direitos reservados.
