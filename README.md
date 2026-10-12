# Projeto Final - Flash Sale

Este repositório foi iniciado com a infraestrutura mínima necessária para o sistema de checkout e processamento de pedidos.

## Stack tecnológica proposta

- Java 21
- Quarkus 3.x (framework principal da aplicação)
- Redis para estoque e cache
- RabbitMQ para filas assíncronas
- PostgreSQL para persistência dos microsserviços
- Docker Compose para orquestração local
- Estrutura inicial de dois microsserviços: checkout-service e order-processor-service

## Arquivos principais

- docker-compose.yml
- .env.example
- docs/planejamento/README.md
- docs/planejamento/01-fundacao-e-arquitetura.md
- docs/planejamento/02-checkout-service.md
- docs/planejamento/03-order-processor-service.md
- docs/planejamento/04-testes-e-integracao.md
- docs/planejamento/05-execucao-e-demonstracao.md

## Como subir a infraestrutura

1. Instale o Docker Desktop ou o Docker Engine.
2. Copie o arquivo .env.example para .env e ajuste os valores sensíveis do ambiente (`APP_API_KEY`, `POSTGRES_PASSWORD`, `RABBITMQ_DEFAULT_PASS`).
3. Execute:

```bash
docker compose up -d --build
```

4. Verifique os serviços:
- Redis: localhost:6379
- RabbitMQ: localhost:5672 e management em localhost:15672
- PostgreSQL: localhost:5432
- Checkout Service: localhost:3001
- Order Processor Service: localhost:3002

## Segurança e produção

- O ambiente é configurável por variáveis de ambiente; nenhuma secret sensível deve ficar hardcoded no código ou no compose.
- A autenticação por API key é ativada via `APP_REQUIRE_AUTH=true` ou automaticamente quando `APP_ENV=prod`; configure `APP_API_KEY` com um segredo próprio. Valores de exemplo como `change-me-in-production` não autenticam.
- Endpoints de health (`/health`, `/health/live`, `/health/ready`) ficam públicos para monitoramento, enquanto endpoints de negócio exigem a chave quando o ambiente exigir autenticação.
- O OpenAPI fica disponível em `/openapi` e a UI em `/swagger-ui`.

## Execução e demonstração do fluxo

### 1) Validar saúde dos serviços

```bash
curl -s http://localhost:3001/health
curl -s http://localhost:3002/health
```

### 2) Criar pedido com estoque disponível

```bash
curl -s -X POST http://localhost:3001/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"produto_id":"PROD-001","quantidade":2,"cliente_cpf":"50329291076","cliente_nome":"Fernando Augusto"}'
```

Resultado esperado: HTTP 202 Accepted com `status: "PROCESSING"` e `pedido_id` gerado.

### 3) Consultar pedido

```bash
curl -s http://localhost:3001/v1/orders/{pedido_id}
```

### 4) Simular falha no processamento

```bash
curl -s -X POST http://localhost:3001/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"produto_id":"PROD-999","quantidade":1,"cliente_cpf":"12345678900","cliente_nome":"Cliente"}'
```

A SKU `PROD-999` foi prevista para falhar no processador, gerando status `CANCELLED` e publicação da compensação.

### 5) Validar processamento do pedido

```bash
curl -s http://localhost:3002/v1/orders/{pedido_id}
```

O pedido com falha deve refletir `status: "CANCELLED"` e `compensacao: true`.

## Observação

A validação sintática do Docker Compose foi concluída com sucesso usando `docker compose config --quiet`, mas o ambiente local de execução foi bloqueado neste host porque o daemon do Docker Desktop/Engine não está disponível (`open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified`). Portanto, a execução real dos containers e dos endpoints precisa ser feita em uma máquina com Docker em funcionamento.
