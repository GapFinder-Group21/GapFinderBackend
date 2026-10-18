package com.backend.gapfinder.model.services;

import com.backend.gapfinder.model.enums.OpenTableStatusEnum;
import com.backend.gapfinder.model.exceptions.NotFoundException;
import com.backend.gapfinder.model.models.ActivityModel;
import com.backend.gapfinder.model.models.BuildingModel;
import com.backend.gapfinder.model.models.OpenTableModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.OpenTableParticipantRepository;
import com.backend.gapfinder.model.repositories.OpenTableRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OpenTableService {

    private final OpenTableRepository openTableRepository;
    private final OpenTableParticipantRepository openTableParticipantRepository;
    private final UserService userService;
    private final BuildingService buildingService;
    private final ActivityService activityService;
    private final OpenTablePhotoService openTablePhotoService;

    public OpenTableService(OpenTableRepository openTableRepository,
                            OpenTableParticipantRepository openTableParticipantRepository,
                            UserService userService,
                            BuildingService buildingService,
                            ActivityService activityService,
                            OpenTablePhotoService openTablePhotoService) {
        this.openTableRepository = openTableRepository;
        this.openTableParticipantRepository = openTableParticipantRepository;
        this.userService = userService;
        this.buildingService = buildingService;
        this.activityService = activityService;
        this.openTablePhotoService = openTablePhotoService;
    }

    // Get an open table by its id
    @Transactional
    public OpenTableModel getById(Long id) {
        log.info("Inicia proceso de consultar la open table con id = {}", id);
        return openTableRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La open table con id " + id + " no existe"));
    }

    // Get all open tables
    @Transactional
    public List<OpenTableModel> getAll() {
        log.info("Inicia proceso de consultar todas las open tables");
        return openTableRepository.findAll();
    }

    // Create a new open table with its photo (the photo is required)
    @Transactional
    public OpenTableModel create(OpenTableModel openTable, MultipartFile photo) {
        log.info("Inicia proceso de creación de una open table con título = {}", openTable.getTitle());

        validateOpenTableData(openTable);

        // A new table must still be running when it is published
        if (!openTable.getEndTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La hora de fin de la open table ya pasó");
        }

        UserModel creator = userService.getById(openTable.getCreator().getId());
        BuildingModel building = buildingService.getById(openTable.getBuilding().getId());
        ActivityModel activity = resolveActivity(openTable.getActivity());

        openTable.setId(null);
        openTable.setCreator(creator);
        openTable.setBuilding(building);
        openTable.setActivity(activity);
        // Every table starts OPEN; joins, leaves and the scheduler move it to the other states
        openTable.setStatus(OpenTableStatusEnum.OPEN);
        openTable.setCreatedAt(LocalDateTime.now());

        // The photo is saved after the data is valid, so invalid tables leave no files behind
        String photoUrl = openTablePhotoService.store(photo);
        openTable.setPhotoUrl(photoUrl);

        try {
            OpenTableModel saved = openTableRepository.save(openTable);
            log.info("Termina proceso de creación de una open table con título = {}", openTable.getTitle());
            return saved;
        } catch (RuntimeException e) {
            // The table was not saved, so its photo is removed
            openTablePhotoService.delete(photoUrl);
            throw e;
        }
    }

    // Update the data from an existing open table
    @Transactional
    public OpenTableModel update(Long id, OpenTableModel openTable) {
        log.info("Inicia proceso de actualización de la open table con id = {}", id);

        OpenTableModel existente = getById(id);
        validateOpenTableData(openTable);

        // The limit cannot go below the students already in the table
        long participants = openTableParticipantRepository.countByOpenTableId(id);
        if (openTable.getMaxParticipants() < participants) {
            throw new IllegalArgumentException(
                    "El máximo de participantes no puede ser menor a los " + participants + " inscritos");
        }

        BuildingModel building = buildingService.getById(openTable.getBuilding().getId());
        ActivityModel activity = resolveActivity(openTable.getActivity());

        // The creator and the status are not edited here: the creator owns the table
        // and the status follows the participants and the table times
        existente.setBuilding(building);
        existente.setActivity(activity);
        existente.setTitle(openTable.getTitle());
        existente.setDescription(openTable.getDescription());
        existente.setStartTime(openTable.getStartTime());
        existente.setEndTime(openTable.getEndTime());
        existente.setMaxParticipants(openTable.getMaxParticipants());

        // An active table is FULL only while it has reached its limit
        if (existente.getStatus() == OpenTableStatusEnum.OPEN || existente.getStatus() == OpenTableStatusEnum.FULL) {
            existente.setStatus(participants >= existente.getMaxParticipants()
                    ? OpenTableStatusEnum.FULL : OpenTableStatusEnum.OPEN);
        }

        log.info("Termina proceso de actualización de la open table con id = {}", id);
        return openTableRepository.save(existente);
    }

    // Delete an existing open table
    @Transactional
    public void delete(Long id) {
        log.info("Inicia proceso de eliminación de la open table con id = {}", id);

        OpenTableModel existente = getById(id);

        // The participants belong to the table, so they are removed with it
        openTableParticipantRepository.deleteAll(openTableParticipantRepository.findByOpenTableId(id));
        openTableRepository.delete(existente);

        log.info("Termina proceso de eliminación de la open table con id = {}", id);
    }

    // Close the open tables whose end time has passed (COMPLETED or EMPTY), runs every minute
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void closeExpiredTables() {
        List<OpenTableModel> expired = openTableRepository.findByStatusInAndEndTimeBefore(
                List.of(OpenTableStatusEnum.OPEN, OpenTableStatusEnum.FULL),
                LocalDateTime.now());

        // Runs every minute, so it only logs when there is something to close
        if (expired.isEmpty()) {
            return;
        }

        log.info("Inicia proceso de cierre de open tables vencidas");

        for (OpenTableModel table : expired) {
            long participants = openTableParticipantRepository.countByOpenTableId(table.getId());
            table.setStatus(participants >= 1 ? OpenTableStatusEnum.COMPLETED : OpenTableStatusEnum.EMPTY);
        }

        log.info("Termina proceso de cierre de open tables vencidas, cerradas = {}", expired.size());
    }

    // Validate that the open table data is correct
    private void validateOpenTableData(OpenTableModel openTable) {
        if (openTable.getCreator() == null || openTable.getCreator().getId() == null) {
            throw new IllegalArgumentException("Debe indicar el usuario creador (creator)");
        }

        if (openTable.getBuilding() == null || openTable.getBuilding().getId() == null) {
            throw new IllegalArgumentException("Debe indicar el edificio (building)");
        }

        if (openTable.getTitle() == null || openTable.getTitle().isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }

        if (openTable.getStartTime() == null || openTable.getEndTime() == null) {
            throw new IllegalArgumentException("La hora de inicio y fin son obligatorias");
        }

        if (!openTable.getStartTime().isBefore(openTable.getEndTime())) {
            throw new IllegalArgumentException("La hora de inicio debe ser antes de la hora de fin");
        }

        if (openTable.getMaxParticipants() == null || openTable.getMaxParticipants() <= 0) {
            throw new IllegalArgumentException("El número máximo de participantes debe ser positivo");
        }
    }

    // Get an open table locking it, used when its participants change
    @Transactional
    public OpenTableModel getByIdForUpdate(Long id) {
        return openTableRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("La open table con id " + id + " no existe"));
    }

    // Resolve the activity from the id provided
    private ActivityModel resolveActivity(ActivityModel activity) {
        if (activity == null || activity.getId() == null) {
            throw new IllegalArgumentException("Debe indicar la actividad (activity)");
        }
        return activityService.getById(activity.getId());
    }

    // Count the open tables created since the given date (used for the abandonment statistics)
    @Transactional(readOnly = true)
    public long countCreatedSince(LocalDateTime since) {
        log.info("Inicia proceso de contar las open tables creadas desde {}", since);

        long count = openTableRepository.countByCreatedAtGreaterThanEqual(since);

        log.info("Termina proceso de contar las open tables creadas desde {}", since);
        return count;
    }
}