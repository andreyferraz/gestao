package com.andreyferraz.gestao.core.utils;

import java.time.LocalDateTime;
import java.util.List;

public class ValidationUtils {

    private ValidationUtils() {
    }

    /**
     * Valida se um campo string não é nulo nem vazio (incluindo espaços)
     */
    public static void validarCampoStringObrigatorio(String campo, String nomeCampo) {
        if (campo == null || campo.trim().isEmpty()) {
            throw new IllegalArgumentException(nomeCampo + " é obrigatório.");
        }
    }

    /**
     * Valida se qualquer campo/objeto não é nulo
     */
    public static void validarCampoObrigatorio(Object campo, String nomeCampo) {
        if (campo == null) {
            throw new IllegalArgumentException(nomeCampo + " é obrigatório.");
        }
    }

    /**
     * Valida se uma lista não é nula nem vazia
     */
    public static void validarListaObrigatoria(List<?> lista, String nomeCampo) {
        if (lista == null || lista.isEmpty()) {
            throw new IllegalArgumentException(nomeCampo + " é obrigatório.");
        }
    }

    /**
     * Valida se um campo data não é nulo
     */
    public static void validarCampoDataObrigatorio(Object campo, LocalDateTime nomeCampo) {
        if (campo == null) {
            throw new IllegalArgumentException(nomeCampo + " é obrigatório.");
        }
    }
}