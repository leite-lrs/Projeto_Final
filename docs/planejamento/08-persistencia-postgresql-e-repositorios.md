# Etapa 8: Persistência em PostgreSQL e repositórios reais

## Objetivo

Implementar a camada de persistência real para os serviços, de modo que o pedido e os registros do processamento deixem de depender de mapas em memória.

## Tarefas

1. Definir o modelo de dados do pedido no order-processor.
2. Criar as migrations/DDL para tabelas de pedidos, eventos e status.
3. Implementar entidades JPA/ORM com mapeamento correto de status e metadata.
4. Criar repositories para persistência e consulta do pedido.
5. Definir a persistência do checkout para consultas e registros de compensação se necessário.
6. Garantir transações de escrita consistentes para atualização de estado.
7. Versionar a estrutura de dados e validar migrations em ambiente Docker.
8. Implementar consultas por pedido e histórico de atualizações.
9. Validar a recuperação após reinicialização dos serviços e do banco.

## Critérios de aceite

- O pedido é persistido no PostgreSQL do order-processor.
- O checkout e o processor possuem acesso real ao banco em ambiente local/produção.
- Consulta por pedido e atualização de status são consistentes.
- O banco é a fonte de verdade para o processamento e a recuperação do status.
- A estrutura de dados está versionada e pronta para migração em ambientes reais.

## Testes de TDD esperados

- Salvamento de pedido confirmado no PostgreSQL.
- Consulta de pedido por ID retorna dados persistidos.
- Atualização de status em processamento e compensação.
- Falha no registro gera tratamento adequado e log de erro.
- Repositorio com múltiplas tentativas mantém consistência.

## Padrões e práticas

- Repository pattern para separar acesso a dados do domínio.
- Migrations como fato operacional e não como artefato opcional.
- Entidades com status explícito e timestamps de auditoria.
- Tratamento de concorrência e consistência para operações de escrita.
