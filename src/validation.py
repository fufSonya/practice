from math import isfinite


def _is_finite_number(value):
    # Целые числа Python конечны и не требуют преобразования во float.
    return isinstance(value, int) or (isinstance(value, float) and isfinite(value))


def check_data(control, standards):
    errors = []

    if not control.get("control_id"):
        errors.append("Не указан идентификатор контроля.")

    if not control.get("date"):
        errors.append("Не указана дата контроля.")

    if not control.get("responsible_id"):
        errors.append("Не указан ответственный сотрудник.")

    if not isinstance(control.get("samples"), list) or not control["samples"]:
        errors.append("У контроля должна быть хотя бы одна проба.")
        return errors

    standards_by_indicator = {}

    for standard in standards:
        indicator = standard.get("indicator")

        if not indicator:
            errors.append("В нормативе не указан показатель.")
            continue

        if indicator in standards_by_indicator:
            errors.append(f"Норматив для показателя «{indicator}» указан повторно.")
        else:
            standards_by_indicator[indicator] = standard

        minimum = standard.get("min")
        maximum = standard.get("max")

        minimum_is_finite = _is_finite_number(minimum)
        maximum_is_finite = _is_finite_number(maximum)

        if not minimum_is_finite:
            errors.append(f"Минимальное значение для «{indicator}» некорректно.")

        if not maximum_is_finite:
            errors.append(f"Максимальное значение для «{indicator}» некорректно.")

        if minimum_is_finite and maximum_is_finite and minimum > maximum:
            errors.append(f"Минимум больше максимума для «{indicator}».")

    for sample in control["samples"]:
        if not sample.get("sample_id"):
            errors.append("У пробы не указан идентификатор.")

        if not sample.get("date"):
            errors.append("У пробы не указана дата.")

        measurements = sample.get("measurements")

        if not isinstance(measurements, list) or not measurements:
            errors.append(
                f"У пробы {sample.get('sample_id', 'без ID')} нет измерений."
            )
            continue

        for measurement in measurements:
            indicator = measurement.get("indicator")
            value = measurement.get("value")

            if not indicator:
                errors.append("В измерении не указан показатель.")
                continue

            if not _is_finite_number(value):
                errors.append(
                    f"Значение показателя «{indicator}» должно быть конечным числом."
                )

            if indicator not in standards_by_indicator:
                errors.append(
                    f"Для показателя «{indicator}» отсутствует норматив."
                )

    return errors
