# Etapa 1: Fundação e arquitetura

## Objetivo

Estabelecer a base técnica e a estrutura de domínio antes da implementação funcional.

## Tarefas

1. Definir os dois microsserviços:
   - checkout-service
   - order-processor-service
2. Definir os contratos de API:
   - POST /v1/orders
   - GET /v1/orders/{pedido_id}
3. Definir os canais de comunicação:
   - order-created
   - order-compensation
4. Definir os modelos de domínio:
   - Pedido
   - StatusPedido
   - ProdutoEstoque
5. Definir persistência por serviço:
   - checkout: Redis para estoque e cache, banco próprio para registros/consulta
   - order-processor: banco próprio para persistência do pedido
6. Configurar a infraestrutura mínima com Docker Compose.

## Critérios de aceite

- A estrutura dos serviços está clara.
- O contrato HTTP e as filas estão definidos antes do código.
- Infra local pronta para execução.
- O relacionamento entre entidades e fluxos de compensação está documentado.

## Testes de TDD esperados

- Teste de contrato de criação de pedido.
- Teste de estoque insuficiente.
- Teste de pedido não encontrado.
- Teste de compensação de estoque.

## Padrões de projeto

- Strategy para regras de processamento do pedido.
- Factory para criação de mensagens e consumidores.
- Adapter para integração com Redis, RabbitMQ e banco de dados.
