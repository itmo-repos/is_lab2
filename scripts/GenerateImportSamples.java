import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import ru.itmo.is.importfile.LabWorkRecord;
import ru.itmo.is.model.Color;
import ru.itmo.is.model.Country;
import ru.itmo.is.model.Difficulty;

public class GenerateImportSamples {

    public static void main(String[] args) throws IOException {
        Path output = Path.of(args[0]);
        Files.createDirectories(output);

        var unnamed = new LabWorkRecord.Location(40, 50L, null);
        var author = person("Тестовый автор", unnamed);
        var discipline = new LabWorkRecord.Discipline("Тестовая дисциплина", 12, 4);
        var draft = lab("Черновик", 5L, null, discipline, author);

        write(output, "success.ser", demoRecords());
        write(output, "invalid-second.ser", new LabWorkRecord[] {
                draft, lab("Некорректные координаты", 578L, 12F, discipline, author)
        });
        write(output, "duplicate-draft.ser", new LabWorkRecord[] {
                draft, lab("Ещё один черновик", 6L, null, discipline, author)
        });
        write(output, "duplicate-location.ser", new LabWorkRecord[] {
                draft,
                lab("Повтор локации", 6L, 12F, discipline,
                        person("Другой автор", new LabWorkRecord.Location(40, 50L, " ")))
        });
        write(output, "duplicate-person.ser", new LabWorkRecord[] {
                draft, lab("Повтор автора", 6L, 12F, discipline, person("Тестовый автор", unnamed))
        });
        write(output, "empty.ser", new LabWorkRecord[0]);
        write(output, "wrong-type.ser", new String[] {"Это сериализованный массив другого типа"});
    }

    private static LabWorkRecord[] demoRecords() {
        LabWorkRecord.Discipline[] disciplines = {
                new LabWorkRecord.Discipline("Информационные системы", 32, 6),
                new LabWorkRecord.Discipline("Базы данных", 36, 6),
                new LabWorkRecord.Discipline("Веб-программирование", 28, 6),
                new LabWorkRecord.Discipline("Операционные системы", 24, 6),
                new LabWorkRecord.Discipline("Анализ данных", 32, 6)
        };
        LabWorkRecord.Person[] authors = {
                new LabWorkRecord.Person("Анна Соколова", Color.GREEN, Color.BROWN,
                        new LabWorkRecord.Location(12, 18L, "Главный корпус"), 168L, Country.USA),
                new LabWorkRecord.Person("Михаил Волков", Color.BLUE, Color.WHITE,
                        new LabWorkRecord.Location(20, 25L, "Учебная лаборатория"), 182L, Country.UNITED_KINGDOM),
                new LabWorkRecord.Person("Елена Морозова", Color.BROWN, null,
                        new LabWorkRecord.Location(30, 42L, null), 172L, Country.USA),
                new LabWorkRecord.Person("Илья Кузнецов", Color.BLUE, Color.BROWN, null,
                        178L, Country.VATICAN),
                new LabWorkRecord.Person("Ольга Лебедева", Color.GREEN, null,
                        new LabWorkRecord.Location(12, 18L, "Библиотека"), 164L, Country.UNITED_KINGDOM),
                new LabWorkRecord.Person("Даниил Орлов", Color.BROWN, Color.BROWN,
                        new LabWorkRecord.Location(47, 11L, "Коворкинг"), 185L, Country.USA)
        };
        String[][] topics = {
                {"Модель предметной области", "Управление справочниками", "Проверка бизнес-правил",
                        "История операций", "Пакетная обработка", "Роли пользователей"},
                {"Проектирование схемы", "Соединения таблиц", "Анализ плана запроса",
                        "Индексы", "Управление транзакциями", "Резервное копирование"},
                {"Обработка HTML-форм", "REST API", "Управление сессией",
                        "События сервера", "Клиентская валидация", "Маршрутизация"},
                {"Планирование процессов", "Системные вызовы", "Потоки и синхронизация",
                        "Виртуальная память", "Файловая система", "Обработка сигналов"},
                {"Подготовка выборки", "Линейная регрессия", "Ближайшие соседи",
                        "Кластеризация", "Оценка качества", "Визуализация результатов"}
        };

        LabWorkRecord[] records = new LabWorkRecord[disciplines.length * authors.length];
        Difficulty[] difficulties = Difficulty.values();
        for (int group = 0; group < disciplines.length; group++) {
            for (int author = 0; author < authors.length; author++) {
                int i = group * authors.length + author;
                String topic = topics[group][author];
                Float point = i % 7 == 0 ? null : (float) (5 + i * 11 % 26);
                Long qualities = i % 4 == 0 ? null : 1L + i % 9;
                records[i] = new LabWorkRecord(topic, "Практическая работа по теме «" + topic + "».",
                        difficulties[(group + author) % difficulties.length], point, qualities,
                        new LabWorkRecord.Coordinates(group * 24L + author * 3L - 38,
                                (group - 2) * 20.0 + author * (1.5 + group)),
                        i == records.length - 1 ? null : disciplines[group], authors[author]);
            }
        }
        return records;
    }

    private static LabWorkRecord.Person person(String name, LabWorkRecord.Location location) {
        return new LabWorkRecord.Person(name, Color.GREEN, null, location, 170L, Country.USA);
    }

    private static LabWorkRecord lab(String name, long x, Float point, LabWorkRecord.Discipline discipline,
                                     LabWorkRecord.Person author) {
        return new LabWorkRecord(name, "Запись для проверки импорта", Difficulty.HARD, point, null,
                new LabWorkRecord.Coordinates(x, 2.5), discipline, author);
    }

    private static void write(Path output, String name, Object value) throws IOException {
        Path path = output.resolve(name);
        try (var stream = new ObjectOutputStream(Files.newOutputStream(path))) {
            stream.writeObject(value);
        }
        System.out.println(path);
    }
}
