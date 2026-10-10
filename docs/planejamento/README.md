# Planejamento de implementação

Este diretório reúne a sequência de etapas para construir o projeto de forma incremental, com foco em arquitetura, TDD, API-first e padrões de projeto (GoF).

## Ordem sugerida

1. Definir a base arquitetural e os contratos de integração.
2. Implementar o microsserviço de checkout.
3. Implementar o microsserviço de processamento de pedidos.
4. Cobrir fluxos com testes unitários e de integração.
5. Validar a execução completa com Docker Compose e apresentação final.
6. Migrar o estoque e o cache para Redis real com concorrência e TTL explícitos.
7. Implementar a SAGA coreografada real com RabbitMQ e compensação via fila.
8. Persistir pedidos e eventos em PostgreSQL com migrations e repositories reais.
9. Formalizar observabilidade, retry, contratos e regras de negócio de produção.
10. Preparar segurança, deploy e operação da solução em produção.
11. Validar o go-live final e registrar evidências de deploy e rollback.

## Sequência completa

- [01-fundacao-e-arquitetura.md](./01-fundacao-e-arquitetura.md)
- [02-checkout-service.md](./02-checkout-service.md)
- [03-order-processor-service.md](./03-order-processor-service.md)
- [04-testes-e-integracao.md](./04-testes-e-integracao.md)
- [05-execucao-e-demonstracao.md](./05-execucao-e-demonstracao.md)
- [06-redis-estoque-e-cache.md](./06-redis-estoque-e-cache.md)
- [07-rabbitmq-filas-e-saga-coreografada.md](./07-rabbitmq-filas-e-saga-coreografada.md)
- [08-persistencia-postgresql-e-repositorios.md](./08-persistencia-postgresql-e-repositorios.md)
- [09-observabilidade-regras-de-negocio-e-operacao.md](./09-observabilidade-regras-de-negocio-e-operacao.md)
- [10-seguranca-contratos-e-deploy-producao.md](./10-seguranca-contratos-e-deploy-producao.md)
- [11-validacao-final-e-go-live.md](./11-validacao-final-e-go-live.md)

## Princípios aplicados

- TDD: escrever testes primeiro para regras de negócio e fluxos críticos.
- API-first: definir payloads, contratos e respostas antes da implementação.
- Java + Quarkus 3.x: framework principal para os microsserviços, com REST e mensageria nativa.
- GoF: priorizar classes/serviços responsáveis e padrões como Strategy, Factory e Adapter quando necessário.
- SAGA coreografada: compensação do estoque por fila de compensação.
- Observabilidade mínima: logs estruturados e status do pedido explícito.

## Entregáveis esperados

- Infra local com Redis, RabbitMQ e PostgreSQL.
- Dois microsserviços independentes.
- APIs REST documentadas por contrato.
- Integração por filas assíncronas.
- Testes de sucesso e falha.
- Documentação de execução para demonstração.
