def check_water_quality(measurements, standards):
    standards_by_indicator = {
        standard["indicator"]: standard
        for standard in standards
    }

    checked_results = []

    for measurement in measurements:
        indicator = measurement["indicator"]
        value = measurement["value"]
        standard = standards_by_indicator[indicator]

        minimum = standard["min"]
        maximum = standard["max"]
        is_within_norm = minimum <= value <= maximum

        checked_results.append({
            "indicator": indicator,
            "value": value,
            "min": minimum,
            "max": maximum,
            "status": "норма" if is_within_norm else "отклонение",
            "is_within_norm": is_within_norm,
        })

    return checked_results


def compare_with_standards(control, standards):
    results = []

    for sample in control["samples"]:
        checked_measurements = check_water_quality(
            sample["measurements"],
            standards,
        )

        results.append({
            "sample_id": sample["sample_id"],
            "date": sample["date"],
            "measurements": checked_measurements,
        })

    return results


def find_repeated_deviations(samples, standards):
    deviations_count = {}

    for sample in samples:
        for measurement in sample["measurements"]:
            indicator = measurement["indicator"]
            value = measurement["value"]

            standard = next(
                (
                    item for item in standards
                    if item["indicator"] == indicator
                ),
                None,
            )

            if standard is None:
                continue

            if not standard["min"] <= value <= standard["max"]:
                deviations_count[indicator] = (
                    deviations_count.get(indicator, 0) + 1
                )

    return [
        {
            "indicator": indicator,
            "deviation_count": count,
        }
        for indicator, count in deviations_count.items()
        if count > 1
    ]