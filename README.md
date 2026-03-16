# Stella Typechecker
Тайпчекер реализован в рамках первого этапа проекта.
## Возможности тайпчекера
Данный тайпчекер поддерживает следующие ОБЯЗАТЕЛЬНЫЕ по требованиям первого этапа части языка Stella:
1. Ядро языка:
   1. Программный модуль
   2. Объявления функций
   3. Логические выражения
   4. Выражения с натуральными числами
   5. Функции как значения первого класса
2. Расширение `#unit-type`
3. Расширение `#tuples` (и `#pairs` как его подмножество)
4. Расширение `#records`
5. Расширение `#let-bindings`
6. Расширение `#type-ascriptions`
7. Расширение `#sum-types`
8. Расширение `#lists`
9. Расширение `#variants`
10. Расширение `#fixpoint-combinator`

Данный тайпчекер поддерживает следующие НЕОБЯЗАТЕЛЬНЫЕ по требованиям первого этапа части языка Stella:
1. ✅ Расширение `#natural-literals`
2. ✅ Расширение `#nested-function-declarations`
3. ✅ Расширения `#nullary-functions` и `#multiparameter-functions`
4. ✅ Расширение `#structural-patterns`
5. ❌ Расширение `#nullary-variant-labels`
6. ✅ Расширение `#letrec-bindings` и `#pattern-ascriptions`
7. ❌ Дополнительные коды ошибок
    - `ERROR_DUPLICATE_FUNCTION_PARAMETER`
    - `ERROR_DUPLICATE_LET_BINDING`
    - `ERROR_DUPLICATE_TYPE_PARAMETER`


## Запуск тайпчекера
Простейший способ - `make run`. Собирает docker-образ при его отсутствии и запускает его в интерактивном режиме.

"Натуральным" для баша образом можно подставить файлв качестве stdin для тайпчекера:
`make run < samples/ill-typed.stella`