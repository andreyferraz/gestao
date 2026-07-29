(function (root, factory) {
    const api = factory();

    if (typeof module === "object" && module.exports) {
        module.exports = api;
        return;
    }

    root.GestaoDashboardAsync = api;
}(typeof window !== "undefined" ? window : globalThis, function () {
    function dispararEmSegundoPlano(tarefa) {
        try {
            return Promise.resolve(tarefa()).catch(function () {
                return undefined;
            });
        } catch (error) {
            return Promise.resolve();
        }
    }

    function executarComTarefaEmSegundoPlano(tarefaEmSegundoPlano, tarefaPrincipal) {
        dispararEmSegundoPlano(tarefaEmSegundoPlano);
        return tarefaPrincipal();
    }

    async function sincronizarListaEResumo(options) {
        const atualizacaoResumo = dispararEmSegundoPlano(options.atualizarResumo);

        try {
            await options.recarregarLista();
            return {
                listaSincronizada: true,
                atualizacaoResumo: atualizacaoResumo
            };
        } catch (error) {
            return {
                listaSincronizada: false,
                erroLista: error,
                atualizacaoResumo: atualizacaoResumo
            };
        }
    }

    return {
        executarComTarefaEmSegundoPlano: executarComTarefaEmSegundoPlano,
        sincronizarListaEResumo: sincronizarListaEResumo
    };
}));
