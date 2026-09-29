const assert = require("node:assert/strict");
const test = require("node:test");

const NIVEIS_FECHAMENTO = {
    MUITO_DISTANTE: {
        chave: "MUITO_DISTANTE",
        classe: "lead-nivel-muito-distante",
        badgeClasse: "lead-badge-muito-distante",
        corClasse: "lead-cor-muito-distante",
        label: "Muito distante",
        cor: "#ef4444"
    },
    POUCO_PROVAVEL: {
        chave: "POUCO_PROVAVEL",
        classe: "lead-nivel-pouco-provavel",
        badgeClasse: "lead-badge-pouco-provavel",
        corClasse: "lead-cor-pouco-provavel",
        label: "Pouco provável",
        cor: "#f97316"
    },
    EM_NEGOCIACAO: {
        chave: "EM_NEGOCIACAO",
        classe: "lead-nivel-em-negociacao",
        badgeClasse: "lead-badge-em-negociacao",
        corClasse: "lead-cor-em-negociacao",
        label: "Em negociação",
        cor: "#eab308"
    },
    MUITO_PROVAVEL: {
        chave: "MUITO_PROVAVEL",
        classe: "lead-nivel-muito-provavel",
        badgeClasse: "lead-badge-muito-provavel",
        corClasse: "lead-cor-muito-provavel",
        label: "Muito provável",
        cor: "#3b82f6"
    },
    CERTEZA_100: {
        chave: "CERTEZA_100",
        classe: "lead-nivel-certeza-100",
        badgeClasse: "lead-badge-certeza-100",
        corClasse: "lead-cor-certeza-100",
        label: "100% de certeza",
        cor: "#22c55e"
    }
};

const obterConfigNivelLead = function (nivel) {
    if (!nivel) {
        return null;
    }
    return NIVEIS_FECHAMENTO[nivel] || null;
};

const normalizarLead = function (lead) {
    return {
        id: lead.id,
        nome: lead.nome || "Sem nome",
        telefone: lead.telefone || "Nao informado",
        observacoes: lead.observacoes || "",
        orcamentoDesenvolvimento: Number(lead.orcamentoDesenvolvimento) || 0,
        orcamentoManutencaoHospedagem: Number(lead.orcamentoManutencaoHospedagem) || 0,
        nivelFechamento: lead.nivelFechamento || ""
    };
};

test("normalizarLead preserva nivelFechamento quando presente e define vazio quando ausente", function () {
    const comNivel = normalizarLead({ id: "1", nome: "Lead 1", nivelFechamento: "MUITO_PROVAVEL" });
    assert.equal(comNivel.nivelFechamento, "MUITO_PROVAVEL");

    const semNivel = normalizarLead({ id: "2", nome: "Lead 2" });
    assert.equal(semNivel.nivelFechamento, "");
});

test("obterConfigNivelLead mapeia corretamente os 5 níveis da escala e suas classes de cores", function () {
    assert.equal(obterConfigNivelLead(null), null);
    assert.equal(obterConfigNivelLead(""), null);

    const mDistante = obterConfigNivelLead("MUITO_DISTANTE");
    assert.ok(mDistante);
    assert.equal(mDistante.label, "Muito distante");
    assert.equal(mDistante.classe, "lead-nivel-muito-distante");
    assert.equal(mDistante.cor, "#ef4444");

    const pProvavel = obterConfigNivelLead("POUCO_PROVAVEL");
    assert.ok(pProvavel);
    assert.equal(pProvavel.label, "Pouco provável");
    assert.equal(pProvavel.classe, "lead-nivel-pouco-provavel");
    assert.equal(pProvavel.cor, "#f97316");

    const negociacao = obterConfigNivelLead("EM_NEGOCIACAO");
    assert.ok(negociacao);
    assert.equal(negociacao.label, "Em negociação");
    assert.equal(negociacao.classe, "lead-nivel-em-negociacao");
    assert.equal(negociacao.cor, "#eab308");

    const mProvavel = obterConfigNivelLead("MUITO_PROVAVEL");
    assert.ok(mProvavel);
    assert.equal(mProvavel.label, "Muito provável");
    assert.equal(mProvavel.classe, "lead-nivel-muito-provavel");
    assert.equal(mProvavel.cor, "#3b82f6");

    const certeza100 = obterConfigNivelLead("CERTEZA_100");
    assert.ok(certeza100);
    assert.equal(certeza100.label, "100% de certeza");
    assert.equal(certeza100.classe, "lead-nivel-certeza-100");
    assert.equal(certeza100.cor, "#22c55e");
});

test("filtro por nível de fechamento seleciona apenas leads com o nível desejado", function () {
    const leads = [
        { id: "1", nome: "Lead Vermelho", nivelFechamento: "MUITO_DISTANTE" },
        { id: "2", nome: "Lead Laranja", nivelFechamento: "POUCO_PROVAVEL" },
        { id: "3", nome: "Lead Amarelo", nivelFechamento: "EM_NEGOCIACAO" },
        { id: "4", nome: "Lead Azul", nivelFechamento: "MUITO_PROVAVEL" },
        { id: "5", nome: "Lead Verde", nivelFechamento: "CERTEZA_100" },
        { id: "6", nome: "Lead Neutro", nivelFechamento: "" }
    ];

    const filtradosVerde = leads.filter(l => l.nivelFechamento === "CERTEZA_100");
    assert.equal(filtradosVerde.length, 1);
    assert.equal(filtradosVerde[0].nome, "Lead Verde");

    const filtradosVermelho = leads.filter(l => l.nivelFechamento === "MUITO_DISTANTE");
    assert.equal(filtradosVermelho.length, 1);
    assert.equal(filtradosVermelho[0].nome, "Lead Vermelho");
});
