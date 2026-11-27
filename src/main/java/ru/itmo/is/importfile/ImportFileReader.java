package ru.itmo.is.importfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.util.Set;
import ru.itmo.is.exception.FieldException;
import ru.itmo.is.model.Color;
import ru.itmo.is.model.Country;
import ru.itmo.is.model.Difficulty;

public final class ImportFileReader {

    private static final int MAX_BYTES = 1024 * 1024;
    private static final int MAX_RECORDS = 500;

    private static final Set<Class<?>> ALLOWED = Set.of(
            LabWorkRecord[].class, LabWorkRecord.class, LabWorkRecord.Coordinates.class,
            LabWorkRecord.Discipline.class, LabWorkRecord.Person.class, LabWorkRecord.Location.class,
            Color.class, Country.class, Difficulty.class, Enum.class, String.class,
            Integer.class, Long.class, Double.class, Float.class, Number.class
    );

    private ImportFileReader() {
    }

    public static LabWorkRecord[] read(InputStream body) {
        try {
            byte[] bytes = body.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new FieldException("file", "Файл должен быть не больше 1 МиБ");
            }

            ByteArrayInputStream source = new ByteArrayInputStream(bytes);
            try (ObjectInputStream input = new ObjectInputStream(source)) {
                input.setObjectInputFilter(ImportFileReader::filter);
                if (!(input.readObject() instanceof LabWorkRecord[] records) || source.available() != 0) {
                    throw invalidFormat();
                }
                if (records.length == 0) {
                    throw new FieldException("file", "Файл не содержит лабораторных работ");
                }
                return records;
            }
        } catch (IOException | ClassNotFoundException e) {
            throw invalidFormat();
        }
    }

    private static ObjectInputFilter.Status filter(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 12 || info.references() > 30_000 || info.arrayLength() > MAX_RECORDS) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        return ALLOWED.contains(type) ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
    }

    private static FieldException invalidFormat() {
        return new FieldException("file", "Ожидается .ser-файл с одним массивом из 1–500 записей LabWorkRecord");
    }
}
