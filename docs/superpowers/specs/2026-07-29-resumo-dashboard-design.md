# Design do Módulo de Resumo do Painel

## Objetivo

Adicionar ao painel um módulo de resumo que seja a tela inicial da aplicação e concentre os indicadores gerais, a distribuição dos clientes ativos por valor mensal exato e os cinco cadastros mais recentes de clientes e leads.

O módulo deve ter uma fronteira própria no backend para que novos indicadores e visualizações possam ser adicionados futuramente sem sobrecarregar os endpoints operacionais de clientes e leads.

## Escopo

O primeiro incremento contém:

- os indicadores atuais de total de clientes, receita mensal ativa e domínios ativos;
- um gráfico de pizza que agrupa somente clientes ativos pelo valor mensal exato;
- os cinco clientes cadastrados mais recentemente;
- os cinco leads cadastrados mais recentemente;
- a aba Resumo como tela inicial em toda abertura ou recarga do painel;
- estados responsivos, vazios e de falha.

Não fazem parte deste incremento filtros, períodos configuráveis, outros gráficos, exportações ou detalhamento interativo das fatias.

## Arquitetura

Será criado um pacote `module/resumo` com controller, service, DTOs de resposta e acesso de leitura aos dados necessários. O endpoint `GET /resumo` fornecerá uma resposta consolidada para a tela:

```json
{
  "indicadores": {
    "totalClientes": 12,
    "receitaMensalAtiva": 4200.00,
    "dominiosAtivos": 10
  },
  "distribuicaoValoresMensais": [
    {
      "valorMensal": 250.00,
      "quantidadeClientes": 4
    }
  ],
  "ultimosClientes": [
    {
      "id": "uuid",
      "nome": "Empresa Orion",
      "createdAt": "2026-07-29T14:30:00Z",
      "valorMensal": 250.00,
      "ativo": true
    }
  ],
  "ultimosLeads": [
    {
      "id": "uuid",
      "nome": "Maria Souza",
      "createdAt": "2026-07-29T14:25:00Z",
      "orcamentoManutencaoHospedagem": 450.00
    }
  ]
}
```

O módulo de resumo será somente leitura. A criação, edição e remoção continuarão pertencendo aos módulos existentes.

## Persistência e ordenação

As tabelas `cliente` e `lead` receberão a coluna `created_at`. Novos registros terão esse valor definido automaticamente na inserção e ele nunca será modificado em uma edição.

O campo `updated_at` do lead continuará sendo atualizado nas edições, mas não participará da escolha dos cinco leads mais recentes.

As instalações SQLite existentes serão migradas pelos inicializadores de esquema:

- leads antigos usarão o `updated_at` existente como a melhor aproximação disponível para `created_at`, normalizado para o formato UTC adotado pelo novo campo;
- clientes antigos receberão o instante da migração, pois o banco atual não possui informação que permita recuperar a data real;
- quando dois registros tiverem o mesmo `created_at`, a ordenação por `id` descendente será usada como desempate estável.

O `schema.sql` também será atualizado para novas instalações. As datas serão armazenadas em formato UTC compatível com ISO 8601 e retornadas sem alteração pela API.

## Regras dos indicadores e do gráfico

- `totalClientes` conta clientes ativos e inativos.
- `receitaMensalAtiva` soma `valor_mensal` somente dos clientes ativos.
- `dominiosAtivos` conta os clientes ativos, mantendo a definição atual do painel.
- O gráfico considera somente clientes ativos.
- Cada valor mensal exato produz uma fatia, com a quantidade de clientes que pagam esse valor.
- Valores monetários são calculados com `BigDecimal`.
- Os grupos são ordenados por valor mensal crescente.
- Clientes ativos com valor mensal zero pertencem à fatia `R$ 0,00`.
- Sem clientes ativos, a distribuição será uma lista vazia.

## Listas recentes

As duas listas usam a data original de criação em ordem decrescente e retornam no máximo cinco registros.

A lista de clientes inclui ativos e inativos e exibe:

- nome;
- data e hora de criação;
- valor mensal;
- status.

A lista de leads exibe:

- nome;
- data e hora de criação;
- orçamento de manutenção/hospedagem.

Quando houver menos de cinco registros, serão exibidos somente os existentes.

## Interface

A navegação lateral ganhará a aba `Resumo` como primeiro item. Ela será a aba ativa declarada no HTML e será ativada explicitamente na inicialização do JavaScript. A restauração da última aba via `localStorage` será removida para que toda abertura ou recarga comece no resumo.

Os três indicadores existentes serão movidos para dentro da aba Resumo e deixarão de aparecer nos demais módulos.

O conteúdo terá:

1. faixa superior com os três indicadores;
2. card principal com o gráfico de pizza e legenda;
3. card com os últimos cinco clientes;
4. card com os últimos cinco leads.

Em telas largas, o gráfico receberá maior destaque e as listas serão organizadas em duas colunas. Em telas estreitas, os cards ficarão empilhados.

O gráfico será renderizado com Chart.js 4.4.7, carregado pelo mesmo modelo CDN já usado pelo painel. A legenda textual mostrará entradas como `R$ 250,00 — 4 clientes`. O canvas terá descrição acessível e o estado vazio será apresentado em texto quando a distribuição não tiver itens.

Os dados textuais vindos da API serão inseridos com APIs seguras de DOM, sem interpolação direta de HTML.

## Fluxo de dados

Na inicialização do painel, o frontend chama `GET /resumo` e preenche os indicadores, o gráfico e as listas. Os carregamentos já existentes dos demais módulos continuam independentes.

Depois de criar, editar ou excluir um cliente ou lead, o frontend recarrega o resumo:

- criar altera as listas recentes e pode alterar indicadores e gráfico;
- editar não altera a ordem de criação, mas pode alterar valores, status e textos exibidos;
- excluir pode alterar qualquer bloco relacionado ao tipo removido.

A instância anterior do gráfico será destruída antes de uma nova renderização para impedir sobreposição e vazamento de recursos.

## Tratamento de falhas

- Falhas do endpoint seguem o tratamento HTTP global existente.
- Uma falha ao carregar o resumo produz uma mensagem dentro da aba e não impede o uso dos outros módulos.
- Um bloco sem registros apresenta um estado vazio específico.
- A ausência de Chart.js produz uma mensagem alternativa no card do gráfico e preserva os indicadores e listas.
- Dados monetários nulos legados são tratados como zero.

## Testes e validação

A implementação seguirá ciclos de teste primeiro.

O backend terá testes para:

- cálculo dos três indicadores;
- agrupamento por valor exato;
- exclusão de clientes inativos do gráfico e da receita;
- inclusão de clientes ativos com valor zero;
- ordenação crescente das fatias;
- limite de cinco clientes e cinco leads;
- ordenação por `created_at`, sem influência de `updated_at`;
- preservação de `created_at` após edição;
- contrato de resposta de `GET /resumo`;
- migração e preenchimento das novas colunas.

O frontend terá testes das funções puras do novo código de resumo com o executor nativo `node --test` e verificação da estrutura HTML pela suíte Java para:

- resumo como aba inicial;
- formatação das legendas;
- estados com dados, vazios e de erro;
- atualização segura das listas.

A conclusão exige:

- suíte Maven completa aprovada;
- testes de frontend aprovados;
- inspeção visual em largura desktop e móvel;
- confirmação de que os módulos existentes continuam navegáveis e operacionais.

## Critérios de aceite

1. Abrir ou recarregar o painel sempre mostra a aba Resumo.
2. Os três indicadores exibem os valores definidos neste documento.
3. O gráfico possui uma fatia por valor mensal exato e considera apenas clientes ativos.
4. A legenda informa valor e quantidade de clientes de cada fatia.
5. As listas exibem no máximo os cinco cadastros mais recentes pela criação original.
6. Editar cliente ou lead não altera sua posição por data de criação.
7. O resumo se atualiza após mutações de clientes e leads.
8. O layout funciona em telas desktop e móveis.
9. Falhas ou ausência de dados são comunicadas sem bloquear os demais módulos.
