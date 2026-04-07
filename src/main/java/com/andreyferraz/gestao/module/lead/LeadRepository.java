package com.andreyferraz.gestao.module.lead;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends CrudRepository<Lead, UUID> {
}
