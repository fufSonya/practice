def create_nonconformity_notice(
    checked_results,
    repeated_deviations,
    responsible_id,
):
    deviations = []

    for sample_result in checked_results:
        for measurement in sample_result["measurements"]:
            if not measurement["is_within_norm"]:
                deviations.append({
                    "sample_id": sample_result["sample_id"],
                    "indicator": measurement["indicator"],
                    "value": measurement["value"],
                    "min": measurement["min"],
                    "max": measurement["max"],
                })

    if not deviations:
        return None

    return {
        "responsible_id": responsible_id,
        "deviations": deviations,
        "repeated_deviations": repeated_deviations,
        "message": "Обнаружены отклонения от нормативов качества воды.",
    }


def save_control_history(control, checked_results, notice, history):
    has_deviations = any(
        not measurement["is_within_norm"]
        for sample_result in checked_results
        for measurement in sample_result["measurements"]
    )

    record = {
        "control_id": control["control_id"],
        "date": control["date"],
        "responsible_id": control["responsible_id"],
        "results": checked_results,
        "notice": notice,
        "decision": (
            "требуются корректирующие действия"
            if has_deviations
            else "соответствует нормативам"
        ),
    }

    history.append(record)
    return record