# Etapa 10: Segurança, contratos e preparação para produção

## Objetivo

Mapear os requisitos necessários para colocar o sistema em um ambiente de produção real, com segurança, governança de configuração e execução robusta dos microsserviços.

## Tarefas

1. Definir configurações de ambiente com variáveis sensíveis e secret management.
2. Proteger endpoints e integrações com autenticação/autorização mínimas conforme contexto de produção.
3. Definir e validar contratos OpenAPI/JSON para checkout e order-processor.
4. Documentar schemas de fila e versionamento de mensagens para RabbitMQ.
5. Criar profiles de execução para produção e desenvolvimento separados.
6. Preparar Docker Compose e imagens para deploy em ambiente real ou Kubernetes.
7. Implementar healthchecks, readiness e liveness mais completos para todos os serviços.
8. Definir políticas de rollback, failover e recuperação em caso de falha do deploy.
9. Validar que a aplicação atende ao princípio de não exposição de dados sensíveis em logs.
10. Preparar documentação operacional para onboarding e suporte.

## Critérios de aceite

- A aplicação é configurável por ambiente sem hardcoded secrets.
- Interfaces e filas possuem contratos documentados e versionados.
- O deploy em produção é conduzido por processo reprodutível e seguro.
- O sistema possui indicadores do estado operacional e ações de recuperação definidas.
- O ambiente de produção está preparado para evoluir sem regressão de contrato.

## Testes de TDD esperados

- Configuração inválida falha de forma explícita e segura.
- Payload sem contrato válido é rejeitado antes do processamento.
- Healthchecks reportam estado correto dos serviços e dependências.
- Failover e rollback são executáveis em documentação e processo.
- O ambiente de produção não expõe dados sensíveis nos logs.

## Padrões e práticas

- Segregação de ambientes e configuração externa.
- Contratos versionados e documentação pública do serviço.
- Deploy reprodutível e rollback planejado.
- Segurança por default e observabilidade operativa.
