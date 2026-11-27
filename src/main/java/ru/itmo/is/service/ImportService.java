package ru.itmo.is.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Validator;
import java.util.IdentityHashMap;
import java.util.Map;
import ru.itmo.is.auth.Account;
import ru.itmo.is.dto.CoordinatesDto;
import ru.itmo.is.dto.DisciplineDto;
import ru.itmo.is.dto.ImportOperationDto;
import ru.itmo.is.dto.LabWorkDto;
import ru.itmo.is.dto.LocationDto;
import ru.itmo.is.dto.PersonDto;
import ru.itmo.is.exception.ConflictException;
import ru.itmo.is.exception.FieldException;
import ru.itmo.is.importfile.LabWorkRecord;

@ApplicationScoped
public class ImportService {

    @Inject
    Validator validator;
    @Inject
    CoordinatesService coordinates;
    @Inject
    DisciplineService disciplines;
    @Inject
    LocationService locations;
    @Inject
    PersonService persons;
    @Inject
    LabWorkService labWorks;
    @Inject
    ImportHistoryService history;
    @Inject
    UniquenessLock uniquenessLock;

    @Transactional
    public ImportOperationDto save(Account account, LabWorkRecord[] records) {
        uniquenessLock.acquire(UniquenessLock.Rule.DRAFT);
        uniquenessLock.acquire(UniquenessLock.Rule.UNNAMED_LOCATION);
        uniquenessLock.acquire(UniquenessLock.Rule.PERSON);

        // Один объект, на который в файле ссылаются несколько записей, сохраняется один раз.
        Map<LabWorkRecord.Coordinates, Long> coordinateIds = new IdentityHashMap<>();
        Map<LabWorkRecord.Discipline, Long> disciplineIds = new IdentityHashMap<>();
        Map<LabWorkRecord.Location, Long> locationIds = new IdentityHashMap<>();
        Map<LabWorkRecord.Person, Long> personIds = new IdentityHashMap<>();

        for (int i = 0; i < records.length; i++) {
            try {
                LabWorkRecord record = records[i];
                if (record == null || record.coordinates() == null || record.author() == null) {
                    throw new FieldException("file", "Запись, координаты и автор обязательны");
                }

                Long coordinateId = coordinateIds.computeIfAbsent(record.coordinates(), this::createCoordinates);
                Long disciplineId = record.discipline() == null ? null
                        : disciplineIds.computeIfAbsent(record.discipline(), this::createDiscipline);
                Long authorId = personIds.computeIfAbsent(record.author(), author -> {
                    Long locationId = author.location() == null ? null
                            : locationIds.computeIfAbsent(author.location(), this::createLocation);
                    checkText("author.name", author.name(), 255);
                    return persons.create(validate("author", new PersonDto(null, author.name(), author.eyeColor(),
                            author.hairColor(), locationId, author.height(), author.nationality()))).id();
                });

                checkText("name", record.name(), 255);
                checkText("description", record.description(), Integer.MAX_VALUE);
                if (record.minimalPoint() != null && !Float.isFinite(record.minimalPoint())) {
                    throw new FieldException("minimalPoint", "Минимальный балл должен быть конечным числом");
                }
                labWorks.create(validate("", new LabWorkDto(null, record.name(), coordinateId, null,
                        record.description(), record.difficulty(), disciplineId, record.minimalPoint(),
                        record.personalQualitiesMinimum(), authorId)));
            } catch (FieldException e) {
                throw new FieldException("file", "Запись " + (i + 1) + ". " + e.getMessage());
            } catch (ConflictException e) {
                throw new ConflictException("Запись " + (i + 1) + ". " + e.getMessage());
            }
        }
        return history.recordSuccess(account, records.length);
    }

    private Long createCoordinates(LabWorkRecord.Coordinates value) {
        if (value.y() != null && !Double.isFinite(value.y())) {
            throw new FieldException("coordinates.y", "Координата y должна быть конечным числом");
        }
        return coordinates.create(validate("coordinates", new CoordinatesDto(null, value.x(), value.y()))).id();
    }

    private Long createDiscipline(LabWorkRecord.Discipline value) {
        checkText("discipline.name", value.name(), 255);
        return disciplines.create(validate("discipline", new DisciplineDto(null, value.name(),
                value.practiceHours(), value.labsCount()))).id();
    }

    private Long createLocation(LabWorkRecord.Location value) {
        checkText("author.location.name", value.name(), 255);
        return locations.create(validate("author.location", new LocationDto(null, value.x(), value.y(), value.name()))).id();
    }

    private void checkText(String field, String value, int maxLength) {
        if (value != null && (value.indexOf(0) >= 0 || value.codePointCount(0, value.length()) > maxLength)) {
            throw new FieldException(field, field + " содержит недопустимый текст");
        }
    }

    private <T> T validate(String prefix, T dto) {
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            var violation = violations.iterator().next();
            String field = (prefix.isEmpty() ? "" : prefix + ".") + violation.getPropertyPath();
            throw new FieldException(field, field + " " + violation.getMessage());
        }
        return dto;
    }
}
