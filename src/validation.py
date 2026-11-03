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

        if not isinstance(minimum, (int, float)):
            errors.append(f"Минимальное значение для «{indicator}» некорректно.")

        if not isinstance(maximum, (int, float)):
            errors.append(f"Максимальное значение для «{indicator}» некорректно.")

        if (
            isinstance(minimum, (int, float))
            and isinstance(maximum, (int, float))
            and minimum > maximum
        ):
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

            if not isinstance(value, (int, float)):
                errors.append(
                    f"Значение показателя «{indicator}» должно быть числом."
                )

            if indicator not in standards_by_indicator:
                errors.append(
                    f"Для показателя «{indicator}» отсутствует норматив."
                )

    return errors