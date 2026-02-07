# RefontSocial

<div align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.16.5–1.21.x-green?style=for-the-badge" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Core-Bukkit%20%7C%20Spigot%20%7C%20Paper-blue?style=for-the-badge" alt="Server Core">
  <img src="https://img.shields.io/badge/Java-8%2B-orange?style=for-the-badge" alt="Java Version">
  <img src="https://img.shields.io/github/v/release/RizonChik/RefontSocial?style=for-the-badge" alt="Release">
  <img src="https://img.shields.io/github/downloads/RizonChik/RefontSocial/total?style=for-the-badge" alt="Downloads">
</div>

Социальная репутация игроков: лайки/дизлайки, теги (причины), топы и красивый профиль. Подходит для выживания, RP и любых серверов, где важно поощрять адекватность и активность.

## Возможности

- Лайк/дизлайк репутации через команду или GUI
- Теги/причины оценки (например: “помог”, “RP-игрок”, “токсик”)
- Профиль игрока: рейтинг, место в топе, лайки/дизлайки, топ-теги, история оценок
- Топы по категориям: рейтинг, лайки, дизлайки, голоса
- Антиабуз: кулдауны, дневной лимит, запрет self-vote, защита по IP, требование “взаимодействия рядом”
- Защита от “фейковых” ников: нельзя оценивать игрока, который ни разу не заходил
- PlaceholderAPI: плейсхолдеры для таба/скора/чата
- Хранилище на выбор: SQLite / MySQL / YAML
- Алгоритм рейтинга: SIMPLE_RATIO или BAYESIAN (сглаживание первых голосов)

## Требования

- Paper/Spigot/Bukkit: 1.16.5–1.21.x
- Java: 8+ (используй Java, которая требуется твоему ядру; для 1.20+ обычно нужна Java 17)

Опционально:
- PlaceholderAPI (если нужны плейсхолдеры)

## Установка

1. Скачай `RefontSocial-*.jar` из Releases: https://github.com/RizonChik/RefontSocial/releases
2. Помести `.jar` в папку `plugins/`
3. Перезапусти сервер
4. Настрой `config.yml` (тип хранилища, антиабуз, причины/теги, GUI)

Плагин создаст нужные файлы при первом запуске.

## Быстрый старт

- `/rep` — показать свой рейтинг
- `/rep ник` — открыть меню оценки игрока
- `/rep profile ник` — профиль игрока (GUI)
- `/rep top` — общий топ по рейтингу

## Команды

- `/rep` — показать свой рейтинг
- `/rep ник` — открыть меню оценки игрока
- `/rep like ник` — поставить лайк (с выбором тега)
- `/rep dislike ник` — поставить дизлайк (с выбором тега)
- `/rep profile ник` — открыть профиль игрока (GUI)
- `/rep top` — топ по рейтингу
- `/rep top score|likes|dislikes|votes` — топ по категориям (GUI)
- `/rep reload` — перезагрузка конфига/GUI/сообщений (админ)

## Права

- `refontsocial.use` — доступ к командам
- `refontsocial.admin` — админ-команды (reload) + расширенные возможности
- `refontsocial.bypass.cooldown` — обход кулдаунов/лимитов
- `refontsocial.bypass.interaction` — обход требования “взаимодействия рядом”
- `refontsocial.bypass.ip` — обход защиты по IP

## PlaceholderAPI

Основные:
- `%refontsocial_score%` — рейтинг игрока
- `%refontsocial_likes%` — лайки
- `%refontsocial_dislikes%` — дизлайки
- `%refontsocial_votes%` — всего голосов (likes+dislikes)
- `%refontsocial_rank%` — место в топе

Топ-плейсы:
- `%refontsocial_nick_1%` — ник #1 в топе
- `%refontsocial_score_1%` — рейтинг #1
- `%refontsocial_like_1%` — лайки #1
- `%refontsocial_dislike_1%` — дизлайки #1
- `%refontsocial_votes_1%` — голоса #1

Аналогично: `_2`, `_3`, ... (ограничение: `placeholders.topMax`)

## Как считается рейтинг?

Рейтинг хранится на шкале `min..max` (в конфиге).

Доступны 2 алгоритма:
- `SIMPLE_RATIO` — простая доля лайков (быстро “скачет” на 1–2 голосах)
- `BAYESIAN` — сглаживание первых голосов (рекомендуется), параметр `priorVotes` отвечает за “стабильность” старта

## Конфигурация

```yaml
storage:
  type: SQLITE # SQLITE | MYSQL | YAML

  sqlite:
    file: "data.db" # Файл SQLite в папке плагина

  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "refontsocial"
    username: "root"
    password: "password"
    useSSL: false
    serverTimezone: "UTC"
    params: "useUnicode=true&characterEncoding=utf8&autoReconnect=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false"
    pool:
      maximumPoolSize: 10
      minimumIdle: 2
      connectionTimeoutMs: 10000
      idleTimeoutMs: 600000
      maxLifetimeMs: 1800000

rating:
  scale:
    min: 0.0 # Минимум шкалы рейтинга
    max: 5.0 # Максимум шкалы рейтинга
  defaultScore: 5.0 # Рейтинг без голосов
  format: "#0.0"    # Формат вывода (в GUI/плейсах)

  # SIMPLE_RATIO: быстро скачет на 1-2 голосах
  # BAYESIAN: сглаживает первые голоса (рекомендую)
  algorithm: BAYESIAN
  bayesian:
    priorVotes: 12 # Чем больше — тем стабильнее рейтинг на старте

antiAbuse:
  preventSelfVote: true # Запрет накрутки себе

  targetEligibility:
    requireHasPlayedBefore: true # Нельзя оценивать тех, кто ни разу не заходил
    requireTargetOnline: false   # true = оценка только если цель онлайн

  ipProtection:
    enabled: false # Включить защиту от накрутки с одного IP
    mode: SAME_IP_DENY # SAME_IP_DENY = запрет, SAME_IP_COOLDOWN = кулдаун
    cooldownSeconds: 86400 # Кулдаун для SAME_IP_COOLDOWN (в секундах)

  cooldowns:
    voteGlobalSeconds: 20  # Пауза между любыми оценками
    sameTargetSeconds: 600 # Пауза на одного и того же игрока
    changeVoteSeconds: 1800 # Пауза на смену лайк↔дизлайк

  requireInteraction:
    enabled: true
    radiusBlocks: 100.0  # Радиус в блоках
    requiredSecondsNear: 8  # Этот параметр больше не используется
    interactionValidSeconds: 86400  # Сколько действует "контакт"
    taskPeriodTicks: 40

  dailyLimit:
    enabled: true
    maxVotesPerDay: 20 # Лимит оценок в сутки на игрока

reasons:
  enabled: true # Включить теги/причины
  requireReason: false # true = без выбора тега оценка не пройдет
  maxReasonLength: 24 # Сейчас используется только как лимит (для будущих расширений)

  tags:
    helpful: "§aпомог"
    polite: "§aадекватный"
    toxic: "§cтоксик"
    scam: "§cобман"
    rp: "§bRP-игрок"
    trader: "§eторговец"
    newbie: "§7новичок"

profile:
  history:
    enabled: true # Показывать историю оценок в профиле
    showVoterNameMode: PERMISSION # ANONYMOUS | PERMISSION | ALWAYS
    showVoterNamePermission: "refontsocial.admin" # Кто видит имена в истории (если PERMISSION)
    limit: 10 # Сколько записей показывать
    showVoterName: false # Старый флаг, не используй (оставь false)

  topTags:
    enabled: true # Показывать топ-теги в профиле
    limit: 3 # Сколько тегов показывать

gui:
  top:
    title: "§x§C§4§8§4§E§2Р§x§C§4§8§7§E§1е§x§C§4§8§A§E§0п§x§C§4§8§D§D§Fу§x§C§4§9§0§D§Eт§x§C§3§9§2§D§Cа§x§C§3§9§5§D§Bц§x§C§3§9§8§D§Aи§x§C§3§9§B§D§9я §7• §fТоп"
    size: 54
    pageSize: 45

  rate:
    title: "§x§C§4§8§4§E§2О§x§C§4§8§9§E§0ц§x§C§4§8§D§D§Eе§x§C§3§9§2§D§Dн§x§C§3§9§6§D§Bк§x§C§3§9§B§D§9а"
    size: 27

  profile:
    title: "§x§C§4§8§4§E§2П§x§C§4§8§8§E§1р§x§C§4§8§C§E§0о§x§C§4§9§0§D§Fф§x§C§3§9§4§D§Eи§x§C§3§9§8§D§Dл§x§C§3§9§B§D§9ь"
    size: 54

  reasons:
    title: "§x§C§4§8§4§E§2П§x§C§4§8§8§E§1р§x§C§4§8§C§E§0и§x§C§4§9§0§D§Fч§x§C§3§9§4§D§Eи§x§C§3§9§8§D§Dн§x§C§3§9§B§D§9а"
    size: 54

  categoryTop:
    title: "§x§C§4§8§4§E§2Т§x§C§4§8§8§E§1о§x§C§4§8§C§E§0п §7• §f%category%"
    size: 54
    pageSize: 45

performance:
  cache:
    enabled: true # Кэшировать профили (уменьшает запросы в БД)
    expireSeconds: 30 # Время жизни кэша

placeholders:
  notFound: "§7Не найден" # Что возвращать, если плейсхолдер не нашел игрока/место
  topMax: 200 # Максимальное N для %refontsocial_*_N%

libraries:
  enabled: true
  folder: "libs"
  repositories:
    - "https://repo1.maven.org/maven2/"
  sqlite:
    enabled: true
    groupId: "org.xerial"
    artifactId: "sqlite-jdbc"
    version: "3.46.0.0"
  mysql:
    enabled: true
    groupId: "com.mysql"
    artifactId: "mysql-connector-j"
    version: "8.0.33"
