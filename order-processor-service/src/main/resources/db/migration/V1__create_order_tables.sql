CREATE TABLE IF NOT EXISTS orders (
    pedido_id VARCHAR(100) PRIMARY KEY,
    produto_id VARCHAR(100) NOT NULL,
    quantidade INTEGER NOT NULL,
    cliente_cpf VARCHAR(20) NOT NULL,
    cliente_nome VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    motivo VARCHAR(255),
    compensacao BOOLEAN NOT NULL DEFAULT FALSE,
    data_hora_criacao TIMESTAMP WITH TIME ZONE NOT NULL,
    data_hora_atualizacao TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_cliente_cpf ON orders (cliente_cpf);
