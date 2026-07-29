const assert = require("node:assert/strict");
const test = require("node:test");

function carregarOrquestrador() {
    try {
        return require("../../main/resources/static/js/dashboard-async.js");
    } catch (error) {
        return {
            executarComTarefaEmSegundoPlano: async function (tarefaEmSegundoPlano, tarefaPrincipal) {
                await tarefaEmSegundoPlano();
                return tarefaPrincipal();
            },
            sincronizarListaEResumo: async function (options) {
                await options.recarregarLista();
                await options.atualizarResumo();
                return { listaSincronizada: true };
            }
        };
    }
}

const GestaoDashboardAsync = carregarOrquestrador();

test("resumo pendente não impede o início dos demais módulos", async function () {
    const resumoPendente = new Promise(function () {});
    let demaisModulosIniciados = false;

    const inicializacao = GestaoDashboardAsync.executarComTarefaEmSegundoPlano(
        function () {
            return resumoPendente;
        },
        function () {
            demaisModulosIniciados = true;
            return Promise.resolve("módulos inicializados");
        });

    assert.equal(demaisModulosIniciados, true);
    assert.equal(await inicializacao, "módulos inicializados");
});

test("falha ao reler lista não impede refresh do resumo", async function () {
    let resumoAtualizado = false;
    let resultado;

    await assert.doesNotReject(async function () {
        resultado = await GestaoDashboardAsync.sincronizarListaEResumo({
            recarregarLista: async function () {
                throw new Error("lista indisponível");
            },
            atualizarResumo: async function () {
                resumoAtualizado = true;
            }
        });
    });

    await resultado.atualizacaoResumo;
    assert.equal(resultado.listaSincronizada, false);
    assert.equal(resumoAtualizado, true);
});
