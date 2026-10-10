# Etapa 9: Observabilidade, regras de negócio e operação

## Objetivo

Formalizar as regras de negócio e os mecanismos operacionais que faltam para transformar o protótipo em um sistema estável e observável em produção.

## Tarefas

1. Implementar logs estruturados em todos os passos do fluxo de checkout e processor.
2. Definir métricas críticas: estoque reservado, pedidos processados, falhas, compensações e falhas de cache.
3. Adicionar tracing distribuído entre checkout, RabbitMQ e processor.
4. Formalizar retries com limites explícitos de 3 tentativas para busca de pedido.
5. Definir retorno explícito de 404 após falha nas tentativas.
6. Estabelecer política de TTL para cache e expiração de dados temporários.
7. Documentar tratamento de transação e consistência do estoque.
8. Criar contratos de integração e versionamento de payload entre serviços.
9. Definir eventos e status de domínio com rastreabilidade completa do ciclo do pedido.
10. Validar os limites de timeout, rate limiting e resiliência do sistema.

## Critérios de aceite

- O ciclo completo do pedido é rastreável em logs e métricas.
- Falhas e compensações ficam visíveis para operação.
- O sistema documenta regras de retry, TTL e status com clareza operacional.
- Contratos de payload e versionamento são estáveis e testados.
- A operação do ambiente local reproducível auxilia a adoção em produção.

## Testes de TDD esperados

- Requisição de pedido não encontrado dispara retry até 3 tentativas.
- Logs estruturados registram criação, processamento e compensação.
- Payload inválido é rejeitado com contrato explícito.
- Métricas de estoque e fila são colhidas sem falha.
- Fluxo de compensação é rastreável por status e logs.

## Padrões e práticas

- Observabilidade como requisito funcional e não como item complementar.
- Contratos versionados para evolução segura da API e das filas.
- Política explícita de retry, timeout e cancelamento de operação.
- Registro de transição de status como base para auditoria operacional.
