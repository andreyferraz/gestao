const assert = require("node:assert/strict");
const test = require("node:test");
const GestaoResumo = require("../../main/resources/static/js/resumo.js");

class FakeElement {
    constructor() {
        this.textContent = "";
        this.hidden = false;
        this.children = [];
        this.className = "";
    }

    replaceChildren() {
        this.children = [];
    }

    appendChild(child) {
        this.children.push(child);
        return child;
    }

    getContext() {
        return {};
    }
}

function criarAmbiente() {
    const ids = [
        "kpi-clientes", "kpi-receita", "kpi-dominios", "resumo-feedback",
        "resumo-grafico", "resumo-grafico-estado", "resumo-grafico-legenda",
        "resumo-grafico-cidades", "resumo-grafico-cidades-estado", "resumo-grafico-cidades-legenda",
        "resumo-clientes-lista", "resumo-leads-lista"
    ];
    const elementos = Object.fromEntries(ids.map(function (id) {
        return [id, new FakeElement()];
    }));
    const graficos = [];

    class FakeChart {
        constructor(context, config) {
            this.context = context;
            this.config = config;
            this.destroyed = false;
            graficos.push(this);
        }

        destroy() {
            this.destroyed = true;
        }
    }

    return {
        elementos: elementos,
        graficos: graficos,
        FakeChart: FakeChart,
        document: {
            getElementById: function (id) { return elementos[id]; },
            createElement: function () { return new FakeElement(); }
        }
    };
}

function criarPainelComPayload(ambiente, payload) {
    return GestaoResumo.criarPainel({
        document: ambiente.document,
        Chart: ambiente.FakeChart,
        buscarJson: async function () { return payload; },
        formatarMoeda: function (value) {
            return "R$ " + Number(value).toFixed(2);
        },
        formatarDataHora: function () {
            return "29/07/2026 10:00";
        }
    });
}

function payloadComUmaFatia() {
    return {
        indicadores: {
            totalClientes: 1,
            receitaMensalAtiva: 100,
            dominiosAtivos: 1
        },
        distribuicaoValoresMensais: [
            { valorMensal: 100, quantidadeClientes: 1 }
        ],
        ultimosClientes: [],
        ultimosLeads: []
    };
}

test("carregar renderiza indicadores, listas seguras e gráfico", async function () {
    const ambiente = criarAmbiente();
    const payload = {
        indicadores: {
            totalClientes: 3,
            receitaMensalAtiva: 350,
            dominiosAtivos: 2
        },
        distribuicaoValoresMensais: [
            { valorMensal: 100, quantidadeClientes: 2 }
        ],
        ultimosClientes: [
            {
                id: "c1",
                nome: "<img src=x onerror=alert(1)>",
                createdAt: "2026-07-29T10:00:00Z",
                valorMensal: 100,
                ativo: true
            }
        ],
        ultimosLeads: [
            {
                id: "l1",
                nome: "Lead novo",
                createdAt: "2026-07-29T09:00:00Z",
                orcamentoManutencaoHospedagem: 300
            }
        ]
    };
    const painel = GestaoResumo.criarPainel({
        document: ambiente.document,
        Chart: ambiente.FakeChart,
        buscarJson: async function () { return payload; },
        formatarMoeda: function (value) { return "R$ " + Number(value).toFixed(2); },
        formatarDataHora: function () { return "29/07/2026 10:00"; }
    });

    await painel.carregar();

    assert.equal(ambiente.elementos["kpi-clientes"].textContent, "3");
    assert.equal(ambiente.elementos["kpi-receita"].textContent, "R$ 350.00");
    assert.match(
        ambiente.elementos["resumo-clientes-lista"].children[0].textContent,
        /<img src=x onerror=alert\(1\)>/);
    assert.equal(ambiente.graficos[0].config.data.labels[0], "R$ 100.00");
    assert.equal(
        ambiente.elementos["resumo-grafico-legenda"].children[0].textContent,
        "R$ 100.00 — 2 clientes");
});

test("carregar mostra estados vazios sem instanciar gráfico", async function () {
    const ambiente = criarAmbiente();
    const painel = criarPainelComPayload(ambiente, {
        indicadores: { totalClientes: 0, receitaMensalAtiva: 0, dominiosAtivos: 0 },
        distribuicaoValoresMensais: [],
        ultimosClientes: [],
        ultimosLeads: []
    });

    await painel.carregar();

    assert.equal(ambiente.graficos.length, 0);
    assert.equal(ambiente.elementos["resumo-grafico"].hidden, true);
    assert.equal(
        ambiente.elementos["resumo-grafico-estado"].textContent,
        "Nenhum cliente ativo para exibir.");
    assert.equal(
        ambiente.elementos["resumo-clientes-lista"].children[0].textContent,
        "Nenhum cliente cadastrado.");
});

test("carregar isola falha do endpoint no feedback", async function () {
    const ambiente = criarAmbiente();
    const painel = GestaoResumo.criarPainel({
        document: ambiente.document,
        Chart: ambiente.FakeChart,
        buscarJson: async function () { throw new Error("offline"); },
        formatarMoeda: String,
        formatarDataHora: String
    });

    await painel.carregar();

    assert.equal(
        ambiente.elementos["resumo-feedback"].textContent,
        "Não foi possível carregar o resumo agora.");
});

test("recarregar destroi o gráfico anterior", async function () {
    const ambiente = criarAmbiente();
    const painel = criarPainelComPayload(ambiente, payloadComUmaFatia());

    await painel.carregar();
    await painel.carregar();

    assert.equal(ambiente.graficos[0].destroyed, true);
    assert.equal(ambiente.graficos.length, 2);
});

test("carregar renderiza gráfico de cidades e legenda quando houver cidades cadastradas", async function () {
    const ambiente = criarAmbiente();
    const payload = {
        indicadores: { totalClientes: 3, receitaMensalAtiva: 300, dominiosAtivos: 3 },
        distribuicaoValoresMensais: [],
        distribuicaoCidades: [
            { cidade: "São Paulo", quantidadeClientes: 2 },
            { cidade: "Rio de Janeiro", quantidadeClientes: 1 }
        ],
        ultimosClientes: [],
        ultimosLeads: []
    };
    const painel = criarPainelComPayload(ambiente, payload);

    await painel.carregar();

    assert.equal(ambiente.elementos["resumo-grafico-cidades"].hidden, false);
    assert.equal(ambiente.elementos["resumo-grafico-cidades-estado"].textContent, "");
    assert.equal(ambiente.graficos.length, 1);
    assert.equal(ambiente.graficos[0].config.type, "doughnut");
    assert.deepEqual(ambiente.graficos[0].config.data.labels, ["São Paulo", "Rio de Janeiro"]);
    assert.deepEqual(ambiente.graficos[0].config.data.datasets[0].data, [2, 1]);
    assert.equal(
        ambiente.elementos["resumo-grafico-cidades-legenda"].children[0].textContent,
        "São Paulo — 2 clientes");
    assert.equal(
        ambiente.elementos["resumo-grafico-cidades-legenda"].children[1].textContent,
        "Rio de Janeiro — 1 cliente");
});

test("carregar exibe estado vazio no gráfico de cidades quando não houver cidades cadastradas", async function () {
    const ambiente = criarAmbiente();
    const payload = {
        indicadores: { totalClientes: 2, receitaMensalAtiva: 200, dominiosAtivos: 2 },
        distribuicaoValoresMensais: [],
        distribuicaoCidades: [],
        ultimosClientes: [],
        ultimosLeads: []
    };
    const painel = criarPainelComPayload(ambiente, payload);

    await painel.carregar();

    assert.equal(ambiente.graficos.length, 0);
    assert.equal(ambiente.elementos["resumo-grafico-cidades"].hidden, true);
    assert.equal(
        ambiente.elementos["resumo-grafico-cidades-estado"].textContent,
        "Nenhuma cidade cadastrada para exibir.");
});

test("recarregar destroi o gráfico de cidades anterior", async function () {
    const ambiente = criarAmbiente();
    const payload = {
        indicadores: { totalClientes: 1, receitaMensalAtiva: 100, dominiosAtivos: 1 },
        distribuicaoValoresMensais: [],
        distribuicaoCidades: [
            { cidade: "Curitiba", quantidadeClientes: 1 }
        ],
        ultimosClientes: [],
        ultimosLeads: []
    };
    const painel = criarPainelComPayload(ambiente, payload);

    await painel.carregar();
    await painel.carregar();

    assert.equal(ambiente.graficos.length, 2);
    assert.equal(ambiente.graficos[0].destroyed, true);
});

