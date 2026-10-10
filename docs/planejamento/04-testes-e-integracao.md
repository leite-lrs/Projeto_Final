# Etapa 4: Testes, integração e qualidade

## Objetivo

Provar que o sistema cumpre requisitos de negócio, concorrência e compensação, além de manter a base saudável.

## Tarefas

1. Escrever testes unitários das regras de negócio.
2. Cobrir casos de sucesso e falha para checkout.
3. Cobrir casos de confirmação e compensação no processor.
4. Implementar testes de integração com Redis e RabbitMQ.
5. Validar uso de cache-aside e retry de consulta.
6. Validar não negatividade do estoque.
7. Rodar testes com ambiente local via Docker Compose.

## Casos críticos a cobrir

- estoque insuficiente
- reserva atômica
- ordem cancelada via compensação
- pedido não encontrado após 3 tentativas
- falha de processamento e retorno de estoque

## Critérios de aceite

- Testes automatizados cobrem os fluxos principais.
- A falha e o sucesso foram testados de forma separada.
- A arquitetura de fila e cache está validada em execução real.

## Padrões aplicados

- TDD como caminho obrigatório para regra de negócio.
- Testes de contrato HTTP para API-first.
- Testes de integração para verificar o comportamento real do sistema.
