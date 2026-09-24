(function (root, factory) {
    const api = factory();

    if (typeof module === "object" && module.exports) {
        module.exports = api;
        return;
    }

    root.GestaoPaginacao = api;
}(typeof window !== "undefined" ? window : globalThis, function () {
    const ITENS_POR_PAGINA_PADRAO = 5;

    function calcularTotalPaginas(totalItens, itensPorPagina) {
        const porPagina = Number(itensPorPagina) > 0 ? Number(itensPorPagina) : ITENS_POR_PAGINA_PADRAO;
        const total = Number(totalItens) || 0;
        if (total <= 0) {
            return 1;
        }
        return Math.ceil(total / porPagina);
    }

    function ajustarPagina(paginaAtual, totalPaginas) {
        const paginas = Number(totalPaginas) > 0 ? Number(totalPaginas) : 1;
        let pagina = Number(paginaAtual) || 1;
        if (pagina < 1) {
            pagina = 1;
        }
        if (pagina > paginas) {
            pagina = paginas;
        }
        return pagina;
    }

    function obterItensPagina(itens, paginaAtual, itensPorPagina) {
        if (!Array.isArray(itens)) {
            return [];
        }
        const porPagina = Number(itensPorPagina) > 0 ? Number(itensPorPagina) : ITENS_POR_PAGINA_PADRAO;
        const totalPaginas = calcularTotalPaginas(itens.length, porPagina);
        const pagina = ajustarPagina(paginaAtual, totalPaginas);
        const inicio = (pagina - 1) * porPagina;
        return itens.slice(inicio, inicio + porPagina);
    }

    function calcularJanelaPaginas(paginaAtual, totalPaginas) {
        const total = Number(totalPaginas) > 0 ? Number(totalPaginas) : 1;
        const atual = ajustarPagina(paginaAtual, total);

        if (total <= 7) {
            const paginas = [];
            for (let i = 1; i <= total; i++) {
                paginas.push(i);
            }
            return paginas;
        }

        if (atual <= 4) {
            return [1, 2, 3, 4, 5, "...", total];
        }

        if (atual >= total - 3) {
            return [1, "...", total - 4, total - 3, total - 2, total - 1, total];
        }

        return [1, "...", atual - 1, atual, atual + 1, "...", total];
    }

    function renderizarControles(container, opcoes) {
        if (!container) {
            return;
        }

        const config = opcoes || {};
        const totalItens = Number(config.totalItens) || 0;
        const itensPorPagina = Number(config.itensPorPagina) > 0 ? Number(config.itensPorPagina) : ITENS_POR_PAGINA_PADRAO;
        const totalPaginas = calcularTotalPaginas(totalItens, itensPorPagina);
        const paginaAtual = ajustarPagina(config.paginaAtual, totalPaginas);
        const onMudarPagina = typeof config.onMudarPagina === "function" ? config.onMudarPagina : function () {};
        const doc = config.document || (typeof document !== "undefined" ? document : null);

        if (!doc) {
            return;
        }

        if (totalItens <= itensPorPagina || totalPaginas <= 1) {
            container.hidden = true;
            if (typeof container.replaceChildren === "function") {
                container.replaceChildren();
            } else {
                container.innerHTML = "";
            }
            return;
        }

        container.hidden = false;
        if (typeof container.replaceChildren === "function") {
            container.replaceChildren();
        } else {
            container.innerHTML = "";
        }

        const info = doc.createElement("div");
        info.className = "paginacao-resumo";
        const strongAtual = doc.createElement("strong");
        strongAtual.textContent = String(paginaAtual);
        const strongTotal = doc.createElement("strong");
        strongTotal.textContent = String(totalPaginas);
        info.appendChild(doc.createTextNode("Página "));
        info.appendChild(strongAtual);
        info.appendChild(doc.createTextNode(" de "));
        info.appendChild(strongTotal);
        info.appendChild(doc.createTextNode(" (" + totalItens + " itens)"));

        const acoes = doc.createElement("div");
        acoes.className = "paginacao-acoes";

        const btnAnterior = doc.createElement("button");
        btnAnterior.type = "button";
        btnAnterior.className = "paginacao-btn paginacao-anterior";
        btnAnterior.textContent = "« Anterior";
        btnAnterior.setAttribute("aria-label", "Página anterior");
        btnAnterior.disabled = paginaAtual <= 1;
        btnAnterior.addEventListener("click", function () {
            if (paginaAtual > 1) {
                onMudarPagina(paginaAtual - 1);
            }
        });
        acoes.appendChild(btnAnterior);

        const paginas = calcularJanelaPaginas(paginaAtual, totalPaginas);
        paginas.forEach(function (p) {
            if (p === "...") {
                const reticencias = doc.createElement("span");
                reticencias.className = "paginacao-ellipsis";
                reticencias.setAttribute("aria-hidden", "true");
                reticencias.textContent = "...";
                acoes.appendChild(reticencias);
            } else {
                const btnNum = doc.createElement("button");
                btnNum.type = "button";
                btnNum.className = "paginacao-btn paginacao-numero" + (p === paginaAtual ? " active" : "");
                btnNum.textContent = String(p);
                btnNum.setAttribute("aria-label", "Página " + p);
                if (p === paginaAtual) {
                    btnNum.setAttribute("aria-current", "page");
                }
                btnNum.addEventListener("click", function () {
                    if (p !== paginaAtual) {
                        onMudarPagina(p);
                    }
                });
                acoes.appendChild(btnNum);
            }
        });

        const btnProximo = doc.createElement("button");
        btnProximo.type = "button";
        btnProximo.className = "paginacao-btn paginacao-proximo";
        btnProximo.textContent = "Próximo »";
        btnProximo.setAttribute("aria-label", "Próxima página");
        btnProximo.disabled = paginaAtual >= totalPaginas;
        btnProximo.addEventListener("click", function () {
            if (paginaAtual < totalPaginas) {
                onMudarPagina(paginaAtual + 1);
            }
        });
        acoes.appendChild(btnProximo);

        container.appendChild(info);
        container.appendChild(acoes);
    }

    return {
        ITENS_POR_PAGINA_PADRAO: ITENS_POR_PAGINA_PADRAO,
        calcularTotalPaginas: calcularTotalPaginas,
        ajustarPagina: ajustarPagina,
        obterItensPagina: obterItensPagina,
        calcularJanelaPaginas: calcularJanelaPaginas,
        renderizarControles: renderizarControles
    };
}));
