package ru.itmo.is.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;
import ru.itmo.is.dto.LabWorkDetailsDto;
import ru.itmo.is.dto.LabWorkDto;
import ru.itmo.is.dto.PageDto;
import ru.itmo.is.event.ChangedEvent;
import ru.itmo.is.exception.ConflictException;
import ru.itmo.is.exception.FieldException;
import ru.itmo.is.model.Coordinates;
import ru.itmo.is.model.Discipline;
import ru.itmo.is.model.LabWork;
import ru.itmo.is.model.Person;
import ru.itmo.is.repository.BaseRepository;
import ru.itmo.is.repository.CoordinatesRepository;
import ru.itmo.is.repository.DisciplineRepository;
import ru.itmo.is.repository.LabWorkRepository;
import ru.itmo.is.repository.PersonRepository;

@ApplicationScoped
public class LabWorkService {

    private static final int MAX_PAGE_SIZE = 100;

    @Inject
    LabWorkRepository repository;

    @Inject
    CoordinatesRepository coordinatesRepository;

    @Inject
    DisciplineRepository disciplineRepository;

    @Inject
    PersonRepository personRepository;

    @Inject
    CoordinatesService coordinatesService;

    @Inject
    DisciplineService disciplineService;

    @Inject
    PersonService personService;

    @Inject
    UniquenessLock uniquenessLock;

    @Inject
    Event<ChangedEvent> changes;

    @Transactional
    public LabWorkDto create(LabWorkDto dto) {
        uniquenessLock.acquire();
        LabWork entity = new LabWork();
        fill(entity, dto);
        repository.save(entity);
        changes.fire(new ChangedEvent("labwork"));
        return toDto(entity);
    }

    public LabWorkDetailsDto get(Integer id) {
        return toDetailsDto(findOrThrow(id));
    }

    public PageDto<LabWorkDetailsDto> list(int page, int size, String sort, String order,
                                           Map<String, String> filters) {
        if (page < 0) {
            throw new FieldException("page", "Номер страницы не может быть отрицательным");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new FieldException("size", "Размер страницы должен быть от 1 до " + MAX_PAGE_SIZE);
        }
        if (!LabWorkRepository.SORTABLE.contains(sort)) {
            throw new FieldException("sort", "Сортировка по колонке " + sort + " не поддерживается");
        }
        if (!order.equals("asc") && !order.equals("desc")) {
            throw new FieldException("order", "Допустимые значения: asc, desc");
        }
        for (String column : filters.keySet()) {
            if (!LabWorkRepository.FILTERABLE.contains(column)) {
                throw new FieldException(column, "Фильтр по этой колонке не поддерживается");
            }
        }
        List<LabWorkDetailsDto> items = repository.findPage(filters, sort, order.equals("asc"), page, size)
                .stream().map(this::toDetailsDto).toList();
        return new PageDto<>(items, repository.count(filters), page, size);
    }

    @Transactional
    public LabWorkDto update(Integer id, LabWorkDto dto) {
        uniquenessLock.acquire();
        LabWork entity = findOrThrow(id);
        fill(entity, dto);
        changes.fire(new ChangedEvent("labwork"));
        return toDto(entity);
    }

    @Transactional
    public void delete(Integer id) {
        uniquenessLock.acquire();
        repository.delete(findOrThrow(id));
        changes.fire(new ChangedEvent("labwork"));
    }

    void requireUniqueDraft(Integer id, Long authorId, Long disciplineId, Float minimalPoint) {
        if (minimalPoint == null && disciplineId != null && repository.hasOtherDraft(id, authorId, disciplineId)) {
            throw new ConflictException("У автора уже есть черновик по этой дисциплине");
        }
    }

    private LabWork findOrThrow(Integer id) {
        LabWork entity = repository.find(id);
        if (entity == null) {
            throw new NotFoundException("LabWork " + id + " не найдена");
        }
        return entity;
    }

    private void fill(LabWork entity, LabWorkDto dto) {
        Coordinates coordinates = requireRef(coordinatesRepository, dto.coordinatesId(), "coordinatesId");
        Discipline discipline = dto.disciplineId() == null ? null
                : requireRef(disciplineRepository, dto.disciplineId(), "disciplineId");
        Person author = requireRef(personRepository, dto.authorId(), "authorId");
        requireUniqueDraft(entity.getId(), dto.authorId(), dto.disciplineId(), dto.minimalPoint()); // Если это черновик, то хотя бы уникальный

        entity.setName(dto.name());
        entity.setCoordinates(coordinates);
        entity.setDescription(dto.description());
        entity.setDifficulty(dto.difficulty());
        entity.setDiscipline(discipline);
        entity.setMinimalPoint(dto.minimalPoint());
        entity.setPersonalQualitiesMinimum(dto.personalQualitiesMinimum());
        entity.setAuthor(author);
    }

    private <T> T requireRef(BaseRepository<T, Long> refRepository, Long id, String field) {
        T entity = refRepository.find(id);
        if (entity == null) {
            throw new FieldException(field, "Объект с id " + id + " не найден");
        }
        return entity;
    }

    public LabWorkDto toDto(LabWork entity) {
        return new LabWorkDto(entity.getId(), entity.getName(), entity.getCoordinates().getId(),
                entity.getCreationDate(), entity.getDescription(), entity.getDifficulty(),
                entity.getDiscipline() == null ? null : entity.getDiscipline().getId(),
                entity.getMinimalPoint(), entity.getPersonalQualitiesMinimum(),
                entity.getAuthor().getId());
    }

    private LabWorkDetailsDto toDetailsDto(LabWork entity) {
        return new LabWorkDetailsDto(entity.getId(), entity.getName(),
                coordinatesService.toDto(entity.getCoordinates()),
                entity.getCreationDate(), entity.getDescription(), entity.getDifficulty(),
                entity.getDiscipline() == null ? null : disciplineService.toDto(entity.getDiscipline()),
                entity.getMinimalPoint(), entity.getPersonalQualitiesMinimum(),
                personService.toDetailsDto(entity.getAuthor()));
    }
}
