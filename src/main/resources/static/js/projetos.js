(function (root, factory) {
    const api = factory();

    if (typeof module === "object" && module.exports) {
        module.exports = api;
        return;
    }

    root.GestaoProjetos = api;
    root.addEventListener("DOMContentLoaded", function () {
        api.inicializar({
            document: root.document,
            fetch: root.fetch.bind(root),
            confirm: root.confirm.bind(root),
            FormData: root.FormData,
            Quill: root.Quill
        });
    });
}(typeof window !== "undefined" ? window : globalThis, function () {
    function obterElementos(doc) {
        return {
            form: doc.getElementById("projeto-form"),
            titulo: doc.getElementById("projeto-titulo"),
            descricao: doc.getElementById("projeto-descricao"),
            editorContainer: doc.getElementById("projeto-editor-container"),
            editor: doc.getElementById("projeto-editor"),
            editorFeedback: doc.getElementById("projeto-editor-feedback"),
            link: doc.getElementById("projeto-link"),
            imagem: doc.getElementById("projeto-imagem"),
            salvar: doc.getElementById("projeto-salvar"),
            cancelar: doc.getElementById("projeto-cancelar"),
            modo: doc.getElementById("projeto-modo"),
            lista: doc.getElementById("projetos-lista"),
            feedback: doc.getElementById("projeto-feedback")
        };
    }

    function criarPainel(options) {
        const doc = options.document;
        const elementos = obterElementos(doc);
        if (!elementos.form || !elementos.lista) {
            return null;
        }

        let projetos = [];
        let projetoEmEdicaoId = null;
        let quill = null;

        function informarEditor(mensagem) {
            if (elementos.editorFeedback) {
                elementos.editorFeedback.textContent = mensagem;
            }
        }

        function ativarFallbackDoEditor() {
            if (elementos.editorContainer) {
                elementos.editorContainer.hidden = true;
            }
            elementos.descricao.hidden = false;
            elementos.descricao.required = true;
            informarEditor(
                "Editor rico indisponível. Usando editor simples.");
        }

        function inicializarEditor() {
            if (typeof options.Quill !== "function"
                    || !elementos.editorContainer
                    || !elementos.editor) {
                ativarFallbackDoEditor();
                return;
            }

            try {
                quill = new options.Quill(elementos.editor, {
                    theme: "snow",
                    formats: [
                        "header", "bold", "italic", "underline",
                        "list", "blockquote", "link"
                    ],
                    modules: {
                        toolbar: [
                            [{ header: [2, 3, false] }],
                            ["bold", "italic", "underline"],
                            [{ list: "ordered" }, { list: "bullet" }],
                            ["blockquote", "link"],
                            ["clean"]
                        ]
                    }
                });
                elementos.editorContainer.hidden = false;
                elementos.descricao.hidden = true;
                elementos.descricao.required = false;
                informarEditor("");
            } catch (error) {
                quill = null;
                ativarFallbackDoEditor();
            }
        }

        function lerMeta(nome) {
            const meta = doc.querySelector('meta[name="' + nome + '"]');
            return meta ? meta.getAttribute("content") || "" : "";
        }

        const contextoBruto = lerMeta("app-context-path");
        const contexto = contextoBruto === "/"
            ? "" : contextoBruto.replace(/\/$/, "");

        function montarUrl(caminho) {
            return contexto + caminho;
        }

        function definirFeedback(mensagem, erro) {
            elementos.feedback.textContent = mensagem;
            elementos.feedback.className = "obs " + (erro ? "erro" : "sucesso");
        }

        async function mensagemDeErro(response, fallback) {
            try {
                const payload = await response.json();
                return payload && payload.message ? payload.message : fallback;
            } catch (error) {
                return fallback;
            }
        }

        function atualizarModo() {
            const editando = projetoEmEdicaoId !== null;
            elementos.imagem.required = !editando;
            elementos.salvar.textContent = editando
                ? "Atualizar projeto" : "Salvar projeto";
            elementos.cancelar.hidden = !editando;
            elementos.modo.textContent = editando
                ? "Modo atual: edição de projeto."
                : "Modo atual: novo projeto.";
        }

        function preencherDescricao(html) {
            const valor = html || "";
            elementos.descricao.value = valor;
            if (quill) {
                quill.clipboard.dangerouslyPasteHTML(valor);
            }
        }

        function limparDescricao() {
            elementos.descricao.value = "";
            if (quill) {
                quill.setText("");
            }
        }

        function limparFormulario() {
            elementos.form.reset();
            limparDescricao();
            projetoEmEdicaoId = null;
            atualizarModo();
        }

        function entrarEmEdicao(id) {
            const projeto = projetos.find(function (item) {
                return item.id === id;
            });
            if (!projeto) {
                return;
            }

            projetoEmEdicaoId = projeto.id;
            elementos.titulo.value = projeto.titulo || "";
            preencherDescricao(projeto.descricao);
            elementos.link.value = projeto.link || "";
            elementos.imagem.value = "";
            atualizarModo();
            elementos.form.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        }

        function cancelarEdicao() {
            limparFormulario();
            definirFeedback("Edição cancelada.", false);
        }

        async function carregar(silencioso) {
            if (!silencioso) {
                definirFeedback("Carregando projetos...", false);
            }

            let response;
            try {
                response = await options.fetch(montarUrl("/api/projetos"), {
                    method: "GET",
                    headers: {
                        Accept: "application/json"
                    }
                });
            } catch (error) {
                definirFeedback(
                    "Não foi possível carregar os projetos.",
                    true);
                throw error;
            }
            if (!response.ok) {
                const mensagem = await mensagemDeErro(
                    response,
                    "Não foi possível carregar os projetos.");
                definirFeedback(mensagem, true);
                throw new Error(mensagem);
            }

            const payload = await response.json();
            renderizar(Array.isArray(payload) ? payload : []);
            if (!silencioso) {
                definirFeedback("", false);
            }
            return projetos.slice();
        }

        function obterDescricao() {
            if (quill) {
                return {
                    html: quill.root.innerHTML,
                    texto: quill.getText().trim()
                };
            }
            const valor = elementos.descricao.value.trim();
            return { html: valor, texto: valor };
        }

        function dadosDoFormulario() {
            const descricao = obterDescricao();
            return {
                titulo: elementos.titulo.value.trim(),
                descricao: descricao.html,
                descricaoTexto: descricao.texto,
                link: elementos.link.value.trim(),
                imagem: elementos.imagem.files && elementos.imagem.files[0]
                    ? elementos.imagem.files[0] : null
            };
        }

        function possuiTextoVisivel(texto) {
            return String(texto || "")
                .replace(/[\s\u200B-\u200D\u2060\uFEFF]/g, "")
                .length > 0;
        }

        function validar(dados) {
            if (!dados.titulo) {
                return "Informe o título do projeto.";
            }
            if (!possuiTextoVisivel(dados.descricaoTexto)) {
                return "Informe a descrição do projeto.";
            }
            if (!/^https?:\/\/.+/i.test(dados.link)) {
                return "Informe um link iniciado por http:// ou https://.";
            }
            if (projetoEmEdicaoId === null && !dados.imagem) {
                return "Selecione uma imagem para cadastrar o projeto.";
            }
            return "";
        }

        function criarFormData(dados) {
            const formData = new options.FormData();
            formData.append("titulo", dados.titulo);
            formData.append("descricao", dados.descricao);
            formData.append("link", dados.link);
            if (dados.imagem) {
                formData.append("imagem", dados.imagem);
            }
            return formData;
        }

        function headersComCsrf() {
            const headers = {
                Accept: "application/json"
            };
            const token = lerMeta("_csrf");
            const nome = lerMeta("_csrf_header");
            if (token && nome) {
                headers[nome] = token;
            }
            return headers;
        }

        async function salvar(event) {
            if (event && typeof event.preventDefault === "function") {
                event.preventDefault();
            }

            const dados = dadosDoFormulario();
            const erroValidacao = validar(dados);
            if (erroValidacao) {
                definirFeedback(erroValidacao, true);
                return false;
            }

            const editando = projetoEmEdicaoId !== null;
            const caminho = editando
                ? "/api/projetos/" + encodeURIComponent(projetoEmEdicaoId)
                : "/api/projetos";
            elementos.salvar.disabled = true;

            try {
                const response = await options.fetch(montarUrl(caminho), {
                    method: editando ? "PUT" : "POST",
                    headers: headersComCsrf(),
                    body: criarFormData(dados)
                });
                if (!response.ok) {
                    const mensagem = await mensagemDeErro(
                        response,
                        editando
                            ? "Não foi possível atualizar o projeto."
                            : "Não foi possível cadastrar o projeto.");
                    definirFeedback(mensagem, true);
                    return false;
                }

                limparFormulario();
                await carregar(true);
                definirFeedback(
                    editando
                        ? "Projeto atualizado com sucesso."
                        : "Projeto cadastrado com sucesso.",
                    false);
                return true;
            } catch (error) {
                definirFeedback(
                    "Não foi possível comunicar com o servidor.",
                    true);
                return false;
            } finally {
                elementos.salvar.disabled = false;
            }
        }

        async function excluirProjeto(id, botao) {
            const projeto = projetos.find(function (item) {
                return item.id === id;
            });
            if (!projeto) {
                return false;
            }

            const confirmado = options.confirm(
                'Excluir o projeto "'
                + (projeto.titulo || "sem título")
                + '"?');
            if (!confirmado) {
                return false;
            }

            if (botao) {
                botao.disabled = true;
            }
            try {
                const response = await options.fetch(
                    montarUrl("/api/projetos/" + encodeURIComponent(id)),
                    {
                        method: "DELETE",
                        headers: headersComCsrf()
                    });
                if (!response.ok) {
                    const mensagem = await mensagemDeErro(
                        response,
                        "Não foi possível excluir o projeto.");
                    definirFeedback(mensagem, true);
                    return false;
                }

                if (projetoEmEdicaoId === id) {
                    limparFormulario();
                }
                await carregar(true);
                definirFeedback("Projeto excluído com sucesso.", false);
                return true;
            } catch (error) {
                definirFeedback(
                    "Não foi possível comunicar com o servidor.",
                    true);
                return false;
            } finally {
                if (botao) {
                    botao.disabled = false;
                }
            }
        }

        function renderizar(itens) {
            projetos = Array.isArray(itens) ? itens.slice() : [];
            elementos.lista.replaceChildren();

            if (projetos.length === 0) {
                const vazio = doc.createElement("li");
                vazio.className = "projeto-lista-vazia";
                vazio.textContent = "Nenhum projeto cadastrado.";
                elementos.lista.appendChild(vazio);
                return;
            }

            projetos.forEach(function (projeto) {
                const item = doc.createElement("li");
                const titulo = doc.createElement("span");
                const acoes = doc.createElement("div");
                const editar = doc.createElement("button");
                const excluir = doc.createElement("button");

                item.className = "projeto-item";
                titulo.className = "projeto-item-titulo";
                titulo.textContent = projeto.titulo || "Projeto sem título";
                acoes.className = "projeto-item-acoes";

                editar.type = "button";
                editar.className = "projeto-editar";
                editar.textContent = "Editar";
                editar.setAttribute("aria-label", "Editar projeto " + titulo.textContent);
                editar.addEventListener("click", function () {
                    entrarEmEdicao(projeto.id);
                });

                excluir.type = "button";
                excluir.className = "projeto-excluir";
                excluir.textContent = "Excluir";
                excluir.setAttribute("aria-label", "Excluir projeto " + titulo.textContent);
                excluir.addEventListener("click", function () {
                    excluirProjeto(projeto.id, excluir);
                });

                acoes.appendChild(editar);
                acoes.appendChild(excluir);
                item.appendChild(titulo);
                item.appendChild(acoes);
                elementos.lista.appendChild(item);
            });
        }

        function inicializarPainel() {
            elementos.form.addEventListener("submit", salvar);
            elementos.cancelar.addEventListener("click", cancelarEdicao);
            atualizarModo();
            carregar().catch(function () {
                // O feedback já foi apresentado por carregar.
            });
        }

        inicializarEditor();

        return {
            renderizar: renderizar,
            editar: entrarEmEdicao,
            cancelarEdicao: cancelarEdicao,
            carregar: carregar,
            salvar: salvar,
            excluir: excluirProjeto,
            inicializar: inicializarPainel,
            obterEstado: function () {
                return {
                    projetos: projetos.slice(),
                    projetoEmEdicaoId: projetoEmEdicaoId
                };
            }
        };
    }

    function inicializar(options) {
        const painel = criarPainel(options);
        if (painel) {
            painel.inicializar();
        }
        return painel;
    }

    return {
        criarPainel: criarPainel,
        inicializar: inicializar
    };
}));
