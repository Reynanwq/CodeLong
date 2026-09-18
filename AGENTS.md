# AGENTS.md — Runbook: subir a aplicacao completa (backend + frontend)

Documento operacional para um agente (ou humano) subir o CodeLong do zero nesta maquina.
Contem os comandos exatos, a ordem correta e as **duas armadilhas conhecidas** deste ambiente.

---

## Pre-requisitos

| Item | Versao usada | Observacao |
| --- | --- | --- |
| JDK | 21+ | testado com Zulu 25 (`C:\Users\Reynan Paiva\scoop\apps\zulu25-jdk`) |
| Maven | 3.9.9 | `C:\apache-maven-3.9.9\bin\mvn.cmd` |
| Node | 22.x | **Angular CLI 20**; a CLI 21+ exige Node >= 22.22.3 |
| Docker Desktop | em execucao | necessario para o MongoDB e para os testes de integracao |
| Portas livres | `3000`, `8080`, `27018` | a `27017` **nao** pode ser usada (ver armadilhas) |

---

## ⚠️ Armadilhas conhecidas desta maquina (leia antes)

### 1. Maven quebra por causa do Nexus corporativo
O `~/.m2/settings.xml` tem um mirror (`<mirrorOf>!central-direct,*</mirrorOf>`) que manda **todo** o trafego Maven para o Nexus da Gubee, inacessivel fora da VPN.
**Sempre** passe as settings locais do projeto:

```powershell
mvn -s .mvn/settings.xml <goal>
```

> **Nao altere o `~/.m2/settings.xml`.** O `.mvn/settings.xml` e gitignored, entao um clone novo nao o possui — crie conforme o Passo 1.

### 2. A porta 27017 pertence a outro ambiente (Gubee)
A `27017` esta ocupada por:
- container `mongo-mongo-1` (infra da Gubee, `workspace-gubee/gubee-apps/gubee-infra/.../docker-compose-mongo-dev.yml`)
- um `mongod` nativo escutando em `127.0.0.1:27017`

**Nunca suba nem escreva nada na 27017.** O MongoDB do CodeLong roda **isolado na 27018**, com container e volume proprios.

### 3. `spring.data.mongodb.uri` e ignorado no Spring Boot 4
Os `application*.yml` usam `spring.data.mongodb.uri`, mas no **Spring Boot 4.1.1** o prefixo canonico passou a ser **`spring.mongodb.uri`**. Consequencia: a URI dos ymls e ignorada e o driver cai no default `localhost:27017` (o ambiente da Gubee!).
Por isso o Passo 3 passa a URI como **system property** `-Dspring.mongodb.uri=...`.

---

## Passo 1 — `.mvn/settings.xml` (so na primeira vez)

Verifique se existe: `Test-Path .mvn\settings.xml`. Se nao existir, crie com este conteudo (resolve do Maven Central, sem tocar no arquivo global):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.1.0">
    <profiles>
        <profile>
            <id>central-only</id>
            <repositories>
                <repository>
                    <id>central</id>
                    <url>https://repo.maven.apache.org/maven2</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>false</enabled></snapshots>
                </repository>
            </repositories>
            <pluginRepositories>
                <pluginRepository>
                    <id>central</id>
                    <url>https://repo.maven.apache.org/maven2</url>
                    <releases><enabled>true</enabled></releases>
                    <snapshots><enabled>false</enabled></snapshots>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>central-only</activeProfile>
    </activeProfiles>
</settings>
```

---

## Passo 2 — MongoDB isolado (27018)

```powershell
docker run -d --name codelong-mongodb --restart unless-stopped -p 127.0.0.1:27018:27017 -v codelong-mongo-data:/data/db mongo:7.0
```

Verificar (deve imprimir `1`):

```powershell
docker exec codelong-mongodb mongosh --quiet --eval "db.adminCommand('ping').ok"
```

Se o container ja existir, apenas `docker start codelong-mongodb`.
O bind e `127.0.0.1`, entao o banco **nao fica exposto na rede corporativa**.

---

## Passo 3 — Backend (porta 8080)

```powershell
cd "C:\Users\Reynan Paiva\ranking"
& "C:\apache-maven-3.9.9\bin\mvn.cmd" -s .mvn/settings.xml spring-boot:run -Dspring-boot.run.profiles=dev "-Dspring-boot.run.jvmArguments=-Dspring.mongodb.uri=mongodb://127.0.0.1:27018/codelong"
```

Esse comando fica em primeiro plano. Para rodar em segundo plano, crie um `.cmd` com as linhas acima e dispare com `Start-Process -WindowStyle Hidden -RedirectStandardOutput <log>`.

Verificar (deve retornar `UP` e o log deve citar `address=127.0.0.1:27018`):

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

O perfil `dev` cria automaticamente o admin inicial: **`admin` / `admin12345`**.

---

## Passo 4 — Frontend (porta 3000)

```powershell
cd "C:\Users\Reynan Paiva\ranking\frontend"
npm install          # apenas na primeira vez
node_modules\.bin\ng.cmd serve --port 3000
```

Abrir **http://localhost:3000**.

> A porta **3000** e obrigatoria por padrao: e a origem liberada no CORS do backend (`CODELONG_CORS_ALLOWED_ORIGINS`). Se usar outra, suba o backend com a variavel ajustada.

---

## Passo 5 — Verificacao ponta a ponta

```powershell
# 1) banco isolado respondendo
docker exec codelong-mongodb mongosh codelong --quiet --eval "db.questions.countDocuments()"

# 2) backend saudavel
Invoke-RestMethod http://localhost:8080/actuator/health

# 3) frontend servindo
(Invoke-WebRequest http://localhost:3000 -UseBasicParsing).StatusCode

# 4) CORS liberado para o frontend
$login = Invoke-WebRequest http://localhost:8080/api/auth/login -Method Post `
  -Headers @{ Origin = 'http://localhost:3000' } -ContentType 'application/json' `
  -Body '{"identifier":"admin","password":"admin12345"}' -UseBasicParsing
$login.Headers['Access-Control-Allow-Origin']   # esperado: http://localhost:3000
```

Fluxo funcional: entrar com `admin`/`admin12345` → **Nova partida** → responder com **setas ↑ ↓ + Enter** → observar o **cronometro de 20s** → **Ranking** (abas Classico/Genocida). Na tela de fim/derrota, **Enter** repete a partida e **← →** escolhem entre jogar novamente, ver ranking ou voltar ao inicio.

---

## Testes e cobertura

```powershell
cd "C:\Users\Reynan Paiva\ranking"
& "C:\apache-maven-3.9.9\bin\mvn.cmd" -s .mvn/settings.xml clean verify
```

- 1081 testes, 0 falhas
- Gate do JaCoCo: **100% de linha e de branch** (o build falha se cair)
- Os testes de integracao usam Testcontainers (exigem Docker) e sao ignorados sem Docker

Frontend:

```powershell
cd frontend
node_modules\.bin\ng.cmd build
```

---

## Parar tudo

```powershell
# frontend e backend: encerre os processos (ou feche as janelas)
Get-NetTCPConnection -LocalPort 8080 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
Get-NetTCPConnection -LocalPort 3000 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }

# banco (mantem os dados no volume)
docker stop codelong-mongodb

# remover tudo (inclusive dados)
docker rm -f codelong-mongodb
docker volume rm codelong-mongo-data
```

---

## Solucao de problemas

| Sintoma | Causa provavel | Acao |
| --- | --- | --- |
| `Plugin ... could not be resolved` / `Connect timed out` no Maven | faltou `-s .mvn/settings.xml` (Nexus corporativo) | repita o comando com `-s .mvn/settings.xml` |
| Backend conecta em `localhost:27017` | `spring.data.mongodb.uri` ignorado no Boot 4 | passe `-Dspring.mongodb.uri=mongodb://127.0.0.1:27018/codelong` |
| `Bind for 0.0.0.0:27017 failed: port is already allocated` | tentou usar a 27017 (Gubee) | use a 27018 (Passo 2) |
| Frontend com erro de CORS | servido em porta diferente de 3000 | sirva na 3000 ou ajuste `CODELONG_CORS_ALLOWED_ORIGINS` |
| `The Angular CLI requires a minimum Node.js version` | CLI 21+ com Node < 22.22.3 | use `@angular/cli@20` |
| Ranking vazio | nenhuma partida com >= 10 respostas concluidas/abandonadas | jogue ao menos 10 perguntas |

---

## Regras de negocio que afetam o uso

- **Tempo por pergunta**: 20 segundos (`GameRules.ANSWER_TIME_LIMIT_SECONDS`). Se o tempo esgotar, a pergunta conta como **erro**, a partida **avanca** e a resposta enviada depois e recusada com **409 `ANSWER_TIME_EXPIRED`**.
- **Ranking**: entram **todas as partidas** `COMPLETED`, `ABANDONED` e `DEFEATED` com **no minimo 10 respostas (CLASSIC) ou 5 (GENOCIDA)**. O ranking e **separado por modo** (`GET /api/rankings?mode=CLASSIC|GENOCIDA`); sem o parametro, mistura os dois. Desempate: `score` ↓ → menor tempo → mais acertos → data mais antiga.
- **Uma partida inclui todas as perguntas ativas** (hoje 840). Por isso o minimo de respostas existe: sem ele, ninguem apareceria no ranking.
