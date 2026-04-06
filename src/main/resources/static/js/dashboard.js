window.addEventListener("DOMContentLoaded", function () {
    const clientes = [
        {
            id: "c1",
            nome: "Orion Tech",
            dominioAplicacao: "oriontech.com.br",
            dataVencimentoDominio: "2026-05-20",
            ativo: true,
            valorMensal: 350.0
        },
        {
            id: "c2",
            nome: "Solaris Studio",
            dominioAplicacao: "solarisstudio.com",
            dataVencimentoDominio: "2026-06-11",
            ativo: true,
            valorMensal: 280.0
        },
        {
            id: "c3",
            nome: "Nexus Cargo",
            dominioAplicacao: "nexuscargo.com.br",
            dataVencimentoDominio: "2026-04-28",
            ativo: false,
            valorMensal: 420.0
        }
    ];

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
        return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(valor);
    };

    const formatarData = function (isoDate) {
        const data = new Date(isoDate + "T00:00:00");
        return new Intl.DateTimeFormat("pt-BR").format(data);
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
            return;
        }

        elementos.detalhe.classList.remove("empty");
        elementos.detalhe.innerHTML = ""
            + "<dl>"
            + "<dt>Nome</dt><dd>" + cliente.nome + "</dd>"
            + "<dt>Dominio</dt><dd>" + cliente.dominioAplicacao + "</dd>"
            + "<dt>Vencimento</dt><dd>" + formatarData(cliente.dataVencimentoDominio) + "</dd>"
            + "<dt>Status</dt><dd>" + (cliente.ativo ? "Ativo" : "Inativo") + "</dd>"
            + "<dt>Valor Mensal</dt><dd>" + formatarMoeda(cliente.valorMensal) + "</dd>"
            + "</dl>";

        elementos.reciboButton.disabled = false;
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

    const selecionarCliente = function (clienteId) {
        clienteSelecionado = clientes.find(function (cliente) {
            return cliente.id === clienteId;
        }) || null;

        const itens = elementos.lista.querySelectorAll(".cliente-item");
        itens.forEach(function (item) {
            item.classList.toggle("active", item.dataset.id === clienteId);
        });

        renderDetalhe(clienteSelecionado);
        ativarAba("tab-detalhes");
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

        clientes.forEach(function (cliente) {
            const item = document.createElement("li");
            item.className = "cliente-item";
            item.dataset.id = cliente.id;
            item.innerHTML = ""
                + "<h4>" + cliente.nome + "</h4>"
                + "<p>Mensalidade: " + formatarMoeda(cliente.valorMensal) + "</p>";

            item.addEventListener("click", function () {
                selecionarCliente(cliente.id);
            });

            elementos.lista.appendChild(item);
        });
    };

    elementos.reciboButton.addEventListener("click", function () {
        gerarRecibo(clienteSelecionado);
    });

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

    atualizarKpis();
    renderLista();
    renderDetalhe(null);
    ativarAba("tab-clientes");
    definirModoRelatorio("mensal");
    gerarRelatorioMensal();
    renderLeads();
});
