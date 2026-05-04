# Kotlin за 30 минут (то что встретится в нашем коде)

Это не учебник Kotlin, а **сжатый разбор именно тех конструкций**, которые
ты уже видишь в `MainActivity.kt`, `CameraScreen.kt`, `FaceLandmarkerHelper.kt`.
Цель — чтобы ты читал код проекта и понимал **что происходит**.

---

## 1. Объявление переменных

```kotlin
val x = 5            // val — иммутабельная, как const
var y = 10           // var — мутабельная
y = 20               // ок

val z: Int = 5       // явный тип. Обычно опускается, выводится сам
```

**Правило проекта:** всегда `val`, если только переменная не меняется.

---

## 2. Функции

```kotlin
fun add(a: Int, b: Int): Int {
    return a + b
}

// Однострочные функции — короче:
fun addShort(a: Int, b: Int): Int = a + b

// Параметры по умолчанию:
fun greet(name: String = "world") = "Hello, $name"
greet()              // "Hello, world"
greet("Kotlin")      // "Hello, Kotlin"
```

В `FaceLandmarkerHelper.kt` ты видишь:

```kotlin
private val onError: (String) -> Unit = { Log.e(TAG, it) }
```

Это **переменная типа функция**. Тип `(String) -> Unit` — функция, принимающая
String, ничего не возвращающая. По умолчанию — лямбда, логирующая ошибку.

---

## 3. Классы

```kotlin
class Point(val x: Int, val y: Int) {
    fun lengthSq() = x * x + y * y
}

val p = Point(3, 4)   // без `new`!
println(p.x)
println(p.lengthSq())
```

**Главный конструктор** пишется прямо в скобках после имени класса.

### data class — для структур данных

```kotlin
data class CameraPermissionState(
    val granted: Boolean,
    val requestPermission: () -> Unit,
)
```

Автоматически получает `equals`, `hashCode`, `toString`, `copy`. Используем,
когда нужно носить пачку значений, без поведения.

### object — синглтон

```kotlin
object DatabaseConfig {
    val url = "..."
}
```

В нашем проекте ещё не используется, но встретится позже.

### companion object — статические члены

```kotlin
class FaceLandmarkerHelper(...) {
    // ...
    companion object {
        private const val TAG = "FaceLandmarkerHelper"
    }
}
```

Это аналог `static` из Java. `FaceLandmarkerHelper.TAG`.

---

## 4. Лямбды

Во всём коде Compose ты видишь блоки `{ ... }` — это лямбды.

```kotlin
val square: (Int) -> Int = { x -> x * x }
square(5)   // 25

// Если параметр один — можно использовать `it`:
val square2: (Int) -> Int = { it * it }
```

Если функция принимает **последним параметром лямбду**, её можно вынести
за скобки:

```kotlin
fun launch(block: () -> Unit) { ... }

launch { println("hi") }  // вместо launch({ println("hi") })
```

Поэтому в `CameraScreen.kt` `Button(onClick = onGrantClick) { Text(...) }` —
это вызов `Button(onClick=..., content={ Text(...) })`.

---

## 5. Null-безопасность

```kotlin
var s: String = "abc"
s = null              // ОШИБКА компиляции

var s2: String? = "abc"  // знак вопроса = nullable
s2 = null             // ок

// Безопасный вызов:
val len = s2?.length  // если s2 null, len = null

// Эльвис-оператор:
val len2 = s2?.length ?: 0   // если null, то 0

// Принудительная разбивка (плохая идея, кидает NPE):
val len3 = s2!!.length
```

В `CameraScreen.kt`:

```kotlin
result.faceLandmarks().firstOrNull()?.size ?: 0
```

Читается так: «возьми первый список landmarks (или null), у него размер
(или null), либо подставь 0».

---

## 6. when — расширенный switch

```kotlin
when (x) {
    1 -> println("один")
    2, 3 -> println("два или три")
    in 4..10 -> println("от 4 до 10")
    is String -> println("строка")
    else -> println("другое")
}
```

`when` в Kotlin **возвращает значение**:

```kotlin
val name = when (n) {
    1 -> "один"
    2 -> "два"
    else -> "много"
}
```

---

## 7. Корутины (без сильного погружения)

В нашем коде на неделе 1 корутины почти не используются — мы обходимся
коллбэками от MediaPipe и CameraX. Но в неделю 2-3 могут появиться:

```kotlin
// suspend-функция — может «приостанавливаться» без блокировки потока
suspend fun loadModel(): Model { ... }

// LaunchedEffect внутри Compose запускает корутину, привязанную к жизни Composable:
LaunchedEffect(Unit) {
    if (!granted) launcher.launch(Manifest.permission.CAMERA)
}
```

Подробно про корутины — [kotlinlang.org/docs/coroutines-overview.html](https://kotlinlang.org/docs/coroutines-overview.html).

---

## 8. Composable-функции

Это основа Jetpack Compose:

```kotlin
@Composable
fun MyScreen() {
    var counter by remember { mutableIntStateOf(0) }

    Column {
        Text("Counter: $counter")
        Button(onClick = { counter++ }) {
            Text("Increment")
        }
    }
}
```

Ключевые моменты:

| Конструкция        | Что делает                                                    |
|--------------------|---------------------------------------------------------------|
| `@Composable`      | Помечает функцию как UI-компонент. Может вызывать только из других @Composable. |
| `remember { ... }` | Сохраняет значение между перерисовками.                       |
| `mutableStateOf`   | Создаёт «реактивную» переменную: при изменении Compose сам перерисует UI. |
| `by remember`      | Property delegation: даёт прямой `counter` вместо `counter.value`. |
| `LaunchedEffect`   | Запускает корутину при первой композиции (или при смене ключа). |
| `DisposableEffect` | То же + блок `onDispose { }`, выполняемый при удалении из дерева. |

В `CameraScreen.kt` мы используем все эти конструкции — посмотри ещё раз
с этим знанием, теперь должно быть понятно.

---

## 9. Что почитать дальше (если всерьёз интересно)

- Официальный туториал Kotlin: [kotlinlang.org/docs/getting-started.html](https://kotlinlang.org/docs/getting-started.html)
- Compose codelab: [developer.android.com/courses/jetpack-compose/course](https://developer.android.com/courses/jetpack-compose/course)
- Стандартная библиотека Kotlin (apply/let/run/with): [kotlinlang.org/docs/scope-functions.html](https://kotlinlang.org/docs/scope-functions.html)

Но прямо сейчас тебе хватит этой шпаргалки, чтобы читать наш код. Возвращайся
сюда, как только в коде встретится непонятная конструкция.
