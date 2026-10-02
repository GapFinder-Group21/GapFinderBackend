package com.backend.gapfinder.services;

import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.InterestModel;
import com.backend.gapfinder.repositories.InterestRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class InterestService {

    private final InterestRepository interestRepository;

    public InterestService(InterestRepository interestRepository) {
        this.interestRepository = interestRepository;
    }

    // Get an interest by its id
    @Transactional
    public InterestModel getById(Long id) {
        log.info("Inicia proceso de consultar el interés con id = {}", id);
        return interestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El interés con id " + id + " no existe"));
    }

    // Get all interests
    @Transactional
    public List<InterestModel> getAll() {
        log.info("Inicia proceso de consultar todos los intereses");
        return interestRepository.findAll();
    }

    // Create a new interest
    @Transactional
    public InterestModel create(InterestModel interest) {
        log.info("Inicia proceso de creación de un interés con nombre = {}", interest.getName());

        validateInterestData(interest);

        boolean yaExiste = interestRepository.existsByName(interest.getName());
        if (yaExiste) {
            throw new IllegalArgumentException("Ya existe un interés con el nombre: " + interest.getName());
        }

        interest.setId(null);

        log.info("Termina proceso de creación de un interés con nombre = {}", interest.getName());
        return interestRepository.save(interest);
    }

    // Update the data from an existing interest
    @Transactional
    public InterestModel update(Long id, InterestModel interest) {
        log.info("Inicia proceso de actualización del interés con id = {}", id);

        InterestModel existente = getById(id);
        validateInterestData(interest);

        existente.setName(interest.getName());

        log.info("Termina proceso de actualización del interés con id = {}", id);
        return interestRepository.save(existente);
    }

    // Delete an existing interest
    @Transactional
    public void delete(Long id) {
        log.info("Inicia proceso de eliminación del interés con id = {}", id);

        InterestModel existente = getById(id);
        interestRepository.delete(existente);

        log.info("Termina proceso de eliminación del interés con id = {}", id);
    }

    // Validate that the interest data is correct
    private void validateInterestData(InterestModel interest) {
        if (interest.getName() == null || interest.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del interés es obligatorio");
        }
    }
}