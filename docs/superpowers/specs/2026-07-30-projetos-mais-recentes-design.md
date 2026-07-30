# Ordenação de projetos por criação

## Objetivo

O endpoint público `GET /api/projetos` deve retornar os projetos do mais
recente para o mais antigo. Como o site externo pagina o array recebido da API,
um projeto recém-criado passa a aparecer na primeira página.

“Mais recente” significa criado por último. Editar um projeto existente não
altera sua posição na listagem.

## Persistência e migração

A tabela `projeto` receberá a coluna `created_at`, preenchida automaticamente
na criação e preservada nas edições.

O inicializador de schema continuará criando a tabela completa em bancos
novos. Em bancos SQLite existentes, ele adicionará a coluna quando necessário
e preencherá registros legados para que nenhum projeto fique sem data.

Como projetos legados não possuem a data histórica de criação, o preenchimento
usará a melhor informação disponível no banco durante a migração. A garantia
principal é que todos os projetos criados depois da mudança sejam posicionados
antes dos registros antigos.

## Consulta e contrato da API

O repositório terá uma consulta explícita ordenada por `created_at DESC` e por
`id DESC` como desempate estável. O serviço usará essa consulta em
`listarProjetos()`.

O formato JSON e as rotas não mudam. A paginação permanece no site consumidor;
a API apenas passa a entregar o array na ordem correta.

## Tratamento de edição

As consultas de atualização não modificarão `created_at`. Trocar título,
descrição, imagem ou link preservará a posição cronológica original.

## Testes

- O teste do repositório comprovará que a consulta retorna primeiro o projeto
  com criação mais recente.
- O teste do serviço comprovará que `listarProjetos()` usa a consulta ordenada.
- O teste do inicializador comprovará a criação e a migração da coluna
  `created_at` em SQLite.
- A suíte completa será executada para detectar regressões no CRUD e na API.

## Fora do escopo

- Implementar paginação no backend.
- Alterar o site externo.
- Reordenar projetos após edição.
- Expor `created_at` no JSON público.
