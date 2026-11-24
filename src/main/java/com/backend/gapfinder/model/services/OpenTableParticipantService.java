package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.enums.NotificationTypeEnum;
import com.backend.gapfinder.model.enums.OpenTableStatusEnum;
import com.backend.gapfinder.model.events.OpenTableEvent;
import com.backend.gapfinder.model.exceptions.AlreadyDoneException;
import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.models.OpenTableModel;
import com.backend.gapfinder.model.models.OpenTableParticipantModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.OpenTableParticipantRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public OpenTableParticipantService(
            OpenTableParticipantRepository openTableParticipantRepository,
            UserService userService,
            OpenTableService openTableService,
            ApplicationEventPublisher eventPublisher) {
        this.openTableParticipantRepository = openTableParticipantRepository;
        this.userService = userService;
        this.openTableService = openTableService;
        this.eventPublisher = eventPublisher;
    }

    // Get a participant record by its id
    @Transactional
    public OpenTableParticipantModel getById(Long id) {
        log.info("Inicia proceso de consultar el participante con id = {}", id);

        return openTableParticipantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El participante con id " + id + " no existe"));
    }

    // The open tables a user joined: one query, so the app does not ask table by table
    @Transactional(readOnly = true)
    public List<OpenTableModel> getJoinedOpenTables(Long userId) {
        log.info("Inicia proceso de consultar las open tables a las que se unió el usuario {}", userId);

        userService.getById(userId);
        return openTableParticipantRepository.findJoinedOpenTables(userId);
    }

    // Get all participant records
    @Transactional
    public List<OpenTableParticipantModel> getAll() {
        log.info("Inicia proceso de consultar todos los participantes");
        return openTableParticipantRepository.findAll();
    }

    // Create a new participant record
    // Adding a participant follows the same rules as joining the table
    @Transactional
    public OpenTableParticipantModel create(OpenTableParticipantModel participant) {
        log.info("Inicia proceso de creación de un registro de participante");

        validateParticipantData(participant);

        OpenTableParticipantModel saved = join(participant.getOpenTable().getId(), participant.getUser().getId());

        log.info("Termina proceso de creación de un registro de participante");

        return saved;
    }

    // Update the data from an existing participant record
    // Moving a participant to another table or user follows the same rules as joining that table
    @Transactional
    public OpenTableParticipantModel update(Long id, OpenTableParticipantModel participant) {
        log.info("Inicia proceso de actualización del participante con id = {}", id);

        OpenTableParticipantModel existente = getById(id);

        validateParticipantData(participant);

        Long newTableId = participant.getOpenTable().getId();
        Long newUserId = participant.getUser().getId();
        OpenTableModel previousTable = existente.getOpenTable();

        boolean sameRecord = previousTable.getId().equals(newTableId)
                && existente.getUser().getId().equals(newUserId);
        if (sameRecord) {
            return existente;
        }

        OpenTableModel openTable = openTableService.getByIdForUpdate(newTableId);
        UserModel user = userService.getById(newUserId);

        validateCanJoin(openTable, user);

        // Moving inside the same table does not take a new spot
        if (!previousTable.getId().equals(newTableId)) {
            validateHasRoom(openTable);
        }

        existente.setOpenTable(openTable);
        existente.setUser(user);
        OpenTableParticipantModel saved = openTableParticipantRepository.save(existente);

        refreshCapacityStatus(previousTable);
        refreshCapacityStatus(openTable);

        log.info("Termina proceso de actualización del participante con id = {}", id);

        return saved;
    }

    // Delete an existing participant record
    @Transactional
    public void delete(Long id) {
        log.info("Inicia proceso de eliminación del participante con id = {}", id);

        OpenTableParticipantModel existente = getById(id);
        OpenTableModel openTable = existente.getOpenTable();

        openTableParticipantRepository.delete(existente);

        // A spot is free again, so a FULL table can go back to OPEN
        refreshCapacityStatus(openTable);

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

    // A user joins an open table and the creator is notified
    @Transactional
    public OpenTableParticipantModel join(Long openTableId, Long userId) {
        log.info("Inicia proceso para que el usuario {} se una a la open table {}", userId, openTableId);

        // The table row stays locked until the join finishes, so the limit is respected
        OpenTableModel openTable = openTableService.getByIdForUpdate(openTableId);
        UserModel user = userService.getById(userId);

        validateCanJoin(openTable, user);

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

        eventPublisher.publishEvent(new OpenTableEvent(
                NotificationTypeEnum.OPEN_TABLE_JOINED,
                openTable.getCreator().getId(),
                user.getName(),
                openTable.getId(),
                openTable.getTitle()));

        log.info("Termina proceso para que el usuario {} se una a la open table {}", userId, openTableId);

        return saved;
    }

    // A user leaves an open table that is still running
    @Transactional
    public void leave(Long openTableId, Long userId) {
        log.info("Inicia proceso para que el usuario {} salga de la open table {}", userId, openTableId);

        OpenTableParticipantModel participant = openTableParticipantRepository
                .findByOpenTableIdAndUserId(openTableId, userId)
                .orElseThrow(() -> new NotFoundException("El usuario no está registrado en esta open table"));

        OpenTableModel openTable = openTableService.getByIdForUpdate(openTableId);

        // A finished table keeps its participants as history
        if (!isActive(openTable)) {
            throw new IllegalStateException("La open table ya terminó, no es posible salir de ella");
        }

        openTableParticipantRepository.delete(participant);

        if (openTable.getStatus() == OpenTableStatusEnum.FULL) {
            openTable.setStatus(OpenTableStatusEnum.OPEN);
        }

        log.info("Termina proceso para que el usuario {} salga de la open table {}", userId, openTableId);
    }

    // Rules to join a table: it is open, it has not ended, the user is not the creator and is not already in it
    private void validateCanJoin(OpenTableModel openTable, UserModel user) {
        if (openTable.getStatus() != OpenTableStatusEnum.OPEN) {
            throw new IllegalStateException("La open table no está abierta");
        }

        if (!openTable.getEndTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("La open table ya terminó");
        }

        if (openTable.getCreator().getId().equals(user.getId())) {
            throw new IllegalStateException("El creador ya hace parte de su propia open table");
        }

        if (openTableParticipantRepository.existsByOpenTableIdAndUserId(openTable.getId(), user.getId())) {
            throw new AlreadyDoneException("El usuario ya está registrado en esta open table");
        }
    }

    // A table has room while its participants are below the limit
    private void validateHasRoom(OpenTableModel openTable) {
        long current = openTableParticipantRepository.countByOpenTableId(openTable.getId());
        if (current >= openTable.getMaxParticipants()) {
            throw new IllegalStateException("La open table está llena");
        }
    }

    // An active table (OPEN or FULL) is FULL only while it has reached its limit
    private void refreshCapacityStatus(OpenTableModel openTable) {
        if (!isActive(openTable)) {
            return;
        }

        long current = openTableParticipantRepository.countByOpenTableId(openTable.getId());
        openTable.setStatus(current >= openTable.getMaxParticipants()
                ? OpenTableStatusEnum.FULL : OpenTableStatusEnum.OPEN);
    }

    // A table is active while it is OPEN or FULL and its end time has not passed
    private boolean isActive(OpenTableModel openTable) {
        boolean activeStatus = openTable.getStatus() == OpenTableStatusEnum.OPEN
                || openTable.getStatus() == OpenTableStatusEnum.FULL;
        return activeStatus && openTable.getEndTime().isAfter(LocalDateTime.now());
    }
}
