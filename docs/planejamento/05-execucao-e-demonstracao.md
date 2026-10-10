# Etapa 5: Execução e demonstração

## Objetivo

Preparar o ambiente para execução local e demonstrar o funcionamento completo do projeto.

## Tarefas

1. Configurar e subir os containers com docker compose up.
2. Verificar healthchecks de Redis, RabbitMQ e PostgreSQL.
3. Executar os microsserviços localmente.
4. Simular pedido com estoque suficiente.
5. Validar resposta 202 Accepted.
6. Confirmar publicação da mensagem na fila order-created.
7. Verificar processamento e persistência do pedido.
8. Simular falha no processo.
9. Confirmar publicação da fila order-compensation.
10. Verificar devolução do estoque no Redis.
11. Validar atualização do status para CANCELLED.
12. Registrar como foi feita a demonstração final.

## Critérios de aceite

- O ambiente completo funciona localmente.
- Os principais fluxos são demonstráveis em poucos minutos.
- A apresentação consegue mostrar sucesso e compensação em sequência.

## Entregáveis finais

- README principal do projeto.
- Arquivos de ambiente e infraestrutura.
- Documentação do fluxo de execução.
- Evidência de testes e demonstração.
