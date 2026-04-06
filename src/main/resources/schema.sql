CREATE TABLE IF NOT EXISTS cliente (
	id TEXT PRIMARY KEY,
	nome TEXT NOT NULL,
	dominio_aplicacao TEXT,
	data_vencimento_dominio DATE,
	ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1))
);
