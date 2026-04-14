# Stella Typechecker (stage 2)
Описание второго этапа тайпчекера
## Возможности тайпчекера
Данный тайпчекер поддерживает следующие ОБЯЗАТЕЛЬНЫЕ по требованиям второго этапа части языка Stella:
1. Все обязательные конструкции первого этапа
2. Расширение `#sequencing`
3. Расширение `#references`
4. Расширение `#panic`
5. Расширение `#exceptions` и `#exception-type-declaration`
6. Расширение `#structural-subtyping`
7. Расширение `#type-cast`
8. Расширение `#top-type` и `#bottom-type`
9. Расширение `#ambiguous-type-as-bottom`

Данный тайпчекер поддерживает следующие НЕОБЯЗАТЕЛЬНЫЕ по требованиям второго этапа части языка Stella:
1. ✅ Расширение `#open-variant-exceptions`
2. ✅ Расширение `#try-cast-as`
3. ✅ Дополнительные коды ошибок
    - ✅ `ERROR_DUPLICATE_EXCEPTION_TYPE`
    - ✅ `ERROR_DUPLICATE_EXCEPTION_VARIANT`
    - ✅ `ERROR_CONFLICTING_EXCEPTION_DECLARATIONS`
    - ✅ `ERROR_ILLEGAL_LOCAL_EXCEPTION_TYPE`
    - ✅ `ERROR_ILLEGAL_LOCAL_OPEN_VARIANT_EXCEPTION`

## Примечания по реализации
1. По какой-то причине, эталонный тайпчекер имеет несколько разное поведение при проверке type-cast'ов и type-cast-pattern'ов. Если быть точнее, то:

   Такой код успешно проходит проверку тайпчекером:
   ```stella
   language core;
    extend with #top-type;
    extend with #structural-subtyping;
    extend with #type-cast;

    fn main(n : Nat) -> Bool {
        return n cast as Bool
    }
   ```

   а такой - нет, получая в ответ ошибку:
   ```stella
    language core;
    extend with #type-cast-patterns;
    extend with #top-type;
    extend with #structural-patterns;
    extend with #structural-subtyping;
    
    
    fn main(n : Nat) -> Bool {
        return match n {
            b cast as Bool => b
            | n => false
        }
    }
   ```
   
   ```
    An error occurred during typechecking!
    Type Error Tag: [ERROR_UNEXPECTED_SUBTYPE]
    expected a subtype of
        Nat
    but got
        Bool
   ```
   При этом если заменить Nat на Top, то ошибки не будет.

   Т.е. "обычный" type-cast не проверяет, что тип к которому кастуется выражение является подтипом выведенного типа выражения, а type-cast-pattern - проверяет.

   Данная особенность сохранена в моей реализации тайпчекера