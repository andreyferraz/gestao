window.addEventListener("DOMContentLoaded", function () {
    const clientes = [];
    const leads = [];
    const chamados = [];
    const CHAVE_ABA_ATIVA = "gestao.dashboard.abaAtiva";

    const elementos = {
        kpiClientes: document.getElementById("kpi-clientes"),
        kpiReceita: document.getElementById("kpi-receita"),
        kpiDominios: document.getElementById("kpi-dominios"),
        dominiosAlerta: document.getElementById("dominios-alerta"),
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
        leadSalvarButton: document.getElementById("lead-salvar"),
        leadsLista: document.getElementById("leads-lista"),
        leadFeedback: document.getElementById("lead-feedback"),
        leadModo: document.getElementById("lead-modo"),
        leadDetalhe: document.getElementById("lead-detalhe"),
        leadEditarButton: document.getElementById("lead-editar"),
        leadExcluirButton: document.getElementById("lead-excluir")
        , chamadoForm: document.getElementById("chamado-form")
        , chamadoClienteSelect: document.getElementById("chamado-cliente")
        , chamadoDescricaoInput: document.getElementById("chamado-descricao")
        , chamadoSalvarButton: document.getElementById("chamado-salvar")
        , chamadosLista: document.getElementById("chamados-lista")
        , chamadoFeedback: document.getElementById("chamado-feedback")
        , chamadoEditarModal: document.getElementById("chamado-editar-modal")
        , chamadoEditarDialog: document.getElementById("chamado-editar-dialog")
        , chamadoEditarTextoInput: document.getElementById("chamado-editar-texto")
        , chamadoEditarSalvarButton: document.getElementById("chamado-editar-salvar")
        , chamadoEditarCancelarButton: document.getElementById("chamado-editar-cancelar")
        , chamadoEditarFecharButton: document.getElementById("chamado-editar-fechar")
    };

    if (!elementos.lista || !elementos.detalhe || !elementos.reciboButton || !elementos.reciboPreview) {
        return;
    }

    let clienteSelecionado = null;
    let clienteEmEdicaoId = null;
    let leadSelecionado = null;
    let leadEmEdicaoId = null;
    let chamadoEmEdicao = null;

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

        try {
            window.localStorage.setItem(CHAVE_ABA_ATIVA, tabId);
        } catch (error) {
            // Ignore storage failures and keep navigation working.
        }
    };

    const obterAbaInicial = function () {
        const abaPadrao = "tab-clientes";
        const abasDisponiveis = new Set(elementos.tabPanels.map(function (panel) {
            return panel.id;
        }));

        try {
            const abaSalva = window.localStorage.getItem(CHAVE_ABA_ATIVA);
            if (abaSalva && abasDisponiveis.has(abaSalva)) {
                return abaSalva;
            }
        } catch (error) {
            // Ignore storage failures and use default tab.
        }

        return abaPadrao;
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

    const diasParaVencerDominio = function (isoDate) {
        if (!isoDate) {
            return null;
        }

        const hoje = new Date();
        hoje.setHours(0, 0, 0, 0);

        const vencimento = new Date(isoDate + "T00:00:00");
        if (Number.isNaN(vencimento.getTime())) {
            return null;
        }

        const diffMs = vencimento.getTime() - hoje.getTime();
        return Math.ceil(diffMs / 86400000);
    };

    const obterClientesComDominioProximo = function () {
        return clientes
            .map(function (cliente) {
                return {
                    cliente: cliente,
                    diasRestantes: diasParaVencerDominio(cliente.dataVencimentoDominio)
                };
            })
            .filter(function (item) {
                return item.diasRestantes !== null && item.diasRestantes >= 0 && item.diasRestantes <= 10;
            })
            .sort(function (a, b) {
                return a.diasRestantes - b.diasRestantes;
            });
    };

    const renderAlertasDominio = function () {
        if (!elementos.dominiosAlerta) {
            return;
        }

        const alertas = obterClientesComDominioProximo();
        if (alertas.length === 0) {
            elementos.dominiosAlerta.classList.add("empty");
            elementos.dominiosAlerta.innerHTML = "";
            return;
        }

        const itens = alertas.map(function (item) {
            const cliente = item.cliente;
            const dias = item.diasRestantes;
            const labelDias = dias === 0 ? "vence hoje" : "vence em " + dias + " dia" + (dias === 1 ? "" : "s");
            return "<li><strong>" + cliente.nome + "</strong> (" + cliente.dominioAplicacao + "): " + labelDias + "</li>";
        }).join("");

        elementos.dominiosAlerta.classList.remove("empty");
        elementos.dominiosAlerta.innerHTML = ""
            + "<div class=\"dominios-alerta-box\">"
            + "<h4>Atencao: dominios vencendo nos proximos 10 dias</h4>"
            + "<ul>" + itens + "</ul>"
            + "</div>";
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

    const normalizarLead = function (lead) {
        return {
            id: lead.id,
            nome: lead.nome || "Sem nome",
            telefone: lead.telefone || "Nao informado",
            observacoes: lead.observacoes || "",
            orcamentoDesenvolvimento: Number(lead.orcamentoDesenvolvimento) || 0,
            orcamentoManutencaoHospedagem: Number(lead.orcamentoManutencaoHospedagem) || 0
        };
    };

    const normalizarChamado = function (chamado) {
        const status = chamado.status === "RESOLVIDO" ? "RESOLVIDO" : "ABERTO";
        return {
            id: chamado.id,
            clienteId: chamado.clienteId,
            descricaoProblema: chamado.descricaoProblema || "Sem descricao",
            status: status
        };
    };

    const obterNomeClientePorId = function (clienteId) {
        const cliente = clientes.find(function (item) {
            return item.id === clienteId;
        });
        return cliente ? cliente.nome : "Cliente nao encontrado";
    };

    const setChamadoFeedback = function (mensagem, erro) {
        if (!elementos.chamadoFeedback) {
            return;
        }

        elementos.chamadoFeedback.textContent = mensagem;
        elementos.chamadoFeedback.style.color = erro ? "#b91c1c" : "#0f766e";
    };

    const abrirModalEdicaoChamado = function (chamado) {
        if (!elementos.chamadoEditarModal || !elementos.chamadoEditarTextoInput || !chamado) {
            return;
        }

        chamadoEmEdicao = chamado;
        elementos.chamadoEditarTextoInput.value = chamado.descricaoProblema || "";
        elementos.chamadoEditarModal.hidden = false;
        if (elementos.chamadoEditarDialog && !elementos.chamadoEditarDialog.open) {
            elementos.chamadoEditarDialog.showModal();
        }
        elementos.chamadoEditarTextoInput.focus();
    };

    const fecharModalEdicaoChamado = function () {
        if (!elementos.chamadoEditarModal || !elementos.chamadoEditarTextoInput) {
            return;
        }

        chamadoEmEdicao = null;
        elementos.chamadoEditarTextoInput.value = "";
        if (elementos.chamadoEditarDialog && elementos.chamadoEditarDialog.open) {
            elementos.chamadoEditarDialog.close();
        }
        elementos.chamadoEditarModal.hidden = true;
    };

    const popularSelectClientesChamado = function () {
        if (!elementos.chamadoClienteSelect) {
            return;
        }

        const valorAtual = elementos.chamadoClienteSelect.value;
        elementos.chamadoClienteSelect.innerHTML = '<option value="">Selecione um cliente</option>';

        clientes.forEach(function (cliente) {
            const option = document.createElement("option");
            option.value = cliente.id;
            option.textContent = cliente.nome;
            elementos.chamadoClienteSelect.appendChild(option);
        });

        if (valorAtual) {
            elementos.chamadoClienteSelect.value = valorAtual;
        }
    };

    const setLeadFeedback = function (mensagem, erro) {
        if (!elementos.leadFeedback) {
            return;
        }

        elementos.leadFeedback.textContent = mensagem;
        elementos.leadFeedback.style.color = erro ? "#b91c1c" : "#0f766e";
    };

    const atualizarModoLead = function () {
        const emEdicao = Boolean(leadEmEdicaoId);
        if (elementos.leadModo) {
            elementos.leadModo.textContent = emEdicao
                ? "Modo atual: edicao de lead."
                : "Modo atual: novo lead.";
        }
        if (elementos.leadSalvarButton) {
            elementos.leadSalvarButton.textContent = emEdicao
                ? "Atualizar Lead"
                : "Salvar Lead";
        }
    };

    const preencherFormularioComLead = function (lead) {
        if (!lead || !elementos.leadNomeInput || !elementos.leadTelefoneInput || !elementos.leadOrcDevInput || !elementos.leadOrcManutencaoInput) {
            return;
        }

        elementos.leadNomeInput.value = lead.nome || "";
        elementos.leadTelefoneInput.value = lead.telefone || "";
        elementos.leadOrcDevInput.value = String(Number(lead.orcamentoDesenvolvimento) || 0);
        elementos.leadOrcManutencaoInput.value = String(Number(lead.orcamentoManutencaoHospedagem) || 0);
        if (elementos.leadObsInput) {
            elementos.leadObsInput.value = lead.observacoes || "";
        }
    };

    const renderDetalheLead = function (lead) {
        if (!elementos.leadDetalhe) {
            return;
        }

        if (!lead) {
            elementos.leadDetalhe.classList.add("empty");
            elementos.leadDetalhe.innerHTML = "<p>Selecione um lead para ver os detalhes.</p>";
            if (elementos.leadEditarButton) {
                elementos.leadEditarButton.disabled = true;
            }
            if (elementos.leadExcluirButton) {
                elementos.leadExcluirButton.disabled = true;
            }
            return;
        }

        elementos.leadDetalhe.classList.remove("empty");
        elementos.leadDetalhe.innerHTML = ""
            + "<dl>"
            + "<dt>Nome</dt><dd>" + lead.nome + "</dd>"
            + "<dt>Telefone</dt><dd>" + lead.telefone + "</dd>"
            + "<dt>Desenvolvimento</dt><dd>" + formatarMoeda(lead.orcamentoDesenvolvimento) + "</dd>"
            + "<dt>Manutencao/Hospedagem</dt><dd>" + formatarMoeda(lead.orcamentoManutencaoHospedagem) + "</dd>"
            + "<dt>Observacoes</dt><dd>" + (lead.observacoes || "Sem observacoes") + "</dd>"
            + "</dl>";

        if (elementos.leadEditarButton) {
            elementos.leadEditarButton.disabled = false;
        }
        if (elementos.leadExcluirButton) {
            elementos.leadExcluirButton.disabled = false;
        }
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

    const gerarRelatorioMensal = async function () {
        if (!elementos.reportPreview || !elementos.mesInput || !elementos.anoMensalInput) {
            return;
        }

        const mesNumero = Number(elementos.mesInput.value);
        const ano = Number(elementos.anoMensalInput.value) || new Date().getFullYear();

        if (!Number.isInteger(mesNumero) || mesNumero < 1 || mesNumero > 12) {
            throw new Error("Mes invalido para relatorio mensal.");
        }

        const relatorio = await buscarJson(
            "/relatorios/mensal?ano=" + encodeURIComponent(ano) + "&mes=" + encodeURIComponent(mesNumero)
        );

        const nomeMes = nomesMeses[Math.max(0, Math.min(11, (Number(relatorio.mes) || mesNumero) - 1))];
        const totalEntradas = Number(relatorio.totalEntradas) || 0;
        const totalSaidas = Number(relatorio.totalSaidas) || 0;
        const saldo = Number(relatorio.saldo) || 0;
        const movimentacoes = Array.isArray(relatorio.movimentacoes) ? relatorio.movimentacoes : [];

        const itensMovimentacoes = movimentacoes.length === 0
            ? "<li>Nenhuma movimentacao encontrada para este periodo.</li>"
            : movimentacoes.map(function (mov) {
                return "<li>"
                    + "<strong>" + formatarData(mov.dataOcorrencia) + "</strong> - "
                    + (mov.tipo || "N/A") + " - "
                    + formatarMoeda(mov.valor)
                    + (mov.descricao ? " - " + mov.descricao : "")
                    + "</li>";
            }).join("");

        elementos.reportPreview.innerHTML = ""
            + "<p><strong>Relatorio Mensal - " + nomeMes + "/" + (Number(relatorio.ano) || ano) + "</strong></p>"
            + "<p>Total de entradas: <strong>" + formatarMoeda(totalEntradas) + "</strong></p>"
            + "<p>Total de saidas: <strong>" + formatarMoeda(totalSaidas) + "</strong></p>"
            + "<p>Saldo do mes: <strong>" + formatarMoeda(saldo) + "</strong></p>"
            + "<p>Movimentacoes:</p>"
            + "<ul>" + itensMovimentacoes + "</ul>";
    };

    const gerarRelatorioAnual = async function () {
        if (!elementos.reportPreview || !elementos.anoAnualInput) {
            return;
        }

        const ano = Number(elementos.anoAnualInput.value) || new Date().getFullYear();

        const relatorio = await buscarJson("/relatorios/anual?ano=" + encodeURIComponent(ano));
        const totalEntradas = Number(relatorio.totalEntradas) || 0;
        const totalSaidas = Number(relatorio.totalSaidas) || 0;
        const saldo = Number(relatorio.saldo) || 0;
        const totalMovimentacoes = Number(relatorio.quantidadeMovimentacoes) || 0;
        const resumoMensal = Array.isArray(relatorio.resumoMensal) ? relatorio.resumoMensal : [];

        const itensResumo = resumoMensal.map(function (item) {
            const mes = Number(item.mes) || 0;
            const nomeMes = nomesMeses[Math.max(0, Math.min(11, mes - 1))] || "Mes " + mes;
            const saldoMes = Number(item.saldo) || 0;
            const quantidade = Number(item.quantidadeMovimentacoes) || 0;
            return "<li><strong>" + nomeMes + "</strong>: saldo " + formatarMoeda(saldoMes)
                + " (" + quantidade + " " + (quantidade === 1 ? "movimentacao" : "movimentacoes") + ")</li>";
        }).join("");

        elementos.reportPreview.innerHTML = ""
            + "<p><strong>Relatorio Anual - " + (Number(relatorio.ano) || ano) + "</strong></p>"
            + "<p>Total de entradas: <strong>" + formatarMoeda(totalEntradas) + "</strong></p>"
            + "<p>Total de saidas: <strong>" + formatarMoeda(totalSaidas) + "</strong></p>"
            + "<p>Saldo anual: <strong>" + formatarMoeda(saldo) + "</strong></p>"
            + "<p>Total de movimentacoes no ano: <strong>" + totalMovimentacoes + "</strong></p>"
            + "<p>Resumo por mes:</p>"
            + "<ul>" + itensResumo + "</ul>";
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
        renderAlertasDominio();
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
            item.className = "lead-item" + (leadSelecionado && leadSelecionado.id === lead.id ? " active" : "");
            item.dataset.id = lead.id;
            item.innerHTML = ""
                + "<h4>" + lead.nome + "</h4>"
                + "<p><strong>Telefone:</strong> " + lead.telefone + "</p>"
                + "<p><strong>Desenvolvimento:</strong> " + formatarMoeda(lead.orcamentoDesenvolvimento)
                + " | <strong>Manutencao/Hospedagem:</strong> " + formatarMoeda(lead.orcamentoManutencaoHospedagem) + "</p>"
                + "<p><strong>Observacoes:</strong> " + (lead.observacoes || "Sem observacoes") + "</p>";

            item.addEventListener("click", function () {
                leadSelecionado = lead;
                renderLeads();
                renderDetalheLead(leadSelecionado);
            });

            elementos.leadsLista.appendChild(item);
        });
    };

    const carregarLeadsBackend = async function () {
        const data = await buscarJson("/leads");
        const lista = Array.isArray(data) ? data : [];
        leads.splice(0, leads.length, ...lista.map(normalizarLead));
    };

    const carregarChamadosBackend = async function () {
        const data = await buscarJson("/chamados");
        const lista = Array.isArray(data) ? data : [];
        chamados.splice(0, chamados.length, ...lista.map(normalizarChamado));
    };

    const renderChamados = function () {
        if (!elementos.chamadosLista) {
            return;
        }

        elementos.chamadosLista.innerHTML = "";

        if (chamados.length === 0) {
            elementos.chamadosLista.innerHTML = "<li class=\"chamado-item\"><p>Nenhum chamado aberto ainda.</p></li>";
            return;
        }

        chamados.forEach(function (chamado) {
            const item = document.createElement("li");
            item.className = "chamado-item";

            const statusClass = chamado.status === "RESOLVIDO" ? "resolvido" : "aberto";
            const statusLabel = chamado.status === "RESOLVIDO" ? "Resolvido" : "Aberto";
            const proximoStatus = chamado.status === "RESOLVIDO" ? "ABERTO" : "RESOLVIDO";
            const acaoTexto = chamado.status === "RESOLVIDO" ? "Reabrir" : "Marcar como resolvido";

            item.innerHTML = ""
                + "<div class=\"chamado-topo\">"
                + "<h4>" + obterNomeClientePorId(chamado.clienteId) + "</h4>"
                + "<span class=\"chamado-status " + statusClass + "\">" + statusLabel + "</span>"
                + "</div>"
                + "<p>" + chamado.descricaoProblema + "</p>"
                + "<div class=\"chamado-botoes\">"
                + "<button type=\"button\" class=\"chamado-editar\" data-id=\"" + chamado.id + "\">Editar texto</button>"
                + "<button type=\"button\" class=\"chamado-acao\" data-id=\"" + chamado.id + "\" data-status=\"" + proximoStatus + "\">" + acaoTexto + "</button>"
                + "</div>";

            const botaoAcao = item.querySelector(".chamado-acao");
            if (botaoAcao) {
                botaoAcao.addEventListener("click", async function () {
                    try {
                        await atualizarStatusChamado(chamado.id, proximoStatus);
                    } catch (error) {
                        const mensagem = error instanceof Error && error.message
                            ? error.message
                            : "Nao foi possivel atualizar status do chamado.";
                        setChamadoFeedback(mensagem, true);
                    }
                });
            }

            const botaoEditar = item.querySelector(".chamado-editar");
            if (botaoEditar) {
                botaoEditar.addEventListener("click", function () {
                    abrirModalEdicaoChamado(chamado);
                });
            }

            elementos.chamadosLista.appendChild(item);
        });
    };

    const salvarChamado = async function () {
        if (!elementos.chamadoClienteSelect || !elementos.chamadoDescricaoInput) {
            return;
        }

        const clienteId = elementos.chamadoClienteSelect.value;
        const descricaoProblema = elementos.chamadoDescricaoInput.value.trim();

        if (!clienteId || !descricaoProblema) {
            throw new Error("Selecione um cliente e descreva o problema do chamado.");
        }

        const payload = {
            clienteId: clienteId,
            descricaoProblema: descricaoProblema,
            status: "ABERTO"
        };

        const response = await fetch("/chamados", {
            method: "POST",
            headers: obterHeadersComCsrf({
                "Content-Type": "application/json",
                Accept: "application/json"
            }),
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const mensagemErro = await obterMensagemErroApi(response, "Falha ao abrir chamado");
            throw new Error(mensagemErro);
        }

        await carregarChamadosBackend();
        renderChamados();
        setChamadoFeedback("Chamado aberto com sucesso.", false);

        if (elementos.chamadoForm) {
            elementos.chamadoForm.reset();
        }
    };

    const atualizarStatusChamado = async function (chamadoId, status) {
        const response = await fetch(
            "/chamados/" + encodeURIComponent(chamadoId) + "/status?status=" + encodeURIComponent(status),
            {
                method: "PUT",
                headers: obterHeadersComCsrf({
                    Accept: "application/json"
                })
            }
        );

        if (!response.ok) {
            const mensagemErro = await obterMensagemErroApi(response, "Falha ao atualizar status do chamado");
            throw new Error(mensagemErro);
        }

        const chamadoAtualizado = normalizarChamado(await response.json());
        const index = chamados.findIndex(function (item) {
            return item.id === chamadoAtualizado.id;
        });
        if (index >= 0) {
            chamados[index] = chamadoAtualizado;
        }

        renderChamados();
        setChamadoFeedback("Status do chamado atualizado com sucesso.", false);
    };

    const editarDescricaoChamado = async function (chamadoId, novaDescricao) {
        if (novaDescricao === null || novaDescricao === undefined) {
            return;
        }

        const descricaoLimpa = novaDescricao.trim();
        if (!descricaoLimpa) {
            throw new Error("A descricao do chamado nao pode ficar vazia.");
        }

        const response = await fetch(
            "/chamados/" + encodeURIComponent(chamadoId) + "/descricao",
            {
                method: "PUT",
                headers: obterHeadersComCsrf({
                    "Content-Type": "application/json",
                    Accept: "application/json"
                }),
                body: JSON.stringify({
                    descricaoProblema: descricaoLimpa
                })
            }
        );

        if (!response.ok) {
            const mensagemErro = await obterMensagemErroApi(response, "Falha ao editar texto do chamado");
            throw new Error(mensagemErro);
        }

        const chamadoAtualizado = normalizarChamado(await response.json());
        const index = chamados.findIndex(function (item) {
            return item.id === chamadoAtualizado.id;
        });
        if (index >= 0) {
            chamados[index] = chamadoAtualizado;
        }

        renderChamados();
        setChamadoFeedback("Texto do chamado atualizado com sucesso.", false);
        fecharModalEdicaoChamado();
    };

    const salvarLead = async function () {
        if (!elementos.leadNomeInput || !elementos.leadTelefoneInput || !elementos.leadOrcDevInput || !elementos.leadOrcManutencaoInput) {
            return;
        }

        const nome = elementos.leadNomeInput.value.trim();
        const telefone = elementos.leadTelefoneInput.value.trim();
        const orcDesenvolvimento = Number(elementos.leadOrcDevInput.value);
        const orcManutencao = Number(elementos.leadOrcManutencaoInput.value);
        const observacoes = elementos.leadObsInput ? elementos.leadObsInput.value.trim() : "";

        if (!nome || !telefone || Number.isNaN(orcDesenvolvimento) || Number.isNaN(orcManutencao)) {
            throw new Error("Preencha os campos obrigatorios do lead.");
        }

        const payload = {
            nome: nome,
            telefone: telefone,
            observacoes: observacoes,
            orcamentoDesenvolvimento: orcDesenvolvimento,
            orcamentoManutencaoHospedagem: orcManutencao
        };

        const editando = Boolean(leadEmEdicaoId);
        const url = editando
            ? "/leads/" + encodeURIComponent(leadEmEdicaoId)
            : "/leads";
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
            const mensagemErro = await obterMensagemErroApi(response, "Falha ao salvar lead");
            throw new Error(mensagemErro);
        }

        const leadSalvo = normalizarLead(await response.json());
        if (editando) {
            const index = leads.findIndex(function (lead) {
                return lead.id === leadSalvo.id;
            });
            if (index >= 0) {
                leads[index] = leadSalvo;
            }
        } else {
            leads.unshift(leadSalvo);
        }

        try {
            await carregarLeadsBackend();
        } catch (error) {
            // Keep optimistic update if list refresh fails.
        }

        leadSelecionado = leadSalvo;
        leadEmEdicaoId = null;
        atualizarModoLead();

        renderLeads();
        renderDetalheLead(leadSelecionado);
        setLeadFeedback(editando ? "Lead atualizado com sucesso." : "Lead salvo com sucesso.", false);

        if (elementos.leadForm) {
            elementos.leadForm.reset();
        }
    };

    const excluirLeadSelecionado = async function () {
        if (!leadSelecionado || !leadSelecionado.id) {
            return;
        }

        const confirmar = window.confirm("Deseja realmente excluir este lead?");
        if (!confirmar) {
            return;
        }

        const response = await fetch("/leads/" + encodeURIComponent(leadSelecionado.id), {
            method: "DELETE",
            headers: obterHeadersComCsrf({
                Accept: "application/json"
            })
        });

        if (!response.ok) {
            const mensagemErro = await obterMensagemErroApi(response, "Falha ao excluir lead");
            throw new Error(mensagemErro);
        }

        const idExcluido = leadSelecionado.id;
        leadSelecionado = null;
        leadEmEdicaoId = null;
        atualizarModoLead();

        const leadsRestantes = leads.filter(function (lead) {
            return lead.id !== idExcluido;
        });
        leads.splice(0, leads.length, ...leadsRestantes);

        renderLeads();
        renderDetalheLead(null);
        if (elementos.leadForm) {
            elementos.leadForm.reset();
        }
        setLeadFeedback("Lead excluido com sucesso.", false);
    };

    const renderLista = function () {
        elementos.lista.innerHTML = "";

        if (clientes.length === 0) {
            elementos.lista.innerHTML = "<li class=\"cliente-item\"><p>Nenhum cliente cadastrado ainda.</p></li>";
            return;
        }

        clientes.forEach(function (cliente) {
            const diasRestantes = diasParaVencerDominio(cliente.dataVencimentoDominio);
            const proximoDeVencer = diasRestantes !== null && diasRestantes >= 0 && diasRestantes <= 10;
            const seloAlerta = proximoDeVencer
                ? "<span class=\"alerta-selo\">Vence em " + diasRestantes + " dia" + (diasRestantes === 1 ? "" : "s") + "</span>"
                : "";

            const item = document.createElement("li");
            item.className = "cliente-item";
            item.dataset.id = cliente.id;
            item.innerHTML = ""
                + "<div class=\"cliente-topo\"><h4>" + cliente.nome + "</h4>" + seloAlerta + "</div>"
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
        popularSelectClientesChamado();
        renderChamados();
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
        renderAlertasDominio();
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
        elementos.gerarMensalBtn.addEventListener("click", async function () {
            try {
                await gerarRelatorioMensal();
            } catch (error) {
                elementos.reportPreview.innerHTML = "<p>Nao foi possivel gerar o relatorio mensal agora.</p>";
            }
        });
    }

    if (elementos.gerarAnualBtn) {
        elementos.gerarAnualBtn.addEventListener("click", async function () {
            try {
                await gerarRelatorioAnual();
            } catch (error) {
                elementos.reportPreview.innerHTML = "<p>Nao foi possivel gerar o relatorio anual agora.</p>";
            }
        });
    }

    if (elementos.leadForm) {
        elementos.leadForm.addEventListener("submit", async function () {
            try {
                await salvarLead();
            } catch (error) {
                const mensagem = error instanceof Error && error.message
                    ? error.message
                    : "Nao foi possivel salvar lead agora.";
                setLeadFeedback(mensagem, true);
            }
        });
    }

    if (elementos.leadEditarButton) {
        elementos.leadEditarButton.addEventListener("click", function () {
            if (!leadSelecionado) {
                return;
            }

            leadEmEdicaoId = leadSelecionado.id;
            preencherFormularioComLead(leadSelecionado);
            atualizarModoLead();
            setLeadFeedback("Edite os campos e clique em Atualizar Lead.", false);
        });
    }

    if (elementos.leadExcluirButton) {
        elementos.leadExcluirButton.addEventListener("click", async function () {
            try {
                await excluirLeadSelecionado();
            } catch (error) {
                const mensagem = error instanceof Error && error.message
                    ? error.message
                    : "Nao foi possivel excluir lead agora.";
                setLeadFeedback(mensagem, true);
            }
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

    if (elementos.chamadoForm) {
        elementos.chamadoForm.addEventListener("submit", async function () {
            try {
                await salvarChamado();
            } catch (error) {
                const mensagem = error instanceof Error && error.message
                    ? error.message
                    : "Nao foi possivel abrir chamado agora.";
                setChamadoFeedback(mensagem, true);
            }
        });
    }

    if (elementos.chamadoEditarSalvarButton) {
        elementos.chamadoEditarSalvarButton.addEventListener("click", async function () {
            if (!chamadoEmEdicao || !elementos.chamadoEditarTextoInput) {
                return;
            }

            try {
                await editarDescricaoChamado(chamadoEmEdicao.id, elementos.chamadoEditarTextoInput.value);
            } catch (error) {
                const mensagem = error instanceof Error && error.message
                    ? error.message
                    : "Nao foi possivel editar o chamado.";
                setChamadoFeedback(mensagem, true);
            }
        });
    }

    if (elementos.chamadoEditarCancelarButton) {
        elementos.chamadoEditarCancelarButton.addEventListener("click", function () {
            fecharModalEdicaoChamado();
        });
    }

    if (elementos.chamadoEditarFecharButton) {
        elementos.chamadoEditarFecharButton.addEventListener("click", function () {
            fecharModalEdicaoChamado();
        });
    }

    if (elementos.chamadoEditarModal) {
        elementos.chamadoEditarModal.addEventListener("click", function (event) {
            if (event.target === elementos.chamadoEditarModal) {
                fecharModalEdicaoChamado();
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
        renderAlertasDominio();
        renderLista();
        renderDetalhe(null);
        renderDetalheLead(null);
        atualizarModoCadastro();
        atualizarModoLead();
        ativarAba(obterAbaInicial());
        definirModoRelatorio("mensal");
        try {
            await gerarRelatorioMensal();
        } catch (error) {
            elementos.reportPreview.innerHTML = "<p>Nao foi possivel carregar o relatorio mensal inicial.</p>";
        }

        try {
            await carregarLeadsBackend();
            setLeadFeedback("Leads carregados do backend.", false);
        } catch (error) {
            setLeadFeedback("Nao foi possivel carregar leads do backend no momento.", true);
        }

        try {
            await carregarChamadosBackend();
            setChamadoFeedback("Chamados carregados do backend.", false);
        } catch (error) {
            setChamadoFeedback("Nao foi possivel carregar chamados do backend no momento.", true);
        }

        renderLeads();
        renderChamados();
    };

    iniciarPainel();
});
