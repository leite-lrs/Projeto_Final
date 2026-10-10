# Etapa 2: Microsserviço de checkout

## Objetivo

Implementar a API de entrada do sistema, responsável por validar pedidos, reservar estoque e publicar eventos assíncronos.

## Tarefas

1. Criar a estrutura do microsserviço checkout-service.
2. Implementar o endpoint POST /v1/orders.
3. Validar entrada:
   - produto_id obrigatório
   - quantidade maior que zero
   - cliente_cpf obrigatório
4. Consultar saldo do estoque no Redis.
5. Reservar estoque de forma atômica.
6. Gerar UUID v4 para o pedido.
7. Publicar mensagem na fila order-created.
8. Retornar 202 Accepted em sucesso.
9. Retornar 409 Conflict em estoque insuficiente.
10. Implementar GET /v1/orders/{pedido_id}.
11. Aplicar estratégia de cache-aside com Redis.
12. Implementar o consumidor da fila order-compensation.
13. Devolver o estoque reservado ao Redis após compensação.

## Critérios de aceite

- O pedido é aceito apenas com estoque disponível.
- O estoque não fica negativo.
- A resposta ao cliente é rápida e assíncrona.
- A compensação restabelece o estoque corretamente.
- A consulta do pedido usa cache antes do banco.

## Testes de TDD esperados

- Pedido com quantidade válida e estoque suficiente.
- Pedido com quantidade inválida.
- Pedido com estoque insuficiente.
- Pedido consultado no cache.
- Pedido compensado devolve estoque.

## Padrões e práticas

- API-first: payloads e respostas documentados antes da implementação.
- GoF: Service layer + repositories + message publisher.
- Tratamento de erros com respostas explícitas HTTP.
