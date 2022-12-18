package io.plotnik.freewriting;

// https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/package-summary.html
import java.io.File;
import java.time.*;
import static java.lang.System.out;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Управление базой фрирайтов.
 */
public class Freewriting {

    /**
     * Каталог, в котором находятся фрирайты.
     */
    String home;

    /**
     * Сегодняшняя дата.
     */
    LocalDate today;

    /**
     * Hазвания месяцев с падежами.
     */
    static List<String> monthNames = Arrays.asList(new String[]{
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря"});

    /**
     * Список дат и имен имеющихся фрирайтов.
     */
    List<FwDate> fdates;

    // Новые даты в Java
    // https://docs.oracle.com/javase/tutorial/datetime/TOC.html
    // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/package-summary.html
    
    /**
     * Первая дата базы.
     */
    LocalDate dbStart;

    /**
     * Первая дата сезона
     */
    LocalDate start;

    Calendar cal = Calendar.getInstance();
    static DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static DateTimeFormatter weekDayFormat = DateTimeFormatter.ofPattern("EE", new Locale("ru"));

    /**
     * Мы загрузим все имеющиеся файлы `.md`
     * из текущего каталога и подкаталогов сезонов.
     * Затем определим, какой реально дате соответствует каждый файл,
     * какова начальная дата базы и есть ли пропуски.
     */
    Freewriting(String home) throws FwException {
        try {
            this.home = home;
            today = LocalDate.now();
            dbStart = LocalDate.parse("2016-09-13", df);

            /* Загрузим все имеющиеся файлы `.md` из текущего каталога
               и подкаталогов сезонов
             */
            File hf = new File(home);
            fdates = new ArrayList<>();
            scanFolder(hf, today.getYear(), today.getMonthValue() - 1);
            File[] hfiles = hf.listFiles();
            for (File f : hfiles) {
                if (f.isDirectory()) {
                    scanFolder(f, extractSeasonYear(f.getName()), -1);
                }
            }
            fdates = fdates.stream().sorted(Comparator.comparing(FwDate::getDate))
                    .collect(Collectors.toList());
            start = fdates.get(0).getDate();

            /* Проверяем, что нет пропущенных дат.
               Ищем даты от стартовой.
             */
            LocalDate date = start;
            while (date.isBefore(today) || date.isEqual(today)) {
                final LocalDate d = date;
                Optional<FwDate> f = fdates.stream()
                        .filter((FwDate it) -> it.getDate().isEqual(d)).findFirst();
                if (!f.isPresent()) {
                    out.println("Date not found: " + date);
                    break;
                }
                date = date.plusDays(1);
            }

            int count = fdates.size();
            out.printf("%d records loaded from `%s` to `%s`\n",
                    count, nameFormat(start), nameFormat(today));

        } catch (ParseException e) {
            throw new FwException("[ParseException] " + e.getMessage());
        }
    }

    int extractSeasonYear(String fname) {
        return Integer.parseInt(fname.substring(0, fname.indexOf('-')));
    }

    /**
     * Загрузим все имеющиеся файлы `.md` из указанного каталога
     * @param dir   Папка, которую нужно просканировать
     * @param year  Поскольку в имени файла фрирайта не указан год,
     *              то нам нужно передавать его как параметр.
     * @param curMonth  Текущий месяц для корневой папки.
     *                  Его нужно указывать, поскольку для корневой папки
     *                  часть файлов может принадлежать предыдущему году.
     *                  Для остальных папок -1.
     */
    void scanFolder(File dir, int year, int curMonth)
            throws FwException, ParseException {
        out.println("...Scanning folder: " + dir.getPath());

        File[] dirFiles = dir.listFiles();
        for (File f : dirFiles) {
            if (f.isDirectory()) {
                continue;
            }
            if (!f.getName().endsWith(".md")) {
                throw new FwException("I EXPECT THE FOLDER TO CONTAIN ONLY '.md' FILES");
            }

            /* Преобразовать имя файла фрирайта в дату
             */
            String fileName = f.getName().substring(0, f.getName().length() - 3);
            String[] d = fileName.split(" ");
            if (d.length != 3) {
                throw new FwException("I EXPECT THE FILE NAME TO BE IN 'DAY MONTH WEEKDAY' FORMAT");
            }

            /* Для некоторых месяцев возможно сокращенное написание.
             */
            switch (d[1]) {
                case "сент":
                    d[1] = "сентября";
                    break;
                case "окт":
                    d[1] = "октября";
                    break;
            }

            int month = monthNames.indexOf(d[1]);
            int day = Integer.parseInt(d[0]);
            if (month == -1) {
                throw new FwException("MONTH NAME MISSPELLED IN FILE: " + fileName);
            }

            int yearD = handleDecember(year, month, curMonth);
            //out.println("-- year: " + year + ", month: " + month + ", curMonth: " + curMonth + ", yearD: " + yearD);

            String datestr = String.format("%4d-%02d-%02d", yearD, (month + 1), day);
            LocalDate date = LocalDate.parse(datestr, df);
            fdates.add(new FwDate(date, f, (curMonth != -1)));

            /* Verify that formatting `date` back will give the same `fileName`
             */
            String nameFormatResult = nameFormat(date);
            String oldName = String.join(" ", d);
            if (!nameFormatResult.equals(oldName)) {
                out.println("Parsed date: " + nameFormatResult);
                out.println("Original file name: " + oldName);

                throw new FwException("I EXPECT THE PARSED DATE AFTER FORMATTING "
                        + "TO BE THE SAME AS THE ORIGINAL FILE NAME");
            }

        }

    }

    /**
     * Вернуть дату в формате фрирайта, например `13 мая сб`
     */
    static String nameFormat(LocalDate date) {
        String weekDay = weekDayFormat.format(date).toLowerCase();
        return date.getDayOfMonth() + " "
                + monthNames.get(date.getMonthValue() - 1) + " " + weekDay;
    }

    /**
     * Для всех месяцев мы считаем год равным текущему году,
     * но для декабря это может быть прошлый год.
     * @param year      Год для всей папки
     * @param month     Месяц для файла фрирайта
     * @param curMonth  Текущий месяц
     * @return          Год для файла фрирайта
     */
    static int handleDecember(int year, int month, int curMonth) {
        if (month != 11) { // все месяцы, кроме декабря
            return year;
        }
        if (curMonth == -1) { // декабрь для папки сезона
            return year - 1;
        }
        // декабрь для корневой папки
        return curMonth == 11 ? year : year - 1;
    }

    public List<FwDate> getFdates() {
        return fdates;
    }

}
