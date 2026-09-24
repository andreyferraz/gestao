(function (root, factory) {
    const api = factory();

    if (typeof module === "object" && module.exports) {
        module.exports = api;
        return;
    }

    root.GestaoResumo = api;
}(typeof window !== "undefined" ? window : globalThis, function () {
    const PALETA_GRAFICO = [
        "#0f766e", "#0369a1", "#7c3aed", "#ea580c",
        "#dc2626", "#0891b2", "#65a30d", "#ca8a04"
    ];

    function obterElementos(doc) {
        return {
            clientes: doc.getElementById("kpi-clientes"),
            receita: doc.getElementById("kpi-receita"),
            dominios: doc.getElementById("kpi-dominios"),
            feedback: doc.getElementById("resumo-feedback"),
            grafico: doc.getElementById("resumo-grafico"),
            estadoGrafico: doc.getElementById("resumo-grafico-estado"),
            legendaGrafico: doc.getElementById("resumo-grafico-legenda"),
            graficoCidades: doc.getElementById("resumo-grafico-cidades"),
            estadoGraficoCidades: doc.getElementById("resumo-grafico-cidades-estado"),
            legendaGraficoCidades: doc.getElementById("resumo-grafico-cidades-legenda"),
            listaClientes: doc.getElementById("resumo-clientes-lista"),
            listaLeads: doc.getElementById("resumo-leads-lista")
        };
    }

    function criarPainel(options) {
        const doc = options.document;
        const elementos = obterElementos(doc);
        let grafico = null;
        let graficoCidades = null;

        function destruirGrafico() {
            if (grafico && typeof grafico.destroy === "function") {
                grafico.destroy();
            }
            grafico = null;
        }

        function destruirGraficoCidades() {
            if (graficoCidades && typeof graficoCidades.destroy === "function") {
                graficoCidades.destroy();
            }
            graficoCidades = null;
        }

        function destruirTodosGraficos() {
            destruirGrafico();
            destruirGraficoCidades();
        }

        function renderizarLista(container, itens, mensagemVazia, criarItem) {
            container.replaceChildren();
            if (itens.length === 0) {
                const vazio = doc.createElement("li");
                vazio.className = "resumo-vazio";
                vazio.textContent = mensagemVazia;
                container.appendChild(vazio);
                return;
            }

            itens.forEach(function (item) {
                container.appendChild(criarItem(item));
            });
        }

        function renderizarIndicadores(indicadores) {
            elementos.clientes.textContent = String(indicadores.totalClientes || 0);
            elementos.receita.textContent = options.formatarMoeda(indicadores.receitaMensalAtiva || 0);
            elementos.dominios.textContent = String(indicadores.dominiosAtivos || 0);
        }

        function renderizarClientes(clientes) {
            renderizarLista(
                elementos.listaClientes,
                clientes,
                "Nenhum cliente cadastrado.",
                function (cliente) {
                    const item = doc.createElement("li");
                    const nome = cliente.nome || "Cliente sem nome";
                    const situacao = cliente.ativo ? "Ativo" : "Inativo";
                    item.textContent = nome
                        + " — " + options.formatarMoeda(cliente.valorMensal || 0)
                        + " — " + situacao
                        + " — " + options.formatarDataHora(cliente.createdAt || "");
                    return item;
                });
        }

        function renderizarLeads(leads) {
            renderizarLista(
                elementos.listaLeads,
                leads,
                "Nenhum lead cadastrado.",
                function (lead) {
                    const item = doc.createElement("li");
                    const nome = lead.nome || "Lead sem nome";
                    item.textContent = nome
                        + " — " + options.formatarMoeda(lead.orcamentoManutencaoHospedagem || 0)
                        + " — " + options.formatarDataHora(lead.createdAt || "");
                    return item;
                });
        }

        function renderizarLegenda(fatias) {
            elementos.legendaGrafico.replaceChildren();
            fatias.forEach(function (fatia) {
                const item = doc.createElement("li");
                const quantidade = Number(fatia.quantidadeClientes) || 0;
                const sufixo = quantidade === 1 ? "cliente" : "clientes";
                item.textContent = options.formatarMoeda(fatia.valorMensal || 0)
                    + " — " + quantidade + " " + sufixo;
                elementos.legendaGrafico.appendChild(item);
            });
        }

        function renderizarGrafico(fatias) {
            destruirGrafico();
            renderizarLegenda(fatias);

            if (fatias.length === 0) {
                elementos.grafico.hidden = true;
                elementos.estadoGrafico.textContent = "Nenhum cliente ativo para exibir.";
                return;
            }

            if (typeof options.Chart !== "function") {
                elementos.grafico.hidden = true;
                elementos.estadoGrafico.textContent = "Gráfico indisponível no momento.";
                return;
            }

            elementos.grafico.hidden = false;
            elementos.estadoGrafico.textContent = "";
            const cores = fatias.map(function (_, index) {
                return PALETA_GRAFICO[index % PALETA_GRAFICO.length];
            });
            grafico = new options.Chart(elementos.grafico.getContext("2d"), {
                type: "pie",
                data: {
                    labels: fatias.map(function (fatia) {
                        return options.formatarMoeda(fatia.valorMensal || 0);
                    }),
                    datasets: [{
                        data: fatias.map(function (fatia) {
                            return Number(fatia.quantidadeClientes) || 0;
                        }),
                        backgroundColor: cores
                    }]
                },
                options: {
                    plugins: {
                        legend: {
                            display: false
                        }
                    }
                }
            });
        }

        function renderizarLegendaCidades(fatias) {
            if (!elementos.legendaGraficoCidades) {
                return;
            }
            elementos.legendaGraficoCidades.replaceChildren();
            fatias.forEach(function (fatia) {
                const item = doc.createElement("li");
                const quantidade = Number(fatia.quantidadeClientes) || 0;
                const sufixo = quantidade === 1 ? "cliente" : "clientes";
                const cidade = fatia.cidade || "Não informada";
                item.textContent = cidade + " — " + quantidade + " " + sufixo;
                elementos.legendaGraficoCidades.appendChild(item);
            });
        }

        function renderizarGraficoCidades(fatias) {
            destruirGraficoCidades();

            if (!elementos.graficoCidades || !elementos.estadoGraficoCidades) {
                return;
            }

            renderizarLegendaCidades(fatias);

            if (fatias.length === 0) {
                elementos.graficoCidades.hidden = true;
                elementos.estadoGraficoCidades.textContent = "Nenhuma cidade cadastrada para exibir.";
                return;
            }

            if (typeof options.Chart !== "function") {
                elementos.graficoCidades.hidden = true;
                elementos.estadoGraficoCidades.textContent = "Gráfico indisponível no momento.";
                return;
            }

            elementos.graficoCidades.hidden = false;
            elementos.estadoGraficoCidades.textContent = "";
            const cores = fatias.map(function (_, index) {
                return PALETA_GRAFICO[index % PALETA_GRAFICO.length];
            });

            graficoCidades = new options.Chart(elementos.graficoCidades.getContext("2d"), {
                type: "doughnut",
                data: {
                    labels: fatias.map(function (fatia) {
                        return fatia.cidade || "Não informada";
                    }),
                    datasets: [{
                        data: fatias.map(function (fatia) {
                            return Number(fatia.quantidadeClientes) || 0;
                        }),
                        backgroundColor: cores
                    }]
                },
                options: {
                    plugins: {
                        legend: {
                            display: false
                        }
                    }
                }
            });
        }

        async function carregar() {
            elementos.feedback.textContent = "Carregando resumo...";
            try {
                const payload = await options.buscarJson("/resumo");
                renderizarIndicadores(payload.indicadores || {});
                renderizarClientes(Array.isArray(payload.ultimosClientes) ? payload.ultimosClientes : []);
                renderizarLeads(Array.isArray(payload.ultimosLeads) ? payload.ultimosLeads : []);
                renderizarGrafico(Array.isArray(payload.distribuicaoValoresMensais)
                    ? payload.distribuicaoValoresMensais : []);
                renderizarGraficoCidades(Array.isArray(payload.distribuicaoCidades)
                    ? payload.distribuicaoCidades : []);
                elementos.feedback.textContent = "";
            } catch (error) {
                destruirTodosGraficos();
                elementos.feedback.textContent = "Não foi possível carregar o resumo agora.";
            }
        }

        return { carregar: carregar, destruir: destruirTodosGraficos };
    }

    return { criarPainel: criarPainel };
}));
