# checkout-service

Microsserviço responsável por receber pedidos, validar dados, reservar estoque e publicar eventos de processamento.

Stack Java + Quarkus 3.x:
- API REST com Quarkus
- camada de serviço
- integração com Redis e RabbitMQ
- testes de unidade e integração

## Redis e cache de pedidos

- estoque e cache são tratados por um adapter com semântica equivalente a Redis: decremento atômico, incremento de compensação e TTL de cache de 5 minutos.
- a consulta de pedido usa cache-aside: tenta o cache primeiro, cai para o repositório em memória e reaproveita o valor em cache quando ainda válido.
- quando o pedido não existe após 3 tentativas de leitura, o serviço retorna `404` explícito com a mensagem `Pedido não encontrado`.
- a política de TTL pode ser ajustada em testes para validar expiração do cache e fallback para banco.
