# Etapa 6: Redis real e gestão de estoque

## Objetivo

Implementar a camada de dados e cache do checkout com Redis real, substituindo o armazenamento em memória por mecanismos de concorrência, retenção e recuperação confiáveis.

## Tarefas

1. Definir a estrutura de chaves e TTL para estoque e cache de pedidos.
2. Implementar o adapter Redis no microsserviço de checkout.
3. Substituir o mapa em memória por operações atômicas de decremento e incremento do estoque.
4. Usar scripts Lua ou operações atômicas para evitar overselling em concorrência.
5. Implementar o padrão cache-aside para consultas de pedido.
6. Definir política de TTL do cache, com valor sugerido de 5 minutos.
7. Aplicar retry para leituras de pedido não encontrado, até 3 tentativas.
8. Garantir 404 explícito após falha de busca e documentar o comportamento.
9. Validar healthcheck e resiliência do Redis em ambiente Docker.

## Critérios de aceite

- O estoque é calculado e atualizado no Redis de forma atômica.
- Não há queda para saldo negativo em cenários de concorrência.
- A consulta de pedido usa cache com TTL e fallback para banco.
- O comportamento de falha em cache ou pedido ausente está documentado e testado.
- O checkout consegue operar sem depender de estado em memória compartilhado.

## Testes de TDD esperados

- Reserva atômica com múltiplas requisições simultâneas.
- Devolução correta do estoque após compensação.
- Consulta do pedido em cache e em banco.
- Pedido não encontrado após 3 tentativas retorna 404.
- TTL do cache expira e força nova leitura.

## Padrões e práticas

- Adapter para Redis com abstração de repositório.
- Consistência eventual controlada por TTL e invalidação.
- Observabilidade com logs e métricas de estoque e cache.
- Retry controlado para falhas transitórias de leitura.
