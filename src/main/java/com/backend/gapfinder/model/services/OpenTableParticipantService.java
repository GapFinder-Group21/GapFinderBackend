package com.backend.gapfinder.services;

import com.backend.gapfinder.enums.OpenTableStatusEnum;
import com.backend.gapfinder.exceptions.NotFoundException;
import com.backend.gapfinder.models.OpenTableModel;
import com.backend.gapfinder.models.OpenTableParticipantModel;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.repositories.OpenTableParticipantRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OpenTableParticipantService {

    private final OpenTableParticipantRepository openTableParticipantRepository;
    private final UserService userService;
    private final OpenTableService openTableService;

    public OpenTableParticipantService(
            OpenTableParticipantRepository openTableParticipantRepository,
            UserService userService,
            OpenTableService openTableService) {
        this.openTableParticipantRepository = openTableParticipantRepository;
        this.userService = userService;
        this.openTableService = openTableService;
    }

    // Get a participant record by its id
    @Transactional
    public OpenTableParticipantModel getById(Long id) {
        log.info("Inicia proceso de consultar el participante con id = {}", id);

        return openTableParticipantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El participante con id " + id + " no existe"));
    }

    // Get all participant records
    @Transactional
    public List<OpenTableParticipantModel> getAll() {
        log.info("Inicia proceso de consultar todos los participantes");
        return openTableParticipantRepository.findAll();
    }

    // Create a new participant record
    @Transactional
    public OpenTableParticipantModel create(OpenTableParticipantModel participant) {
        log.info("Inicia proceso de creación de un registro de participante");

        validateParticipantData(participant);

        OpenTableModel openTable = openTableService.getById(participant.getOpenTable().getId());
        UserModel user = userService.getById(participant.getUser().getId());

        boolean yaExiste = openTableParticipantRepository.existsByOpenTableIdAndUserId(
                openTable.getId(),
                user.getId());

        if (yaExiste) {
            throw new IllegalArgumentException("El usuario ya está registrado en esta open table");
        }

        participant.setId(null);
        participant.setOpenTable(openTable);
        participant.setUser(user);
        participant.setJoinedAt(LocalDateTime.now());

        OpenTableParticipantModel saved = openTableParticipantRepository.save(participant);

        log.info("Termina proceso de creación de un registro de participante");

        return saved;
    }

    // Update the data from an existing participant record
    @Transactional
    public OpenTableParticipantModel update(Long id, OpenTableParticipantModel participant) {
        log.info("Inicia proceso de actualización del participante con id = {}", id);

        OpenTableParticipantModel existente = getById(id);

        validateParticipantData(participant);

        OpenTableModel openTable = openTableService.getById(participant.getOpenTable().getId());
        UserModel user = userService.getById(participant.getUser().getId());

        existente.setOpenTable(openTable);
        existente.setUser(user);

        log.info("Termina proceso de actualización del participante con id = {}", id);

        return openTableParticipantRepository.save(existente);
    }

    // Delete an existing participant record
    @Transactional
    public void delete(Long id) {
        log.info("Inicia proceso de eliminación del participante con id = {}", id);

        OpenTableParticipantModel existente = getById(id);

        openTableParticipantRepository.delete(existente);

        log.info("Termina proceso de eliminación del participante con id = {}", id);
    }

    // Validate that the participant data is correct
    private void validateParticipantData(OpenTableParticipantModel participant) {
        if (participant.getOpenTable() == null || participant.getOpenTable().getId() == null) {
            throw new IllegalArgumentException("Debe indicar la open table (openTable)");
        }

        if (participant.getUser() == null || participant.getUser().getId() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario (user)");
        }
    }

    // Get all participants of a given open table
    @Transactional
    public List<OpenTableParticipantModel> getByOpenTable(Long openTableId) {
        log.info("Inicia proceso de consultar los participantes de la open table con id = {}", openTableId);

        openTableService.getById(openTableId); // lanza NotFoundException si no existe

        return openTableParticipantRepository.findByOpenTableId(openTableId);
    }

    // A user joins an open table
    @Transactional
    public OpenTableParticipantModel join(Long openTableId, Long userId) {
        log.info("Inicia proceso para que el usuario {} se una a la open table {}", userId, openTableId);

        OpenTableModel openTable = openTableService.getById(openTableId);
        UserModel user = userService.getById(userId);

        if (openTable.getStatus() != OpenTableStatusEnum.OPEN) {
            throw new IllegalStateException("La open table no está abierta");
        }

        if (openTableParticipantRepository.existsByOpenTableIdAndUserId(openTableId, userId)) {
            throw new IllegalStateException("El usuario ya está registrado en esta open table");
        }

        long current = openTableParticipantRepository.countByOpenTableId(openTableId);

        if (current >= openTable.getMaxParticipants()) {
            throw new IllegalStateException("La open table está llena");
        }

        OpenTableParticipantModel participant = new OpenTableParticipantModel();
        participant.setOpenTable(openTable);
        participant.setUser(user);
        participant.setJoinedAt(LocalDateTime.now());

        OpenTableParticipantModel saved = openTableParticipantRepository.save(participant);

        if (current + 1 >= openTable.getMaxParticipants()) {
            openTable.setStatus(OpenTableStatusEnum.FULL); // entidad gestionada, se guarda con la transacción
        }

        log.info("Termina proceso para que el usuario {} se una a la open table {}", userId, openTableId);

        return saved;
    }

    // A user leaves an open table
    @Transactional
    public void leave(Long openTableId, Long userId) {
        log.info("Inicia proceso para que el usuario {} salga de la open table {}", userId, openTableId);

        OpenTableParticipantModel participant = openTableParticipantRepository
                .findByOpenTableIdAndUserId(openTableId, userId)
                .orElseThrow(() -> new NotFoundException("El usuario no está registrado en esta open table"));

        OpenTableModel openTable = participant.getOpenTable();

        openTableParticipantRepository.delete(participant);

        if (openTable.getStatus() == OpenTableStatusEnum.FULL) {
            openTable.setStatus(OpenTableStatusEnum.OPEN);
        }

        log.info("Termina proceso para que el usuario {} salga de la open table {}", userId, openTableId);
    }
}