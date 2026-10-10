# Auditoria do Projeto Final

## Versões

- [2026-10-10-v1.0-auditoria-projeto-final.md](./2026-10-10-v1.0-auditoria-projeto-final.md) — versão inicial da auditoria de implementação

## Objetivo

Registrar o que já foi implementado e o que ainda falta para atender ao documento de arquitetura e requisitos descritos em [docs/Projeto_final.md](../docs/Projeto_final.md).

## Resumo executivo

O projeto está em um estado de protótipo funcional local, com endpoints HTTP simulando o fluxo de checkout e processamento de pedidos. A base de infraestrutura e parte da lógica de negócio já existem, mas ainda faltam integrações reais com Redis, RabbitMQ e PostgreSQL, além da automação do fluxo de SAGA entre os microsserviços.

## Status geral

- Infraestrutura local: parcialmente concluída
- Serviços Java/Quarkus: concluídos como protótipo
- Integração com Redis: não implementada de forma real
- Integração com RabbitMQ: não implementada de forma real
- Persistência com PostgreSQL: não implementada
- Fluxo de SAGA: parcialmente modelado, mas não integrado em produção
- Cobertura de testes: parcialmente atendida
