



![LOGO](https://user-images.githubusercontent.com/122222024/211359036-5e1c49d0-ebca-48c5-a95b-a74f7064678f.png)




WEB-приложение SESearchBot - поисковый движок, осуществляющий индексирование, сохранение индексируемой информации в БД и дальнейший поиск по заданным интернет-сайтам с учетом морфологии русского языка.

![IndexingPage](https://user-images.githubusercontent.com/122222024/211211135-960bca9b-03ac-4314-ad99-938928d5e19e.png)

Получает на выходе список найденных страниц в соответствии с заданным запросом,  и релевантностью найденной информации, независимо от морфологических форм слов в запросе.

![SearchPage](https://user-images.githubusercontent.com/122222024/211304586-e95688f0-702b-449c-8837-8aeeffa2bd8a.png)

Представляет из себя  WEB-приложение, управление которым осуществляется через обычный интернет-браузер.
Построено на основе фреймворка  SpringFramework.Boot (https://spring.io/projects/spring-boot), с использованием библиотеки Spring-Boot-Starter-WEB, реализующей REST-контроллеры (классы APIController, DefaultController), осуществляющий взаимодействие между frontend и backend частями приложения, с помощью встроенного Web-сервера Apache TomCat. И библиотеки Spring-Boot-Starter-Data-JPA, включающей механизм объектно-реляционного отображения ORM Hibernate, дополнительный слой абстракций Jakarta Persistent API или JPA, позволяющий отображать записи в таблицах БД в экземпляры объектов Java и обратно, а также включающий ряд удобных методов работы с БД в стиле Java, а не в виде запросов SQL. Подключение зависимостей можно увидеть в файле pom.xml данного проекта с настройками для фреймворка Maven (https://maven.apache.org).
Для скрытия шаблонного кода, геттеров, сеттеров, конструкторов и других стандартных методов, для облегчения восприятия основного кода используется библиотека Lombok (https://projectlombok.org) и соответствующие аннотации.

Обход сайтов реализован в классе Crawler (интернет-паук) в многопоточном режиме c полной или частичной загрузкой всех имеющихся процессоров/ядер/сред с использованием фреймворка Fork/Join, с разбиением работы на множество мелких подзадач от сотен до десятков тысяч и равномерное их распределение по очередям к средам исполнения. Метод Crawler.printDebug включен по умолчанию и позволяет наблюдать на консоли процесс индексации - адреса страниц, коды ответов, количество проиндексированных страниц, некоторые перехваченные ошибки. Например, тайм-аут открытия страницы.

Для парсинга ссылок, найденных на HTML-страницах используется библиотека JSoup (https://jsoup.org). Найденные гиперссылки рекурсивно передаются для последующего парсинга вновь создаваемым для этого экземплярам классов, унаследованным от RecursiveAction с помощью метода fork. Содержимое, после удаления всей не текстовой информации, кроме заголовка страниц Title, пробелов и тире, адреса страниц, коды ответа HTTP записываются в таблицу page базы данных. Например размер типовой страницы на новостном сайте Lenta.ru около 300 кб, а их количество более 500 тысяч страниц. Можно представить какой объем лишней информации придется хранить и обрабатывать. К тому же часто на страницах встречаются не текстовые символы, которые вызывают исключение при сохранении в СУБД. После удаления всей не текстовой информации со страницы, размер страницы с 300 кб уменьшается до 16 и менее кб. Т.к. информация заголовка title HTML-страницы Используется при выводе поисковой информации, то заголовок страницы переформатируется и вставляется в содержимое с нулевой позиции, а окончание заголовка помечается словом title на латинице.  

В качестве базы данных можно использовать любую реляционную СУБД. В данном проекте подключена СУБД MySQL.  Для ее работы необходимо установить соответствующий сервер СУБД и средство управления им, например WorkBench для MySQL и создать пустую схему БД с названием search_engine.

![YML1![WorkBench](https://user-images.githubusercontent.com/122222024/211324980-f9eb4918-3d29-4b72-811b-2705bb246778.png)

Настроить соответствующий диалект БД в поле dialect. Путь подключения, имя и пароль пользователя с правами root в полях url, user, password соответственно. Также при первом запуске программы для автоматического создания таблиц в БД необходимо установить параметр     ddl-auto: create-drop, а при обычной работе программы параметр должен быть установлен в update. 

![YML1](https://user-images.githubusercontent.com/122222024/211325389-515dd0d3-d428-45c0-bc0d-d4dce80cdfbf.png)

Также необходимо добавить зависимость для загрузки библиотеки коннектора подключения к соответствующей БД. Для фреймворка Maven в файл pom.xml необходимо добавить следующие строки.

![YML2](https://user-images.githubusercontent.com/122222024/211324548-ef09316c-2c1d-4918-9a4f-77805a6541cf.png)

Перед сохранением в БД из содержимого обнаруженных HTML-страниц выделяются все слова. Затем в классе LemmaMaker происходит исключение служебных частей речи и преобразование всех оставшихся слов в нормальную форму. Например, существительные преобразуются в именительный падеж, единственное число, мужской род. Данные преобразования производятся с помощью библиотеки морфологической обработки текста Apache Lucene Morphology (https://mvnrepository.com/artifact/org.apache.lucene.morphology). Русскоязычная версия данной библиотеки собрана , скомпилирована образовательной платформой https://skillbox.ru и находится в их репозитории. Подключение  библиотеки можно проследить в файле pom.xml проекта. Такие преобразованные слова называются леммами , а процесс преобразования лемматизацией. Далее подсчитывается количество вхождений каждого слова на данной странице и данные разносятся по таблицам lemma и index, формируя индекс поиска для каждого слова и его связи со страницами (таблица page), на которых оно присутствует. Количество вхождений на данной странице записывается в поле rank таблицы index, а количество страниц, на которых присутствует лемма по мере обхода страниц аккумулируются  в поле frequency таблицы lemma.

Весь процесс индексации координируется классом IndexingService, который получает команду от REST-контроллера на запуск/останов индексации. Индексация возможна либо по всем сайтам из списка сразу, либо по отдельной странице из вышеуказанного списка.

![ManagementPage](https://user-images.githubusercontent.com/122222024/211343066-724a22df-a6cd-47bd-bf2e-5ccc7ed9dc26.png)

Метод run() класса запущен в отдельной среде на протяжении всего процесса индексации и следит за состоянием процесса, отображает его статус в таблице site.status (INDEXING, INDEXED, FAIL). Остановка процесса индексации происходит в трех случаях - по команде пользователя, при обходе всех страниц сайта или при достижении лимита страниц и происходит не мгновенно, а при исчерпании очередей задач ForkJoinPool. Статус FAIL формируется при остановке индексации пользователем. Статус INDEXED информирует об окончании индексации. Список сайтов, лимит количества обходимых страниц, задержка перед открытием следующей страницы настраиваются в полях sites, pageNumberLimit, DelayCrawler соответственно. В формировании ответов в формате JSON задействованы классы в пакете dto.indexing

![IndexingConfig](https://user-images.githubusercontent.com/122222024/211338969-f0ddc7ba-00a5-4b72-89aa-74152c90a587.png)

Задержка нужна для исключения блокировки сайтами обходчика страниц. Также увеличивая задержку можно кратно снижать загрузку процессора. Лимит страниц помогает быстро проверить работоспособность программы, установив небольшое число страниц. 
Для защиты от блокировки сайта также способствуют параметры userAgent, refferer, устанавливаемые в файле конфигурации, которые могут анализироваться WEB-серверами при установлении сеанса.

Формирование и выдачу статистических данных процесса индексации REST-контроллеру производит класс StatisticsServiceImpl, реализованный на основе интерфейса StatisticsService. С помощью него можно следить за ходом индексации сайтов, количеством лемм и индексированных страниц на каждом сайте, периодически обновляя страницу в браузере. В формировании ответов в формате JSON используются классы пакета dto.statistics

Исполнение поисковых запросов и выдачу результатов поиска реализовано в классе SearchService. Для формирования JSON ответов используются классы пакета dto.search. Процес поиска протекает в порядке подобном индексации. Выделяются отдельные слова из поискового запроса, переводятся с помощью класса морфологии LemmaMaker в нормальную форму, то же самое производится со всеми словами страниц в поисковой выдаче. Далее находятся местоположения на странице первого вхождения каждого слова из поискового запроса. Производится формирование фрагментов с найденными словами нужной величины и передача их REST-контроллеру, а от него WEB-браузеру постранично.

Классы пакета config предназначены для передачи настроек (список сайтов) из файла конфигурации application.yaml в программу.

Объектно-реляционное отображение ORM реализовано в пакете model. Классы SiteEntity, PageEntity, LemmaEntity, IndexEntity являются сущностями по терминологии ORM Hibernate и дополнительно создают ассоциации типа ManyToOne между несколькими сущностями. Задают также имена некоторых полей таблиц в БД, основные индексы и индексы поиска, а также внешние ключи. Формируют каскадное обновление или удаление данных в таблицах БД. Позволяют оперировать при сохранении в БД или получении из БД не только отдельными полями данных, но и полями сгруппированными в составе классов-сущностей. 
Репозитории, формируемые на основе интерфейсов SiteRepositoty, PageRepositoty, LemmaRepositoty, IndexRepositoty, создаются при запуске приложения средствами фреймворка SPRING.DATA и реализуют декларированные в них  методы работы с соответствующими таблицами в БД.





