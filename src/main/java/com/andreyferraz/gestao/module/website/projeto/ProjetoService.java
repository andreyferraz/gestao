package com.andreyferraz.gestao.module.website.projeto;

import org.springframework.stereotype.Service;

@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;

    public ProjetoService(ProjetoRepository projetoRepository) {
        this.projetoRepository = projetoRepository;
    }

}
