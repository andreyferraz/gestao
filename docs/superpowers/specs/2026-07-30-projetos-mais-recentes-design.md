# Ordenação de projetos por última alteração

## Objetivo

O endpoint público `GET /api/projetos` deve retornar os projetos do mais
recentemente alterado para o menos recente. Como o site externo pagina o array
recebido da API, um projeto recém-criado ou recém-editado passa a aparecer na
primeira página.

“Mais recente” significa criado ou editado por último. Uma edição só altera a
posição depois que a atualização do projeto for persistida com sucesso.

## Persistência e migração

A tabela `projeto` receberá a coluna `updated_at`, preenchida automaticamente
na criação e renovada em toda edição bem-sucedida.

O inicializador de schema continuará criando a tabela completa em bancos
novos. Em bancos SQLite existentes, ele adicionará a coluna quando necessário
e preencherá registros legados para que nenhum projeto fique sem data.

Como projetos legados não possuem a data histórica de alteração, o
preenchimento usará a melhor informação disponível no banco durante a
migração. A garantia principal é que todos os projetos criados ou editados
depois da mudança sejam posicionados antes dos registros antigos.

## Consulta e contrato da API

O repositório terá uma consulta explícita ordenada por `updated_at DESC` e por
`id DESC` como desempate estável. O serviço usará essa consulta em
`listarProjetos()`.

O formato JSON e as rotas não mudam. A paginação permanece no site consumidor;
a API apenas passa a entregar o array na ordem correta.

## Tratamento de criação e edição

A criação persistirá o instante atual junto com os demais campos. As consultas
de edição persistirão um novo instante junto com título, descrição, imagem e
link. Se a edição falhar por concorrência ou erro de persistência, a data
anterior permanecerá inalterada porque a atualização inteira ocorre na mesma
instrução SQL e transação.

## Testes

- O teste do repositório comprovará que a consulta retorna primeiro o projeto
  com alteração mais recente.
- O teste do serviço comprovará que `listarProjetos()` usa a consulta ordenada.
- O teste do inicializador comprovará a criação e a migração da coluna
  `updated_at` em SQLite.
- Os testes de criação e edição comprovarão que o timestamp é persistido nas
  duas operações.
- A suíte completa será executada para detectar regressões no CRUD e na API.

## Fora do escopo

- Implementar paginação no backend.
- Alterar o site externo.
- Expor `updated_at` no JSON público.
