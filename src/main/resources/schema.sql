CREATE TABLE IF NOT EXISTS cliente (
	id TEXT PRIMARY KEY,
	nome TEXT NOT NULL,
	contato TEXT,
	dominio_aplicacao TEXT,
	data_vencimento_dominio DATE,
	valor_mensal NUMERIC NOT NULL DEFAULT 0,
	ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1))
);

CREATE TABLE IF NOT EXISTS movimentacao (
	id TEXT PRIMARY KEY,
	tipo TEXT NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
	valor NUMERIC NOT NULL,
	data_ocorrencia DATE NOT NULL,
	descricao TEXT,
	cliente_id TEXT,
	FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);

CREATE TABLE IF NOT EXISTS usuarios (
	id TEXT PRIMARY KEY,
	username TEXT NOT NULL UNIQUE,
	senha TEXT NOT NULL,
	ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1)),
	role TEXT NOT NULL DEFAULT 'ADMIN'
);
