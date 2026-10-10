# Etapa 3: Microsserviço order processor

## Objetivo

Processar pedidos em segundo plano, persistir dados e decidir se o pedido deve seguir ou ser compensado.

## Tarefas

1. Criar a estrutura do microsserviço order-processor-service.
2. Implementar o consumidor da fila order-created.
3. Validar o payload da mensagem recebida.
4. Persistir o pedido em banco de dados do serviço.
5. Definir status inicial como PROCESSING.
6. Processar a ordem de compra com regra simulada de sucesso/falha.
7. Em caso de sucesso:
   - salvar no banco
   - atualizar status para CONFIRMED
8. Em caso de falha:
   - atualizar status para CANCELLED
   - publicar mensagem em order-compensation
9. Registrar logs estruturados para acompanhar o fluxo.

## Critérios de aceite

- Mensagens da fila são consumidas e processadas reliably.
- O pedido é persistido com status correto.
- A falha do processamento dispara a compensação.
- O fluxo de sucesso e falha está demonstrável.

## Testes de TDD esperados

- Mensagem válida gera pedido confirmado.
- Falha simulada gera pedido cancelado e mensagem de compensação.
- Mensagem inválida é rejeitada com log e tratamento adequado.

## Padrões e práticas

- Strategy: separar comportamento de sucesso e falha.
- Factory: criar eventos e mensagens em formatos normalizados.
- Observability: logs e status são essenciais para a verificação da saga.
