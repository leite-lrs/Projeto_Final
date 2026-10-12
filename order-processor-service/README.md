# order-processor-service

Microsserviço responsável por consumir e processar pedidos, persistir dados e aplicar a lógica de confirmação ou compensação.

## Funcionalidades

- processa mensagens do checkout com validação de payload
- salva o pedido em memória e sincroniza a persistência por repositório com PostgreSQL quando disponível
- aplica a regra de falha simulada para `PROD-999`
- publica compensação em fila interna para cenários de cancelamento
- expõe endpoints de saúde e consulta por pedido
- mantém a estrutura de pedidos versionada por migrations Flyway

## Endpoints principais

- `GET /health`
- `POST /v1/process-order`
- `GET /v1/orders/{pedidoId}`

## Exemplo de processamento

Pedido válido:

```json
{
  "pedido_id": "pedido-01",
  "produto_id": "PROD-001",
  "quantidade": 2,
  "cliente_cpf": "50329291076",
  "cliente_nome": "Fernando Augusto"
}
```

Pedido com falha simulada:

```json
{
  "pedido_id": "pedido-99",
  "produto_id": "PROD-999",
  "quantidade": 1,
  "cliente_cpf": "12345678900",
  "cliente_nome": "Cliente"
}
```

## Validação local

```bash
cd order-processor-service
mvn test
```
