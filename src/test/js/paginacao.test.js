const assert = require("node:assert/strict");
const test = require("node:test");
const GestaoPaginacao = require("../../main/resources/static/js/paginacao.js");

class FakeNode {
    constructor(text) {
        this.nodeValue = text || "";
        this.textContent = text || "";
    }
}

class FakeElement {
    constructor(tagName) {
        this.tagName = (tagName || "div").toUpperCase();
        this.className = "";
        this.textContent = "";
        this.children = [];
        this.attributes = {};
        this.listeners = {};
        this.hidden = false;
        this.disabled = false;
    }

    setAttribute(name, value) {
        this.attributes[name] = String(value);
    }

    getAttribute(name) {
        return Object.prototype.hasOwnProperty.call(this.attributes, name) ? this.attributes[name] : null;
    }

    addEventListener(event, callback) {
        this.listeners[event] = callback;
    }

    appendChild(child) {
        this.children.push(child);
        return child;
    }

    replaceChildren() {
        this.children = [];
    }

    click() {
        if (this.disabled) {
            return;
        }
        if (this.listeners.click) {
            this.listeners.click();
        }
    }
}

const fakeDocument = {
    createElement(tag) {
        return new FakeElement(tag);
    },
    createTextNode(text) {
        return new FakeNode(text);
    }
};

test("calcularTotalPaginas respeita 5 itens por página", function () {
    assert.equal(GestaoPaginacao.calcularTotalPaginas(0, 5), 1);
    assert.equal(GestaoPaginacao.calcularTotalPaginas(1, 5), 1);
    assert.equal(GestaoPaginacao.calcularTotalPaginas(5, 5), 1);
    assert.equal(GestaoPaginacao.calcularTotalPaginas(6, 5), 2);
    assert.equal(GestaoPaginacao.calcularTotalPaginas(10, 5), 2);
    assert.equal(GestaoPaginacao.calcularTotalPaginas(11, 5), 3);
});

test("ajustarPagina restringe páginas aos limites válidos", function () {
    assert.equal(GestaoPaginacao.ajustarPagina(0, 3), 1);
    assert.equal(GestaoPaginacao.ajustarPagina(-5, 3), 1);
    assert.equal(GestaoPaginacao.ajustarPagina(2, 3), 2);
    assert.equal(GestaoPaginacao.ajustarPagina(4, 3), 3);
    assert.equal(GestaoPaginacao.ajustarPagina(99, 3), 3);
});

test("obterItensPagina fatia 5 itens por página", function () {
    const itens = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

    const pag1 = GestaoPaginacao.obterItensPagina(itens, 1, 5);
    assert.deepEqual(pag1, [1, 2, 3, 4, 5]);

    const pag2 = GestaoPaginacao.obterItensPagina(itens, 2, 5);
    assert.deepEqual(pag2, [6, 7, 8, 9, 10]);

    const pag3 = GestaoPaginacao.obterItensPagina(itens, 3, 5);
    assert.deepEqual(pag3, [11, 12]);
});

test("calcularJanelaPaginas cria reticências adequadas", function () {
    assert.deepEqual(GestaoPaginacao.calcularJanelaPaginas(1, 5), [1, 2, 3, 4, 5]);
    assert.deepEqual(GestaoPaginacao.calcularJanelaPaginas(2, 8), [1, 2, 3, 4, 5, "...", 8]);
    assert.deepEqual(GestaoPaginacao.calcularJanelaPaginas(5, 10), [1, "...", 4, 5, 6, "...", 10]);
    assert.deepEqual(GestaoPaginacao.calcularJanelaPaginas(8, 8), [1, "...", 4, 5, 6, 7, 8]);
});

test("renderizarControles esconde quando há 5 ou menos itens", function () {
    const container = new FakeElement("nav");
    GestaoPaginacao.renderizarControles(container, {
        totalItens: 4,
        paginaAtual: 1,
        itensPorPagina: 5,
        document: fakeDocument
    });

    assert.equal(container.hidden, true);
    assert.equal(container.children.length, 0);
});

test("renderizarControles exibe controles quando há mais de 5 itens e reage a cliques", function () {
    const container = new FakeElement("nav");
    let paginaClicada = null;

    GestaoPaginacao.renderizarControles(container, {
        totalItens: 12,
        paginaAtual: 1,
        itensPorPagina: 5,
        document: fakeDocument,
        onMudarPagina: function (novaPagina) {
            paginaClicada = novaPagina;
        }
    });

    assert.equal(container.hidden, false);
    assert.equal(container.children.length, 2); // info e acoes

    const info = container.children[0];
    assert.equal(info.className, "paginacao-resumo");

    const acoes = container.children[1];
    assert.equal(acoes.className, "paginacao-acoes");

    const btnAnterior = acoes.children[0];
    assert.equal(btnAnterior.disabled, true);

    const btnPagina2 = acoes.children[2];
    assert.equal(btnPagina2.textContent, "2");
    btnPagina2.click();
    assert.equal(paginaClicada, 2);

    const btnProximo = acoes.children[acoes.children.length - 1];
    assert.equal(btnProximo.disabled, false);
    btnProximo.click();
    assert.equal(paginaClicada, 2);
});

test("simulação aba clientes: 7 clientes distribui 5 na primeira e 2 na segunda página", function () {
    const clientes = [
        { id: 1, nome: "Cliente A" },
        { id: 2, nome: "Cliente B" },
        { id: 3, nome: "Cliente C" },
        { id: 4, nome: "Cliente D" },
        { id: 5, nome: "Cliente E" },
        { id: 6, nome: "Cliente F" },
        { id: 7, nome: "Cliente G" }
    ];

    const totalPaginas = GestaoPaginacao.calcularTotalPaginas(clientes.length, 5);
    assert.equal(totalPaginas, 2);

    const pagina1 = GestaoPaginacao.obterItensPagina(clientes, 1, 5);
    assert.equal(pagina1.length, 5);
    assert.equal(pagina1[0].nome, "Cliente A");
    assert.equal(pagina1[4].nome, "Cliente E");

    const pagina2 = GestaoPaginacao.obterItensPagina(clientes, 2, 5);
    assert.equal(pagina2.length, 2);
    assert.equal(pagina2[0].nome, "Cliente F");
    assert.equal(pagina2[1].nome, "Cliente G");
});

test("simulação aba leads: busca que reduz resultados para <= 5 esconde paginação", function () {
    const todosLeads = Array.from({ length: 9 }, function (_, i) {
        return { id: i + 1, nome: "Lead " + (i + 1) };
    });

    const container = new FakeElement("nav");
    GestaoPaginacao.renderizarControles(container, {
        totalItens: todosLeads.length,
        paginaAtual: 1,
        itensPorPagina: 5,
        document: fakeDocument
    });
    assert.equal(container.hidden, false);

    // Usuário filtra por nome e sobram apenas 3 leads
    const leadsFiltrados = todosLeads.slice(0, 3);
    GestaoPaginacao.renderizarControles(container, {
        totalItens: leadsFiltrados.length,
        paginaAtual: 1,
        itensPorPagina: 5,
        document: fakeDocument
    });
    assert.equal(container.hidden, true);
});

test("simulação aba chamados: remoção de item na última página recua página atual", function () {
    let chamados = Array.from({ length: 6 }, function (_, i) {
        return { id: i + 1, titulo: "Chamado " + (i + 1) };
    });

    let paginaAtual = 2;
    let totalPaginas = GestaoPaginacao.calcularTotalPaginas(chamados.length, 5);
    assert.equal(totalPaginas, 2);

    // Usuário resolve e remove o 6º chamado
    chamados = chamados.filter(function (c) { return c.id !== 6; });
    assert.equal(chamados.length, 5);

    totalPaginas = GestaoPaginacao.calcularTotalPaginas(chamados.length, 5);
    assert.equal(totalPaginas, 1);

    paginaAtual = GestaoPaginacao.ajustarPagina(paginaAtual, totalPaginas);
    assert.equal(paginaAtual, 1);

    const itensPagina = GestaoPaginacao.obterItensPagina(chamados, paginaAtual, 5);
    assert.equal(itensPagina.length, 5);
});
