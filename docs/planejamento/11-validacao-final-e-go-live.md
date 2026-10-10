# Etapa 11: Validação final e go-live

## Objetivo

Executar a validação completa do fluxo de ponta a ponta e preparar a entrega final para produção, com evidências de funcionamento, compensação, resiliência e operação.

## Tarefas

1. Subir a infraestrutura completa em Docker Compose com Redis, RabbitMQ, PostgreSQL e microsserviços.
2. Executar migrações de banco e validar população inicial de dados.
3. Rodar smoke tests de saúde dos serviços e dependências.
4. Simular pedido com estoque suficiente e validar resposta HTTP correta.
5. Validar publicação em `order-created` e processamento real do order-processor.
6. Simular falha de processamento e confirmar publicação de `order-compensation`.
7. Validar compensação do estoque e atualização do status do pedido.
8. Executar testes de integração end-to-end com sucesso e falha.
9. Rodar suite de regressão e validação de API com contratos atualizados.
10. Registrar evidência de execução para apresentação e documentação final.
11. Definir checklist de go-live e plano de rollback.

## Critérios de aceite

- O fluxo completo работает de ponta a ponta em ambiente realista.
- Sucesso e compensação são validados com evidência.
- O sistema respeita critérios de produção: persistência, fila, cache, observabilidade e documentação.
- O release final está pronto para operação, com planos de manutenção e rollback.
- O projeto atende ao documento arquitetural com cobertura real e não apenas simulação local.

## Testes de TDD esperados

- Smoke test dos serviços e dependências.
- Pedido bem-sucedido end-to-end.
- Pedido cancelado por falha de processamento.
- Requisição de pedido inexistente com retry e retorno 404.
- Consistência de estoque e status após falha e compensação.

## Entregáveis finais

- README principal atualizado.
- Documentação operacional final.
- Evidência de testes e execução em Docker.
- Checklist de deploy, observabilidade e rollback.
- Relatório de aderência às etapas arquiteturais e de produção.
