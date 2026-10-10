# Etapa 7: RabbitMQ, filas e SAGA coreografada

## Objetivo

Substituir a simulação local de eventos por uma comunicação assíncrona real com RabbitMQ, implementando a SAGA coreografada completa entre checkout e order-processor.

## Tarefas

1. Definir os contratos de payload para `order-created` e `order-compensation`.
2. Implementar o producer da fila `order-created` no checkout.
3. Implementar o consumer da fila `order-created` no order-processor.
4. Implementar o producer da fila `order-compensation` no order-processor.
5. Implementar o consumer da fila `order-compensation` no checkout.
6. Configurar ack/nack, retry e fila de dead-letter para mensagens não processadas.
7. Garantir idempotência dos processamentos e tratamento de mensagens duplicadas.
8. Definir e registrar a transição de status do pedido ao longo da saga.
9. Validar a execução real do fluxo: criação -> processamento -> compensação -> restauração.

## Critérios de aceite

- Mensagens reais são publicadas e consumidas por RabbitMQ.
- O checkout e o processor não dependem mais de filas em memória.
- A falha no processamento dispara compensação automática.
- A compensação restaura estoque e atualiza o estado do pedido no checkout.
- O fluxo end-to-end é demonstrável em Docker com infraestrutura real.

## Testes de TDD esperados

- Publicação de `order-created` após reserva de estoque bem-sucedida.
- Processamento de mensagem válida no processor.
- Falha simulada gera `order-compensation` com payload correto.
- Mensagem rejeitada sem ack volta a fila ou para DLQ conforme regra.
- Consumo de compensação restaura o estoque corretamente.

## Padrões e práticas

- Mensageria assíncrona com filas explícitas e contratos versionados.
- Coreografia da SAGA por eventos e status de domínio.
- Tratamento de erro com retry, dead-letter e observabilidade de fila.
- Garantia de consistência eventual com compensação explícita.
