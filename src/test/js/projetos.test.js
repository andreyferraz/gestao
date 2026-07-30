const assert = require("node:assert/strict");
const test = require("node:test");
const GestaoProjetos = require("../../main/resources/static/js/projetos.js");

class FakeElement {
    constructor(id) {
        this.id = id || "";
        this.textContent = "";
        this.children = [];
        this.hidden = false;
        this.disabled = false;
        this.value = "";
        this.required = false;
        this.files = [];
        this.className = "";
        this.type = "";
        this.dataset = {};
        this.attributes = {};
        this.listeners = {};
        this.scrollOptions = null;
    }

    replaceChildren() {
        this.children = [];
    }

    appendChild(child) {
        this.children.push(child);
        return child;
    }

    setAttribute(name, value) {
        this.attributes[name] = String(value);
    }

    getAttribute(name) {
        return Object.prototype.hasOwnProperty.call(this.attributes, name)
            ? this.attributes[name] : null;
    }

    addEventListener(name, listener) {
        this.listeners[name] = listener;
    }

    reset() {
        this.value = "";
    }

    scrollIntoView(options) {
        this.scrollOptions = options;
    }
}

class FakeFormData {
    constructor() {
        this.valores = [];
    }

    append(name, value) {
        this.valores.push([name, value]);
    }
}

function respostaJson(status, payload) {
    return {
        ok: status >= 200 && status < 300,
        status: status,
        json: async function () { return payload; }
    };
}

function respostaSemConteudo(status) {
    return {
        ok: status >= 200 && status < 300,
        status: status,
        json: async function () {
            throw new Error("Resposta sem conteúdo");
        }
    };
}

function criarAmbiente(opcoes) {
    const config = opcoes || {};
    const ids = [
        "projeto-form",
        "projeto-titulo",
        "projeto-descricao",
        "projeto-link",
        "projeto-imagem",
        "projeto-salvar",
        "projeto-cancelar",
        "projeto-modo",
        "projetos-lista",
        "projeto-feedback"
    ];
    const elementos = Object.fromEntries(ids.map(function (id) {
        return [id, new FakeElement(id)];
    }));
    elementos["projeto-imagem"].required = true;
    elementos["projeto-cancelar"].hidden = true;
    elementos["projeto-salvar"].textContent = "Salvar projeto";
    elementos["projeto-form"].reset = function () {
        elementos["projeto-titulo"].value = "";
        elementos["projeto-descricao"].value = "";
        elementos["projeto-link"].value = "";
        elementos["projeto-imagem"].value = "";
        elementos["projeto-imagem"].files = [];
    };

    const metaValores = {
        "app-context-path": "/gestao/",
        "_csrf": "csrf-123",
        "_csrf_header": "X-CSRF-TOKEN"
    };
    const respostas = (config.respostas || []).slice();
    const requisicoes = [];
    const confirmacoes = [];
    const document = {
        getElementById: function (id) {
            return elementos[id] || null;
        },
        createElement: function () {
            return new FakeElement();
        },
        querySelector: function (selector) {
            const correspondencia = selector.match(/^meta\[name="(.+)"\]$/);
            if (!correspondencia || !(correspondencia[1] in metaValores)) {
                return null;
            }
            const meta = new FakeElement();
            meta.setAttribute("content", metaValores[correspondencia[1]]);
            return meta;
        }
    };

    return {
        elementos: elementos,
        requisicoes: requisicoes,
        confirmacoes: confirmacoes,
        document: document,
        FormData: FakeFormData,
        confirm: function (mensagem) {
            confirmacoes.push(mensagem);
            return config.confirmar !== false;
        },
        fetch: async function (url, options) {
            requisicoes.push({ url: url, options: options });
            if (respostas.length === 0) {
                throw new Error("Nenhuma resposta preparada.");
            }
            return respostas.shift();
        }
    };
}

function criarPainel(ambiente) {
    return GestaoProjetos.criarPainel({
        document: ambiente.document,
        fetch: ambiente.fetch,
        confirm: ambiente.confirm,
        FormData: ambiente.FormData
    });
}

function projetoCompleto() {
    return {
        id: "p1",
        titulo: "Projeto Um",
        descricao: "Descrição",
        imagemUrl: "https://api.example/imagem.webp",
        link: "https://example.com"
    };
}

function preencherCampos(ambiente, opcoes) {
    ambiente.elementos["projeto-titulo"].value = "Projeto Um";
    ambiente.elementos["projeto-descricao"].value = "Descrição";
    ambiente.elementos["projeto-link"].value = "https://example.com";
    ambiente.elementos["projeto-imagem"].files = opcoes && opcoes.imagem
        ? [opcoes.imagem] : [];
}

test("renderizar mostra somente título e ações de cada projeto", function () {
    const ambiente = criarAmbiente();
    const painel = criarPainel(ambiente);

    painel.renderizar([{
        id: "p1",
        titulo: "<img src=x onerror=alert(1)>",
        descricao: "Não deve aparecer",
        imagemUrl: "https://api/imagem.webp",
        link: "https://example.com"
    }]);

    const item = ambiente.elementos["projetos-lista"].children[0];
    assert.equal(item.children.length, 2);
    assert.equal(item.children[0].textContent, "<img src=x onerror=alert(1)>");
    assert.equal(item.children[1].children[0].textContent, "Editar");
    assert.equal(item.children[1].children[1].textContent, "Excluir");
    assert.equal(item.children[1].children.length, 2);
    assert.equal(
        item.children[1].children[0].getAttribute("aria-label"),
        "Editar projeto <img src=x onerror=alert(1)>");
});

test("renderizar informa quando não existem projetos", function () {
    const ambiente = criarAmbiente();
    const painel = criarPainel(ambiente);

    painel.renderizar([]);

    assert.equal(ambiente.elementos["projetos-lista"].children.length, 1);
    assert.equal(
        ambiente.elementos["projetos-lista"].children[0].textContent,
        "Nenhum projeto cadastrado.");
});

test("editar preenche dados e torna imagem opcional", function () {
    const ambiente = criarAmbiente();
    const painel = criarPainel(ambiente);
    painel.renderizar([projetoCompleto()]);

    painel.editar("p1");

    assert.equal(ambiente.elementos["projeto-titulo"].value, "Projeto Um");
    assert.equal(ambiente.elementos["projeto-descricao"].value, "Descrição");
    assert.equal(ambiente.elementos["projeto-link"].value, "https://example.com");
    assert.equal(ambiente.elementos["projeto-imagem"].required, false);
    assert.equal(
        ambiente.elementos["projeto-salvar"].textContent,
        "Atualizar projeto");
    assert.equal(ambiente.elementos["projeto-cancelar"].hidden, false);
    assert.equal(painel.obterEstado().projetoEmEdicaoId, "p1");
});

test("cancelar edição limpa formulário e restaura modo de cadastro", function () {
    const ambiente = criarAmbiente();
    const painel = criarPainel(ambiente);
    painel.renderizar([projetoCompleto()]);
    painel.editar("p1");

    painel.cancelarEdicao();

    assert.equal(ambiente.elementos["projeto-titulo"].value, "");
    assert.equal(ambiente.elementos["projeto-imagem"].required, true);
    assert.equal(
        ambiente.elementos["projeto-salvar"].textContent,
        "Salvar projeto");
    assert.equal(ambiente.elementos["projeto-cancelar"].hidden, true);
    assert.equal(painel.obterEstado().projetoEmEdicaoId, null);
});

test("inicializar ignora páginas sem o painel de projetos", function () {
    const ambiente = criarAmbiente();
    ambiente.document.getElementById = function () { return null; };

    const painel = GestaoProjetos.inicializar({
        document: ambiente.document,
        fetch: ambiente.fetch,
        confirm: ambiente.confirm,
        FormData: ambiente.FormData
    });

    assert.equal(painel, null);
    assert.equal(ambiente.requisicoes.length, 0);
});

test("carregar busca projetos usando o contexto da aplicação", async function () {
    const ambiente = criarAmbiente({
        respostas: [respostaJson(200, [projetoCompleto()])]
    });
    const painel = criarPainel(ambiente);

    await painel.carregar();

    assert.equal(ambiente.requisicoes[0].url, "/gestao/api/projetos");
    assert.equal(ambiente.requisicoes[0].options.method, "GET");
    assert.equal(ambiente.elementos["projetos-lista"].children.length, 1);
});

test("carregar informa falha de rede sem alterar outras áreas", async function () {
    const ambiente = criarAmbiente();
    const painel = criarPainel(ambiente);

    await assert.rejects(function () {
        return painel.carregar();
    });

    assert.equal(
        ambiente.elementos["projeto-feedback"].textContent,
        "Não foi possível carregar os projetos.");
});

test("salvar valida os campos textuais antes de enviar", async function () {
    const cenarios = [
        {
            alterar: function (elementos) {
                elementos["projeto-titulo"].value = " ";
            },
            mensagem: "Informe o título do projeto."
        },
        {
            alterar: function (elementos) {
                elementos["projeto-descricao"].value = " ";
            },
            mensagem: "Informe a descrição do projeto."
        },
        {
            alterar: function (elementos) {
                elementos["projeto-link"].value = "javascript:alert(1)";
            },
            mensagem: "Informe um link iniciado por http:// ou https://."
        }
    ];

    for (const cenario of cenarios) {
        const ambiente = criarAmbiente();
        preencherCampos(ambiente, { imagem: { name: "capa.png" } });
        cenario.alterar(ambiente.elementos);
        const painel = criarPainel(ambiente);

        await painel.salvar({ preventDefault: function () {} });

        assert.equal(ambiente.requisicoes.length, 0);
        assert.equal(
            ambiente.elementos["projeto-feedback"].textContent,
            cenario.mensagem);
    }
});

test("salvar exige imagem ao cadastrar sem enviar requisição", async function () {
    const ambiente = criarAmbiente();
    preencherCampos(ambiente, { imagem: null });
    const painel = criarPainel(ambiente);

    await painel.salvar({ preventDefault: function () {} });

    assert.equal(ambiente.requisicoes.length, 0);
    assert.equal(
        ambiente.elementos["projeto-feedback"].textContent,
        "Selecione uma imagem para cadastrar o projeto.");
});

test("salvar cadastra multipart com CSRF e sem Content-Type manual", async function () {
    const ambiente = criarAmbiente({
        respostas: [
            respostaJson(201, projetoCompleto()),
            respostaJson(200, [projetoCompleto()])
        ]
    });
    preencherCampos(ambiente, { imagem: { name: "capa.png" } });
    const painel = criarPainel(ambiente);

    await painel.salvar({ preventDefault: function () {} });

    const requisicao = ambiente.requisicoes[0];
    assert.equal(requisicao.url, "/gestao/api/projetos");
    assert.equal(requisicao.options.method, "POST");
    assert.equal(requisicao.options.headers["X-CSRF-TOKEN"], "csrf-123");
    assert.equal("Content-Type" in requisicao.options.headers, false);
    assert.deepEqual(requisicao.options.body.valores, [
        ["titulo", "Projeto Um"],
        ["descricao", "Descrição"],
        ["link", "https://example.com"],
        ["imagem", { name: "capa.png" }]
    ]);
    assert.equal(ambiente.elementos["projeto-titulo"].value, "");
});

test("salvar edição usa PUT e não envia imagem quando nenhuma nova foi escolhida",
        async function () {
    const ambiente = criarAmbiente({
        respostas: [
            respostaJson(200, projetoCompleto()),
            respostaJson(200, [projetoCompleto()])
        ]
    });
    const painel = criarPainel(ambiente);
    painel.renderizar([projetoCompleto()]);
    painel.editar("p1");

    await painel.salvar({ preventDefault: function () {} });

    const requisicao = ambiente.requisicoes[0];
    assert.equal(requisicao.url, "/gestao/api/projetos/p1");
    assert.equal(requisicao.options.method, "PUT");
    assert.equal(
        requisicao.options.body.valores.some(function (par) {
            return par[0] === "imagem";
        }),
        false);
});

test("excluir cancelado pede confirmação e não chama a API", async function () {
    const ambiente = criarAmbiente({ confirmar: false });
    const painel = criarPainel(ambiente);
    painel.renderizar([projetoCompleto()]);

    const resultado = await painel.excluir("p1");

    assert.equal(resultado, false);
    assert.deepEqual(
        ambiente.confirmacoes,
        ['Excluir o projeto "Projeto Um"?']);
    assert.equal(ambiente.requisicoes.length, 0);
});

test("excluir confirmado envia DELETE com CSRF e atualiza a lista", async function () {
    const ambiente = criarAmbiente({
        confirmar: true,
        respostas: [
            respostaSemConteudo(204),
            respostaJson(200, [])
        ]
    });
    const painel = criarPainel(ambiente);
    painel.renderizar([projetoCompleto()]);

    const resultado = await painel.excluir("p1");

    assert.equal(resultado, true);
    assert.equal(ambiente.requisicoes[0].url, "/gestao/api/projetos/p1");
    assert.equal(ambiente.requisicoes[0].options.method, "DELETE");
    assert.equal(
        ambiente.requisicoes[0].options.headers["X-CSRF-TOKEN"],
        "csrf-123");
    assert.equal(
        ambiente.elementos["projetos-lista"].children[0].textContent,
        "Nenhum projeto cadastrado.");
});

test("excluir projeto em edição restaura o modo de cadastro", async function () {
    const ambiente = criarAmbiente({
        confirmar: true,
        respostas: [
            respostaSemConteudo(204),
            respostaJson(200, [])
        ]
    });
    const painel = criarPainel(ambiente);
    painel.renderizar([projetoCompleto()]);
    painel.editar("p1");

    await painel.excluir("p1");

    assert.equal(painel.obterEstado().projetoEmEdicaoId, null);
    assert.equal(ambiente.elementos["projeto-imagem"].required, true);
    assert.equal(
        ambiente.elementos["projeto-salvar"].textContent,
        "Salvar projeto");
});

test("falha da API mantém os dados e apresenta a mensagem retornada", async function () {
    const ambiente = criarAmbiente({
        respostas: [respostaJson(400, {
            message: "Título do projeto é obrigatório."
        })]
    });
    preencherCampos(ambiente, { imagem: { name: "capa.png" } });
    const painel = criarPainel(ambiente);

    await painel.salvar({ preventDefault: function () {} });

    assert.equal(
        ambiente.elementos["projeto-titulo"].value,
        "Projeto Um");
    assert.equal(
        ambiente.elementos["projeto-feedback"].textContent,
        "Título do projeto é obrigatório.");
});

test("inicializar liga os eventos e inicia o carregamento da lista", function () {
    const ambiente = criarAmbiente({
        respostas: [respostaJson(200, [])]
    });

    const painel = GestaoProjetos.inicializar({
        document: ambiente.document,
        fetch: ambiente.fetch,
        confirm: ambiente.confirm,
        FormData: ambiente.FormData
    });

    assert.notEqual(painel, null);
    assert.equal(
        typeof ambiente.elementos["projeto-form"].listeners.submit,
        "function");
    assert.equal(
        typeof ambiente.elementos["projeto-cancelar"].listeners.click,
        "function");
    assert.equal(ambiente.requisicoes[0].url, "/gestao/api/projetos");
});
