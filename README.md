# Ghostbound

Spectator restrictions and consent-based teleport requests for Minecraft Java 1.20.1 and Forge 47.x. The project uses Java 17 and currently builds against Forge 47.4.28.

[English](#english) | [Русский](#русский)

## English

Ghostbound gives server owners control over spectator access to containers, vanilla spectator teleportation, and entity cameras. Spectators can request a visit to another player through chat. The server checks both players again before teleporting.

The mod applies to players currently in Spectator, including those placed there by Awaiting Dawn. It does not change death, respawn, or limited-lives mechanics.

### Installation

Build the project and put `build/libs/ghostbound-1.0.0.jar` in the server's `mods` directory. Use Minecraft 1.20.1 with Forge 47.x.

The client mod is optional. Players without it can use commands and clickable chat buttons with English fallback messages. Install the same JAR on a Forge client for English/Russian translations and the configurable P shortcut. No additional library mod or resource pack is required.

### Teleport requests

| Action | Command or control |
| --- | --- |
| Request a visit to a player | `/spectp Steve` |
| Accept a specific request | `/spectpaccept <UUID>` |
| Decline a specific request | `/spectpdeny <UUID>` |
| Accept the latest valid incoming request | `/spectpaccept` or P with the client mod |
| Decline the latest valid incoming request | `/spectpdeny` |

Incoming chat messages have accept and decline buttons. P works when no screen is open and can be rebound in Controls > Ghostbound.

The sender must remain a spectator. The recipient must be alive and in Survival, Adventure, or Creative. Operators go through the same request checks. Acceptance moves the sender to the recipient's current position and dimension.

Default limits:

- A request expires after 60 real seconds
- Declining or letting a request expire starts a 600-second cooldown for that sender/recipient pair
- A sender must wait 3 seconds between new requests and can have up to 3 pending requests
- Accepting a request does not create a cooldown

### Configuration and saved data

Forge creates `<world>/serverconfig/ghostbound-server.toml`. In singleplayer, find it under `saves/<world>/serverconfig/`. See the [example configuration](config/ghostbound-server.toml) for all options. Stop the server before editing and restart afterward; `/reload` does not reload this Forge config.

The three options under `restrictions` independently block container viewing, vanilla spectator teleportation, and entity spectating. All are enabled by default. Teleport requests and incoming chat notifications have separate switches. Hiding notifications still allows players to accept the latest request with a command or P.

Requests, expiry times, pair cooldowns, and spam limits are saved in `<world>/data/ghostbound_requests.dat`. Disconnects and restarts do not reset deadlines. Expired requests are processed on the next server tick or access. Keep the server clock accurate: clock changes affect real-time deadlines.

Administrative commands such as `/tp`, `/gamemode`, and `/spectate`, along with third-party teleportation, retain their own behavior. Review [SECURITY_NOTES.md](SECURITY_NOTES.md) before combining mods that change spectator handling. Removing Ghostbound leaves its saved data behind; deleting that file resets its stored requests and cooldowns.

### Building and testing

Install JDK 17 and set `JAVA_HOME` to its installation directory.

```powershell
.\gradlew.bat build
```

On Linux or macOS:

```sh
bash ./gradlew build
```

The release JAR is written to `build/libs/`. The Gradle Wrapper is included; a separate Gradle installation is unnecessary.

Development tasks are `runClient`, `runServer`, and `runGameTestServer`. GameTests live in `src/main/java/com/paceah/ghostbound/gametest` and are excluded from the release JAR. [TESTING.md](TESTING.md) records validation results and remaining manual checks. A successful build alone does not verify two-player behavior on a dedicated server.

### License

[LICENSE](LICENSE) contains the MIT license. The current mod metadata declares `All Rights Reserved`; these declarations need to be reconciled before distributing a release.

## Русский

Ghostbound позволяет настроить доступ наблюдателей к контейнерам, ванильной телепортации и камерам сущностей. Для перехода к другому игроку наблюдатель отправляет заявку в чате. Перед телепортацией сервер повторно проверяет обоих участников.

Мод работает с игроками, которые сейчас находятся в Spectator, в том числе после перевода в этот режим через Awaiting Dawn. Механики смерти, возрождения и ограниченных жизней он не меняет.

### Установка

Соберите проект и поместите `build/libs/ghostbound-1.0.0.jar` в папку `mods` сервера. Нужны Minecraft 1.20.1 и Forge 47.x.

На клиенте мод необязателен. Без него работают команды и кнопки в чате с английскими сообщениями. Для переводов на русский и английский и переназначаемой клавиши P установите тот же JAR на Forge-клиент. Дополнительные моды-библиотеки и ресурспаки не нужны.

### Заявки на телепортацию

| Действие | Команда или клавиша |
| --- | --- |
| Запросить переход к игроку | `/spectp Steve` |
| Принять конкретную заявку | `/spectpaccept <UUID>` |
| Отклонить конкретную заявку | `/spectpdeny <UUID>` |
| Принять последнюю действительную входящую заявку | `/spectpaccept` или P с клиентским модом |
| Отклонить последнюю действительную входящую заявку | `/spectpdeny` |

Во входящем сообщении есть кнопки принятия и отказа. Клавиша P работает при закрытых экранах и переназначается в настройках управления, в разделе Ghostbound.

Отправитель должен оставаться наблюдателем. Получатель должен быть жив и находиться в Survival, Adventure или Creative. Операторы проходят те же проверки заявок. При принятии отправитель переносится в текущее измерение и текущую позицию получателя.

Ограничения по умолчанию:

- Заявка действует 60 реальных секунд
- Отказ или истечение запускают кулдаун 600 секунд для конкретной пары отправитель/получатель
- Между новыми заявками одного отправителя должно пройти 3 секунды, одновременно допускается до 3 ожидающих заявок
- Принятие заявки не создаёт кулдаун

### Настройки и сохранение данных

Forge создаёт файл `<world>/serverconfig/ghostbound-server.toml`. В одиночной игре он находится в `saves/<world>/serverconfig/`. Все параметры есть в [примере конфига](config/ghostbound-server.toml). Остановите сервер перед изменениями и перезапустите после них; `/reload` не перезагружает этот конфиг Forge.

Три параметра раздела `restrictions` независимо запрещают просмотр контейнеров, ванильную телепортацию наблюдателя и наблюдение от лица сущностей. По умолчанию все три запрета включены. У заявок и входящих уведомлений отдельные переключатели. При скрытых уведомлениях последнюю заявку по-прежнему можно принять командой или клавишей P.

Заявки, сроки истечения, парные кулдауны и ограничения частоты сохраняются в `<world>/data/ghostbound_requests.dat`. Выход игроков и перезапуск сервера не сбрасывают сроки. Просроченные заявки обрабатываются на следующем серверном тике или при обращении. Следите за системными часами сервера: их перевод влияет на реальные сроки.

Административные команды `/tp`, `/gamemode`, `/spectate` и сторонние телепортации сохраняют своё поведение. Перед совместным использованием модов, меняющих Spectator, прочитайте [SECURITY_NOTES.md](SECURITY_NOTES.md). После удаления Ghostbound файл его данных остаётся в мире; удаление этого файла сбрасывает сохранённые заявки и кулдауны.

### Сборка и проверка

Установите JDK 17 и укажите путь к нему в `JAVA_HOME`.

```powershell
.\gradlew.bat build
```

В Linux или macOS:

```sh
bash ./gradlew build
```

Готовый JAR появится в `build/libs/`. Gradle Wrapper включён в проект, отдельно устанавливать Gradle не нужно.

Для разработки доступны задачи `runClient`, `runServer` и `runGameTestServer`. GameTest-тесты находятся в `src/main/java/com/paceah/ghostbound/gametest` и не попадают в релизный JAR. Результаты проверок и оставшиеся ручные сценарии описаны в [TESTING.md](TESTING.md). Успешная сборка сама по себе не подтверждает работу двух игроков на выделенном сервере.

### Лицензия

Файл [LICENSE](LICENSE) содержит лицензию MIT. В текущих метаданных мода указано `All Rights Reserved`; перед распространением релиза эти условия нужно согласовать.
