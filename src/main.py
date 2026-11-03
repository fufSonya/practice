from validation import check_data
from quality_check import compare_with_standards, find_repeated_deviations
from notifications import create_nonconformity_notice, save_control_history


def register_results(control_data, sample_data):
    control = control_data.copy()
    control["samples"] = [sample.copy() for sample in sample_data]
    return control


def get_demo_data():
    control_data = {
        "control_id": 1,
        "date": "2026-10-20",
        "responsible_id": 101,
    }

    sample_data = [
        {
            "sample_id": 1,
            "date": "2026-10-20",
            "measurements": [
                {"indicator": "pH", "value": 7.2},
                {"indicator": "Мутность", "value": 1.8},
            ],
        },
        {
            "sample_id": 2,
            "date": "2026-10-20",
            "measurements": [
                {"indicator": "pH", "value": 8.1},
                {"indicator": "Мутность", "value": 2.5},
            ],
        },
    ]

    standards = [
        {"indicator": "pH", "min": 6.0, "max": 9.0},
        {"indicator": "Мутность", "min": 0.0, "max": 2.0},
    ]

    return control_data, sample_data, standards


def main():
    control_data, sample_data, standards = get_demo_data()

    control = register_results(control_data, sample_data)

    print("1. Проверка входных данных")
    errors = check_data(control, standards)

    if errors:
        for error in errors:
            print(f"Ошибка: {error}")
        return

    print("Данные корректны.")

    print("\n2. Проверка качества воды")
    checked_results = compare_with_standards(control, standards)

    for sample_result in checked_results:
        print(f"Проба {sample_result['sample_id']}:")
        for measurement in sample_result["measurements"]:
            print(
                f"  {measurement['indicator']}: "
                f"{measurement['value']} — {measurement['status']}"
            )

    print("\n3. Поиск повторных отклонений")
    repeated_deviations = find_repeated_deviations(
        control["samples"],
        standards,
    )

    if repeated_deviations:
        for item in repeated_deviations:
            print(
                f"{item['indicator']}: "
                f"{item['deviation_count']} отклонения"
            )
    else:
        print("Повторных отклонений нет.")

    print("\n4. Формирование уведомления")
    notice = create_nonconformity_notice(
        checked_results,
        repeated_deviations,
        control["responsible_id"],
    )

    if notice:
        print(notice["message"])
        print(f"Количество отклонений: {len(notice['deviations'])}")
    else:
        print("Уведомление не требуется.")

    print("\n5. Сохранение истории контроля")
    history = []
    record = save_control_history(
        control,
        checked_results,
        notice,
        history,
    )

    print(f"Решение: {record['decision']}")
    print(f"Записей в истории: {len(history)}")


if __name__ == "__main__":
    main()