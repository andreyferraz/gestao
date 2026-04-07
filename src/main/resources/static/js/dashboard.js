window.addEventListener("DOMContentLoaded", function () {
    const clientes = [];

    const leads = [
        {
            id: "l1",
            nome: "Clara Mkt",
            telefone: "(11) 99876-1122",
            observacoes: "Landing page + blog institucional.",
            orcamentoDesenvolvimento: 2800.0,
            orcamentoManutencaoHospedagem: 350.0
        }
    ];

    const elementos = {
        kpiClientes: document.getElementById("kpi-clientes"),
        kpiReceita: document.getElementById("kpi-receita"),
        kpiDominios: document.getElementById("kpi-dominios"),
        navTabs: Array.from(document.querySelectorAll(".nav-tab")),
        tabPanels: Array.from(document.querySelectorAll(".tab-panel")),
        lista: document.getElementById("clientes-lista"),
        detalhe: document.getElementById("cliente-detalhe"),
        detalheFeedback: document.getElementById("detalhe-feedback"),
        editarClienteButton: document.getElementById("editar-cliente"),
        excluirClienteButton: document.getElementById("excluir-cliente"),
        reciboButton: document.getElementById("gerar-recibo"),
        reciboPreview: document.getElementById("recibo-preview"),
        modoMensalBtn: document.getElementById("modo-mensal"),
        modoAnualBtn: document.getElementById("modo-anual"),
        filtroMensal: document.getElementById("filtro-mensal"),
        filtroAnual: document.getElementById("filtro-anual"),
        mesInput: document.getElementById("relatorio-mes"),
        anoMensalInput: document.getElementById("relatorio-ano-mensal"),
        anoAnualInput: document.getElementById("relatorio-ano-anual"),
        gerarMensalBtn: document.getElementById("gerar-relatorio-mensal"),
        gerarAnualBtn: document.getElementById("gerar-relatorio-anual"),
        reportPreview: document.getElementById("report-preview"),
        cadastroForm: document.getElementById("cadastro-form"),
        cadastroFeedback: document.getElementById("cadastro-feedback"),
        novoNomeInput: document.getElementById("novo-nome"),
        novoContatoInput: document.getElementById("novo-contato"),
        novoDominioInput: document.getElementById("novo-dominio"),
        novoVencimentoInput: document.getElementById("novo-vencimento"),
        novoValorInput: document.getElementById("novo-valor"),
        novoAtivoInput: document.getElementById("novo-ativo"),
        cadastroModo: document.getElementById("cadastro-modo"),
        salvarClienteButton: document.getElementById("salvar-cliente"),
        leadForm: document.getElementById("lead-form"),
        leadNomeInput: document.getElementById("lead-nome"),
        leadTelefoneInput: document.getElementById("lead-telefone"),
        leadOrcDevInput: document.getElementById("lead-orcamento-dev"),
        leadOrcManutencaoInput: document.getElementById("lead-orcamento-manutencao"),
        leadObsInput: document.getElementById("lead-observacoes"),
        leadsLista: document.getElementById("leads-lista")
    };

    if (!elementos.lista || !elementos.detalhe || !elementos.reciboButton || !elementos.reciboPreview) {
        return;
    }

    let clienteSelecionado = null;
    let clienteEmEdicaoId = null;

    const nomesMeses = [
        "Janeiro", "Fevereiro", "Marco", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    ];

    const ativarAba = function (tabId) {
        elementos.navTabs.forEach(function (tab) {
            tab.classList.toggle("active", tab.dataset.tabTarget === tabId);
        });

        elementos.tabPanels.forEach(function (panel) {
            panel.classList.toggle("active", panel.id === tabId);
        });
    };

    const formatarMoeda = function (valor) {
        const numero = Number(valor);
        const valorNormalizado = Number.isFinite(numero) ? numero : 0;
        return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(valorNormalizado);
    };

    const formatarData = function (isoDate) {
        if (!isoDate) {
            return "Nao informado";
        }
        const data = new Date(isoDate + "T00:00:00");
        if (Number.isNaN(data.getTime())) {
            return "Nao informado";
        }
        return new Intl.DateTimeFormat("pt-BR").format(data);
    };

    const normalizarCliente = function (cliente) {
        const ativoRaw = cliente.ativo;
        const ativoNormalizado = ativoRaw === true || ativoRaw === 1 || ativoRaw === "1";

        return {
            id: cliente.id,
            nome: cliente.nome || "Sem nome",
            contato: cliente.contato || "Nao informado",
            dominioAplicacao: cliente.dominioAplicacao || "Nao informado",
            dataVencimentoDominio: cliente.dataVencimentoDominio || "",
            ativo: ativoNormalizado,
            valorMensal: Number(cliente.valorMensal) || 0
        };
    };

    const setCadastroFeedback = function (mensagem, erro) {
        if (!elementos.cadastroFeedback) {
            return;
        }

        elementos.cadastroFeedback.textContent = mensagem;
        elementos.cadastroFeedback.style.color = erro ? "#b91c1c" : "#0f766e";
    };

    const atualizarModoCadastro = function () {
        const emEdicao = Boolean(clienteEmEdicaoId);
        if (elementos.cadastroModo) {
            elementos.cadastroModo.textContent = emEdicao
                ? "Modo atual: edicao de cliente."
                : "Modo atual: novo cadastro.";
        }
        if (elementos.salvarClienteButton) {
            elementos.salvarClienteButton.textContent = emEdicao
                ? "Atualizar Cliente"
                : "Salvar Cliente";
        }
    };

    const preencherFormularioComCliente = function (cliente) {
        if (!cliente || !elementos.novoNomeInput || !elementos.novoContatoInput || !elementos.novoDominioInput || !elementos.novoVencimentoInput || !elementos.novoAtivoInput) {
            return;
        }

        elementos.novoNomeInput.value = cliente.nome || "";
        elementos.novoContatoInput.value = cliente.contato || "";
        elementos.novoDominioInput.value = cliente.dominioAplicacao || "";
        elementos.novoVencimentoInput.value = cliente.dataVencimentoDominio || "";
        if (elementos.novoValorInput) {
            elementos.novoValorInput.value = String(Number(cliente.valorMensal) || 0);
        }
        elementos.novoAtivoInput.value = cliente.ativo ? "true" : "false";
    };

    const setDetalheFeedback = function (mensagem, erro) {
        if (!elementos.detalheFeedback) {
            return;
        }

        elementos.detalheFeedback.textContent = mensagem;
        elementos.detalheFeedback.style.color = erro ? "#b91c1c" : "#0f766e";
    };

    const csrfTokenElement = document.querySelector('meta[name="_csrf"]');
    const csrfHeaderElement = document.querySelector('meta[name="_csrf_header"]');

    const obterHeadersComCsrf = function (headersBase) {
        const headers = Object.assign({}, headersBase || {});
        const token = csrfTokenElement ? csrfTokenElement.getAttribute("content") : "";
        const headerName = csrfHeaderElement ? csrfHeaderElement.getAttribute("content") : "";

        if (token && headerName) {
            headers[headerName] = token;
        }

        return headers;
    };

    const buscarJson = async function (url) {
        const response = await fetch(url, {
            method: "GET",
            headers: {
                Accept: "application/json"
            }
        });

        if (!response.ok) {
            throw new Error("Falha na requisicao: " + url);
        }

        return response.json();
    };

    const obterMensagemErroApi = async function (response, fallbackMessage) {
        try {
            const data = await response.json();
            if (data && typeof data.message === "string" && data.message.trim()) {
                return data.message;
            }
        } catch (error) {
            try {
                const texto = await response.text();
                if (texto && texto.trim()) {
                    return fallbackMessage + " (HTTP " + response.status + ")";
                }
            } catch (textError) {
                return fallbackMessage + " (HTTP " + response.status + ")";
            }
        }
        return fallbackMessage + " (HTTP " + response.status + ")";
    };

    const obterReceitaMensal = function () {
        return clientes.reduce(function (acc, cliente) {
            return acc + cliente.valorMensal;
        }, 0);
    };

    const atualizarKpis = function () {
        const totalClientes = clientes.length;
        const totalReceita = clientes.reduce(function (acc, cliente) {
            return acc + cliente.valorMensal;
        }, 0);
        const dominiosAtivos = clientes.filter(function (cliente) {
            return cliente.ativo;
        }).length;

        elementos.kpiClientes.textContent = String(totalClientes);
        elementos.kpiReceita.textContent = formatarMoeda(totalReceita);
        elementos.kpiDominios.textContent = String(dominiosAtivos);
    };

    const renderDetalhe = function (cliente) {
        if (!cliente) {
            elementos.detalhe.classList.add("empty");
            elementos.detalhe.innerHTML = "<p>Selecione um cliente na lista para ver as informacoes.</p>";
            elementos.reciboButton.disabled = true;
            if (elementos.editarClienteButton) {
                elementos.editarClienteButton.disabled = true;
            }
            if (elementos.excluirClienteButton) {
                elementos.excluirClienteButton.disabled = true;
            }
            return;
        }

        elementos.detalhe.classList.remove("empty");
        elementos.detalhe.innerHTML = ""
            + "<dl>"
            + "<dt>Nome</dt><dd>" + cliente.nome + "</dd>"
            + "<dt>Contato</dt><dd>" + cliente.contato + "</dd>"
            + "<dt>Dominio</dt><dd>" + cliente.dominioAplicacao + "</dd>"
            + "<dt>Vencimento</dt><dd>" + formatarData(cliente.dataVencimentoDominio) + "</dd>"
            + "<dt>Status</dt><dd>" + (cliente.ativo ? "Ativo" : "Inativo") + "</dd>"
            + "<dt>Valor Mensal</dt><dd>" + formatarMoeda(cliente.valorMensal) + "</dd>"
            + "</dl>";

        elementos.reciboButton.disabled = false;
        if (elementos.editarClienteButton) {
            elementos.editarClienteButton.disabled = false;
        }
        if (elementos.excluirClienteButton) {
            elementos.excluirClienteButton.disabled = false;
        }
    };

    const gerarRecibo = function (cliente) {
        if (!cliente) {
            return;
        }

        const dataAtual = new Intl.DateTimeFormat("pt-BR").format(new Date());

        elementos.reciboPreview.innerHTML = ""
            + "<p><strong>RECIBO DE SERVICO</strong></p>"
            + "<p>Recebemos de <strong>" + cliente.nome + "</strong> o valor de <strong>" + formatarMoeda(cliente.valorMensal)
            + "</strong>, referente a servicos de <strong>manutencao e hospedagem</strong>.</p>"
            + "<p>Data de emissao: <strong>" + dataAtual + "</strong></p>"
            + "<p>Dominio vinculado: <strong>" + cliente.dominioAplicacao + "</strong></p>";
    };

    const gerarReciboComMovimentacoes = async function (cliente) {
        if (!cliente || !cliente.id) {
            return;
        }

        const movimentacoes = await buscarJson("/movimentacoes?clienteId=" + encodeURIComponent(cliente.id));
        const listaMovimentacoes = Array.isArray(movimentacoes) ? movimentacoes : [];
        const entradas = listaMovimentacoes.filter(function (mov) {
            return mov && mov.tipo === "ENTRADA";
        });

        const valorRecibo = entradas.reduce(function (acc, mov) {
            return acc + (Number(mov.valor) || 0);
        }, 0);

        const dataAtual = new Intl.DateTimeFormat("pt-BR").format(new Date());
        const totalLancamentos = entradas.length;
        const valorFinal = valorRecibo > 0 ? valorRecibo : (Number(cliente.valorMensal) || 0);

        elementos.reciboPreview.innerHTML = ""
            + "<p><strong>RECIBO DE SERVICO</strong></p>"
            + "<p>Recebemos de <strong>" + cliente.nome + "</strong> o valor de <strong>" + formatarMoeda(valorFinal)
            + "</strong>, referente a servicos de <strong>manutencao e hospedagem</strong>.</p>"
            + "<p>Data de emissao: <strong>" + dataAtual + "</strong></p>"
            + "<p>Dominio vinculado: <strong>" + cliente.dominioAplicacao + "</strong></p>"
            + "<p>Total de entradas encontradas na API financeira: <strong>" + totalLancamentos + "</strong></p>";
    };

    const definirModoRelatorio = function (modo) {
        const mensal = modo === "mensal";
        if (!elementos.modoMensalBtn || !elementos.modoAnualBtn || !elementos.filtroMensal || !elementos.filtroAnual) {
            return;
        }

        elementos.modoMensalBtn.classList.toggle("active", mensal);
        elementos.modoAnualBtn.classList.toggle("active", !mensal);
        elementos.filtroMensal.classList.toggle("active", mensal);
        elementos.filtroAnual.classList.toggle("active", !mensal);
    };

    const gerarRelatorioMensal = function () {
        if (!elementos.reportPreview || !elementos.mesInput || !elementos.anoMensalInput) {
            return;
        }

        const mesNumero = Number(elementos.mesInput.value);
        const ano = Number(elementos.anoMensalInput.value) || new Date().getFullYear();
        const nomeMes = nomesMeses[Math.max(0, Math.min(11, mesNumero - 1))];
        const receitaMensal = obterReceitaMensal();

        const itensClientes = clientes.map(function (cliente) {
            return "<li><strong>" + cliente.nome + "</strong>: " + formatarMoeda(cliente.valorMensal) + "</li>";
        }).join("");

        elementos.reportPreview.innerHTML = ""
            + "<p><strong>Relatorio Mensal - " + nomeMes + "/" + ano + "</strong></p>"
            + "<p>Total previsto de receita: <strong>" + formatarMoeda(receitaMensal) + "</strong></p>"
            + "<p>Base de clientes ativos no painel estático:</p>"
            + "<ul>" + itensClientes + "</ul>";
    };

    const gerarRelatorioAnual = function () {
        if (!elementos.reportPreview || !elementos.anoAnualInput) {
            return;
        }

        const ano = Number(elementos.anoAnualInput.value) || new Date().getFullYear();
        const receitaMensal = obterReceitaMensal();
        const receitaAnual = receitaMensal * 12;

        elementos.reportPreview.innerHTML = ""
            + "<p><strong>Relatorio Anual - " + ano + "</strong></p>"
            + "<p>Receita mensal base: <strong>" + formatarMoeda(receitaMensal) + "</strong></p>"
            + "<p>Receita anual estimada: <strong>" + formatarMoeda(receitaAnual) + "</strong></p>"
            + "<p>Observacao: esta tela esta em modo estatico e sera integrada ao backend na proxima etapa.</p>";
    };

    const selecionarCliente = async function (clienteId) {
        if (!clienteId) {
            return;
        }

        const itens = elementos.lista.querySelectorAll(".cliente-item");
        itens.forEach(function (item) {
            item.classList.toggle("active", item.dataset.id === clienteId);
        });

        try {
            const clienteApi = await buscarJson("/clientes/" + encodeURIComponent(clienteId));
            clienteSelecionado = normalizarCliente(clienteApi);
            setDetalheFeedback("", false);
            const clienteLista = clientes.find(function (cliente) {
                return cliente.id === clienteSelecionado.id;
            });
            if (clienteLista) {
                clienteSelecionado.valorMensal = clienteLista.valorMensal;
            }
            renderDetalhe(clienteSelecionado);
            ativarAba("tab-detalhes");
        } catch (error) {
            clienteSelecionado = null;
            renderDetalhe(null);
            elementos.reciboPreview.innerHTML = "<p>Nao foi possivel carregar os detalhes do cliente selecionado.</p>";
        }
    };

    const excluirClienteSelecionado = async function () {
        if (!clienteSelecionado || !clienteSelecionado.id) {
            return;
        }

        const confirmar = window.confirm("Deseja realmente excluir este cliente?");
        if (!confirmar) {
            return;
        }

        const response = await fetch("/clientes/" + encodeURIComponent(clienteSelecionado.id), {
            method: "DELETE",
            headers: obterHeadersComCsrf({
                Accept: "application/json"
            })
        });

        if (!response.ok) {
            const mensagemErro = await obterMensagemErroApi(response, "Falha ao excluir cliente");
            throw new Error(mensagemErro);
        }

        clienteSelecionado = null;
        clienteEmEdicaoId = null;
        atualizarModoCadastro();
        await carregarClientesBackend();
        atualizarKpis();
        renderLista();
        renderDetalhe(null);
        elementos.reciboPreview.innerHTML = "<p>O recibo sera preenchido automaticamente com os dados do cliente selecionado.</p>";
        setDetalheFeedback("Cliente excluido com sucesso.", false);
    };

    const renderLeads = function () {
        if (!elementos.leadsLista) {
            return;
        }

        elementos.leadsLista.innerHTML = "";

        if (leads.length === 0) {
            elementos.leadsLista.innerHTML = "<li class=\"lead-item\"><p>Nenhum lead cadastrado ainda.</p></li>";
            return;
        }

        leads.forEach(function (lead) {
            const item = document.createElement("li");
            item.className = "lead-item";
            item.innerHTML = ""
                + "<h4>" + lead.nome + "</h4>"
                + "<p><strong>Telefone:</strong> " + lead.telefone + "</p>"
                + "<p><strong>Desenvolvimento:</strong> " + formatarMoeda(lead.orcamentoDesenvolvimento)
                + " | <strong>Manutencao/Hospedagem:</strong> " + formatarMoeda(lead.orcamentoManutencaoHospedagem) + "</p>"
                + "<p><strong>Observacoes:</strong> " + (lead.observacoes || "Sem observacoes") + "</p>";

            elementos.leadsLista.appendChild(item);
        });
    };

    const salvarLead = function () {
        if (!elementos.leadNomeInput || !elementos.leadTelefoneInput || !elementos.leadOrcDevInput || !elementos.leadOrcManutencaoInput) {
            return;
        }

        const nome = elementos.leadNomeInput.value.trim();
        const telefone = elementos.leadTelefoneInput.value.trim();
        const orcDesenvolvimento = Number(elementos.leadOrcDevInput.value);
        const orcManutencao = Number(elementos.leadOrcManutencaoInput.value);
        const observacoes = elementos.leadObsInput ? elementos.leadObsInput.value.trim() : "";

        if (!nome || !telefone || Number.isNaN(orcDesenvolvimento) || Number.isNaN(orcManutencao)) {
            return;
        }

        leads.unshift({
            id: "l" + Date.now(),
            nome: nome,
            telefone: telefone,
            observacoes: observacoes,
            orcamentoDesenvolvimento: orcDesenvolvimento,
            orcamentoManutencaoHospedagem: orcManutencao
        });

        renderLeads();

        if (elementos.leadForm) {
            elementos.leadForm.reset();
        }
    };

    const renderLista = function () {
        elementos.lista.innerHTML = "";

        if (clientes.length === 0) {
            elementos.lista.innerHTML = "<li class=\"cliente-item\"><p>Nenhum cliente cadastrado ainda.</p></li>";
            return;
        }

        clientes.forEach(function (cliente) {
            const item = document.createElement("li");
            item.className = "cliente-item";
            item.dataset.id = cliente.id;
            item.innerHTML = ""
                + "<h4>" + cliente.nome + "</h4>"
                + "<p>Contato: " + cliente.contato + "</p>"
                + "<p>Dominio: " + cliente.dominioAplicacao + "</p>"
                + "<p>Mensalidade: " + formatarMoeda(cliente.valorMensal) + "</p>";

            item.addEventListener("click", async function () {
                await selecionarCliente(cliente.id);
            });

            elementos.lista.appendChild(item);
        });
    };

    const carregarClientesBackend = async function () {
        const data = await buscarJson("/clientes/dashboard");
        const lista = Array.isArray(data) ? data : [];
        clientes.splice(0, clientes.length, ...lista.map(normalizarCliente));
    };

    const salvarNovoCliente = async function () {
        if (!elementos.novoNomeInput || !elementos.novoContatoInput || !elementos.novoDominioInput || !elementos.novoVencimentoInput || !elementos.novoAtivoInput) {
            return;
        }

        const nome = elementos.novoNomeInput.value.trim();
        const contato = elementos.novoContatoInput.value.trim();
        const dominioAplicacao = elementos.novoDominioInput.value.trim();
        const dataVencimentoDominio = elementos.novoVencimentoInput.value;
        const ativo = elementos.novoAtivoInput.value === "true";
        const valorMensal = Number(elementos.novoValorInput ? elementos.novoValorInput.value : 0) || 0;

        if (!nome || !contato || !dominioAplicacao || !dataVencimentoDominio) {
            setCadastroFeedback("Preencha todos os campos obrigatorios para salvar.", true);
            return;
        }

        const payload = {
            nome: nome,
            contato: contato,
            dominioAplicacao: dominioAplicacao,
            dataVencimentoDominio: dataVencimentoDominio,
            valorMensal: valorMensal,
            ativo: ativo ? 1 : 0
        };

        const csrfToken = csrfTokenElement ? csrfTokenElement.getAttribute("content") : "";
        const csrfHeader = csrfHeaderElement ? csrfHeaderElement.getAttribute("content") : "";
        if (!csrfToken || !csrfHeader) {
            throw new Error("Token de seguranca nao encontrado. Atualize a pagina e tente novamente.");
        }

        const editando = Boolean(clienteEmEdicaoId);
        const url = editando
            ? "/clientes/" + encodeURIComponent(clienteEmEdicaoId)
            : "/clientes";
        const metodo = editando ? "PUT" : "POST";

        const response = await fetch(url, {
            method: metodo,
            headers: obterHeadersComCsrf({
                "Content-Type": "application/json",
                Accept: "application/json"
            }),
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const mensagemErro = await obterMensagemErroApi(response, editando ? "Falha ao atualizar cliente" : "Falha ao criar cliente");
            throw new Error(mensagemErro);
        }

        const clienteCriado = normalizarCliente(await response.json());
        clienteCriado.valorMensal = valorMensal;

        await carregarClientesBackend();

        if (elementos.cadastroForm) {
            elementos.cadastroForm.reset();
        }

        clienteEmEdicaoId = null;
        atualizarModoCadastro();

        atualizarKpis();
        renderLista();
        setCadastroFeedback(editando ? "Cliente atualizado com sucesso." : "Cliente cadastrado com sucesso.", false);
        ativarAba("tab-cadastro");
    };

    elementos.reciboButton.addEventListener("click", async function () {
        if (!clienteSelecionado) {
            return;
        }

        try {
            await gerarReciboComMovimentacoes(clienteSelecionado);
        } catch (error) {
            gerarRecibo(clienteSelecionado);
        }
    });

    if (elementos.excluirClienteButton) {
        elementos.excluirClienteButton.addEventListener("click", async function () {
            try {
                await excluirClienteSelecionado();
            } catch (error) {
                const mensagem = error instanceof Error && error.message
                    ? error.message
                    : "Nao foi possivel excluir cliente agora.";
                setDetalheFeedback(mensagem, true);
            }
        });
    }

    if (elementos.editarClienteButton) {
        elementos.editarClienteButton.addEventListener("click", function () {
            if (!clienteSelecionado) {
                return;
            }

            clienteEmEdicaoId = clienteSelecionado.id;
            preencherFormularioComCliente(clienteSelecionado);
            atualizarModoCadastro();
            setCadastroFeedback("Edite os campos e clique em Atualizar Cliente.", false);
            ativarAba("tab-cadastro");
        });
    }

    elementos.navTabs.forEach(function (tab) {
        tab.addEventListener("click", function () {
            ativarAba(tab.dataset.tabTarget);
        });
    });

    if (elementos.modoMensalBtn && elementos.modoAnualBtn) {
        elementos.modoMensalBtn.addEventListener("click", function () {
            definirModoRelatorio("mensal");
        });

        elementos.modoAnualBtn.addEventListener("click", function () {
            definirModoRelatorio("anual");
        });
    }

    if (elementos.gerarMensalBtn) {
        elementos.gerarMensalBtn.addEventListener("click", gerarRelatorioMensal);
    }

    if (elementos.gerarAnualBtn) {
        elementos.gerarAnualBtn.addEventListener("click", gerarRelatorioAnual);
    }

    if (elementos.leadForm) {
        elementos.leadForm.addEventListener("submit", function () {
            salvarLead();
        });
    }

    if (elementos.cadastroForm) {
        elementos.cadastroForm.addEventListener("submit", async function () {
            try {
                await salvarNovoCliente();
            } catch (error) {
                const mensagem = error instanceof Error && error.message
                    ? error.message
                    : "Nao foi possivel salvar cliente agora. Tente novamente.";
                setCadastroFeedback(mensagem, true);
            }
        });
    }

    const iniciarPainel = async function () {
        try {
            await carregarClientesBackend();
        } catch (error) {
            setCadastroFeedback("Nao foi possivel carregar clientes do backend no momento.", true);
        }

        atualizarKpis();
        renderLista();
        renderDetalhe(null);
        atualizarModoCadastro();
        ativarAba("tab-clientes");
        definirModoRelatorio("mensal");
        gerarRelatorioMensal();
        renderLeads();
    };

    iniciarPainel();
});
