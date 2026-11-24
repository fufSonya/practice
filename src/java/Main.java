import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Main {
    public static void main(String[] args) {
        LocalDate date = LocalDate.parse("2026-10-20");
        Indicator ph = new Indicator("pH");
        Indicator turbidity = new Indicator("Мутность");
        List<Standard> standards = List.of(
                new Standard(ph, 6.0, 9.0),
                new Standard(turbidity, 0.0, 2.0)
        );
        Employee responsible = new ResponsibleEmployee(101);
        Control control = new Control(1, date, responsible);
        control.registerResults(List.of(
                new Sample(1, date, List.of(new Reading(ph, 7.2), new Reading(turbidity, 2.2))),
                new Sample(2, date, List.of(new Reading(ph, 8.1), new Reading(turbidity, 2.5)))
        ));

        System.out.println("1. Проверка входных данных");
        List<String> errors = control.checkData(standards);
        if (!errors.isEmpty()) {
            errors.forEach(System.out::println);
            return;
        }
        System.out.println("Данные корректны.");
        System.out.println();

        System.out.println("2. Проверка качества воды");
        Map<Sample, List<MeasurementResult>> results = control.compareWithStandards(standards);
        results.forEach((sample, measurements) -> {
            System.out.println("Проба " + sample.id() + ":");
            measurements.forEach(result -> System.out.println("  " + result.indicator().name()
                    + ": " + result.value() + " — " + result.status()));
        });
        System.out.println();

        System.out.println("3. Поиск повторных отклонений");
        Map<String, Integer> repeated = new QualityAnalyzer().findRepeatedDeviations(control.samples(), standards);
        if (repeated.isEmpty()) {
            System.out.println("Повторных отклонений нет.");
        } else {
            repeated.forEach((name, count) -> System.out.println(name + ": " + count + " отклонения"));
        }
        System.out.println();

        System.out.println("4. Формирование уведомления");
        Notice notice = control.createNonconformityNotice(results, repeated);
        if (notice == null) {
            System.out.println("Уведомление не требуется.");
        } else {
            System.out.println(notice.message());
            System.out.println("Количество отклонений: " + notice.deviations().size());
        }
        System.out.println();

        System.out.println("5. Сохранение истории контроля");
        List<Decision> history = new ArrayList<>();
        Decision decision = control.saveControlHistory(results, notice, history);
        System.out.println("Решение: " + decision.status());
        System.out.println("Записей в истории: " + history.size());
    }
}

class Employee {
    private final int id;

    Employee(int id) {
        this.id = id;
    }

    int id() {
        return id;
    }

    boolean canApprove() {
        return false;
    }
}

final class ResponsibleEmployee extends Employee {
    ResponsibleEmployee(int id) {
        super(id);
    }

    @Override
    boolean canApprove() {
        return true;
    }
}

final class Indicator {
    private final String name;

    Indicator(String name) {
        this.name = name;
    }

    String name() {
        return name;
    }
}

interface RangeRule {
    boolean accepts(double value, double minimum, double maximum);
}

final class InclusiveRangeRule implements RangeRule {
    @Override
    public boolean accepts(double value, double minimum, double maximum) {
        return minimum <= value && value <= maximum;
    }
}

final class ExactValueRule implements RangeRule {
    @Override
    public boolean accepts(double value, double minimum, double maximum) {
        return value == minimum && value == maximum;
    }
}

final class Standard {
    private final Indicator indicator;
    private final double minimum;
    private final double maximum;
    private final RangeRule rule;

    Standard(Indicator indicator, double minimum, double maximum) {
        this.indicator = indicator;
        this.minimum = minimum;
        this.maximum = maximum;
        this.rule = minimum == maximum ? new ExactValueRule() : new InclusiveRangeRule();
    }

    Indicator indicator() {
        return indicator;
    }

    double minimum() {
        return minimum;
    }

    double maximum() {
        return maximum;
    }

    boolean accepts(double value) {
        return rule.accepts(value, minimum, maximum);
    }
}

final class Reading {
    private final Indicator indicator;
    private final double value;

    Reading(Indicator indicator, double value) {
        this.indicator = indicator;
        this.value = value;
    }

    Indicator indicator() {
        return indicator;
    }

    double value() {
        return value;
    }
}

final class MeasurementResult {
    private final Sample sample;
    private final Standard standard;
    private final double value;
    private final boolean withinNorm;

    MeasurementResult(Sample sample, Standard standard, double value) {
        this.sample = sample;
        this.standard = standard;
        this.value = value;
        this.withinNorm = standard.accepts(value);
    }

    Sample sample() {
        return sample;
    }

    Standard standard() {
        return standard;
    }

    Indicator indicator() {
        return standard.indicator();
    }

    double value() {
        return value;
    }

    boolean withinNorm() {
        return withinNorm;
    }

    String status() {
        return withinNorm ? "норма" : "отклонение";
    }
}

final class Sample {
    private final int id;
    private final LocalDate date;
    private final List<Reading> readings;

    Sample(int id, LocalDate date, List<Reading> readings) {
        this.id = id;
        this.date = date;
        this.readings = List.copyOf(readings);
    }

    int id() {
        return id;
    }

    LocalDate date() {
        return date;
    }

    List<Reading> readings() {
        return readings;
    }

    List<MeasurementResult> checkWaterQuality(List<Standard> standards) {
        Map<String, Standard> standardsByIndicator = new LinkedHashMap<>();
        for (Standard standard : standards) {
            standardsByIndicator.put(standard.indicator().name(), standard);
        }
        List<MeasurementResult> results = new ArrayList<>();
        for (Reading reading : readings) {
            Standard standard = standardsByIndicator.get(reading.indicator().name());
            results.add(new MeasurementResult(this, standard, reading.value()));
        }
        return List.copyOf(results);
    }
}

final class QualityAnalyzer {
    Map<String, Integer> findRepeatedDeviations(List<Sample> samples, List<Standard> standards) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Sample sample : samples) {
            for (Reading reading : sample.readings()) {
                String indicator = reading.indicator().name();
                for (Standard standard : standards) {
                    if (standard.indicator().name().equals(indicator)) {
                        if (!standard.accepts(reading.value())) {
                            counts.merge(indicator, 1, Integer::sum);
                        }
                        break;
                    }
                }
            }
        }
        counts.values().removeIf(count -> count <= 1);
        return counts;
    }
}

final class Notice {
    private final Employee recipient;
    private final List<MeasurementResult> deviations;
    private final Map<String, Integer> repeatedDeviations;

    Notice(Employee recipient, List<MeasurementResult> deviations, Map<String, Integer> repeatedDeviations) {
        this.recipient = recipient;
        this.deviations = List.copyOf(deviations);
        this.repeatedDeviations = Map.copyOf(repeatedDeviations);
    }

    Employee recipient() {
        return recipient;
    }

    List<MeasurementResult> deviations() {
        return deviations;
    }

    Map<String, Integer> repeatedDeviations() {
        return repeatedDeviations;
    }

    String message() {
        return "Обнаружены отклонения от нормативов качества воды.";
    }
}

final class Decision {
    private final int controlId;
    private final LocalDate date;
    private final Employee responsible;
    private final Map<Sample, List<MeasurementResult>> results;
    private final Notice notice;
    private final String status;

    Decision(int controlId, LocalDate date, Employee responsible,
             Map<Sample, List<MeasurementResult>> results, Notice notice) {
        this.controlId = controlId;
        this.date = date;
        this.responsible = responsible;
        this.results = Map.copyOf(results);
        this.notice = notice;
        boolean hasDeviations = results.values().stream().flatMap(List::stream)
                .anyMatch(result -> !result.withinNorm());
        this.status = hasDeviations ? "требуются корректирующие действия" : "соответствует нормативам";
    }

    String status() {
        return status;
    }
}

final class Control {
    private final int id;
    private final LocalDate date;
    private final Employee responsible;
    private List<Sample> samples = List.of();

    Control(int id, LocalDate date, Employee responsible) {
        this.id = id;
        this.date = date;
        this.responsible = responsible;
    }

    void registerResults(List<Sample> samples) {
        this.samples = List.copyOf(samples);
    }

    List<Sample> samples() {
        return samples;
    }

    List<String> checkData(List<Standard> standards) {
        List<String> errors = new ArrayList<>();
        if (id == 0) {
            errors.add("Не указан идентификатор контроля.");
        }
        if (date == null) {
            errors.add("Не указана дата контроля.");
        }
        if (responsible == null || responsible.id() == 0) {
            errors.add("Не указан ответственный сотрудник.");
        }
        if (samples.isEmpty()) {
            errors.add("У контроля должна быть хотя бы одна проба.");
            return errors;
        }
        Map<String, Standard> byIndicator = new LinkedHashMap<>();
        for (Standard standard : standards) {
            String name = standard.indicator().name();
            if (name == null || name.isEmpty()) {
                errors.add("В нормативе не указан показатель.");
                continue;
            }
            if (byIndicator.putIfAbsent(name, standard) != null) {
                errors.add("Норматив для показателя «" + name + "» указан повторно.");
            }
            if (standard.minimum() > standard.maximum()) {
                errors.add("Минимум больше максимума для «" + name + "».");
            }
        }
        for (Sample sample : samples) {
            if (sample.id() == 0) {
                errors.add("У пробы не указан идентификатор.");
            }
            if (sample.date() == null) {
                errors.add("У пробы не указана дата.");
            }
            if (sample.readings().isEmpty()) {
                errors.add("У пробы " + sample.id() + " нет измерений.");
                continue;
            }
            for (Reading reading : sample.readings()) {
                String name = reading.indicator().name();
                if (name == null || name.isEmpty()) {
                    errors.add("В измерении не указан показатель.");
                    continue;
                }
                if (!byIndicator.containsKey(name)) {
                    errors.add("Для показателя «" + name + "» отсутствует норматив.");
                }
            }
        }
        return errors;
    }

    Map<Sample, List<MeasurementResult>> compareWithStandards(List<Standard> standards) {
        Map<Sample, List<MeasurementResult>> results = new LinkedHashMap<>();
        for (Sample sample : samples) {
            results.put(sample, sample.checkWaterQuality(standards));
        }
        return results;
    }

    Notice createNonconformityNotice(Map<Sample, List<MeasurementResult>> results, Map<String, Integer> repeated) {
        List<MeasurementResult> deviations = new ArrayList<>();
        for (List<MeasurementResult> sampleResults : results.values()) {
            for (MeasurementResult result : sampleResults) {
                if (!result.withinNorm()) {
                    deviations.add(result);
                }
            }
        }
        return deviations.isEmpty() ? null : new Notice(responsible, deviations, repeated);
    }

    Decision saveControlHistory(Map<Sample, List<MeasurementResult>> results, Notice notice, List<Decision> history) {
        if (!responsible.canApprove()) {
            throw new IllegalStateException("Сотрудник не уполномочен фиксировать решение.");
        }
        Decision decision = new Decision(id, date, responsible, results, notice);
        history.add(decision);
        return decision;
    }
}
