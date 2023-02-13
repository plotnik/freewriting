package io.plotnik.freewriting;

import groovy.json.*
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;

public class SearchPatterns {

    // шаблоны для поиска
    Map<String,Object> searchPatterns = [:];

    // настройки UI
    Map uiState = [:];

    // папка с шаблонами для поиска
    String patternsFolder = 'patterns/'

    // файл для сохранения настроек UI
    String uiStateFileName = 'ui-state.json'

    // в эту папку помещаем сгенеренные документы
    String wwwFolder = 'www/'

    JsonSlurper jsonSlurper = new JsonSlurper();

    String home;

    String serverURL;

    String asciidoctor;

    List<FwDate> fdates;

    SimpleDateFormat linkFormat = new SimpleDateFormat('yyyy-MM-dd');
    DateTimeFormatter ymdFormat = DateTimeFormatter.ofPattern('dd.MM.yyyy');


    SearchPatterns(String home, String serverURL, String asciidoctor) {
        this.home = home;
        this.serverURL = serverURL;
        this.asciidoctor = asciidoctor;
    }

    public setFdates(List<FwDate> fdates) {
        this.fdates = fdates;
    }

    /**
     * Прочитать определения поисковых паттернов
     */
    void readPatternsFolder() throws FwException {

        File pdir = new File(home, patternsFolder);
        if (!pdir.exists()) {
            throw new FwException("Folder not found: " + pdir.path)
        }

        def pfiles = pdir.listFiles()
        int count = 0
        pfiles.each {
            if (it.name.endsWith('.json')) {
                def p = jsonSlurper.parseText(it.text)
                p.fname = it.name.substring(0, it.name.length()-5)
                searchPatterns.put(p.title, p)
                count++
            }
        }
        //println "Search patterns found: " + count

        // Прочитать сортировку поисковых паттернов
        File uiStateFile = new File(home, uiStateFileName)
        if (uiStateFile.exists()) {
            uiState = jsonSlurper.parseText(uiStateFile.text)
        }
    }

    String getPatternName(String patternFile) {
        def p = jsonSlurper.parseText(new File(home, patternsFolder + patternFile + ".json").text);
        return p.title
    }

    List<String> getSortedNames() {
        List keys = new ArrayList(searchPatterns.keySet())
        keys.sort { uiState[it]==null? 0: -uiState[it] }
        return keys;
    }

    void moveToListTop(String result) {
        uiState[result] = new Date().getTime()
        new File(home, uiStateFileName).text = JsonOutput.toJson(uiState)
    }

    /**
     * Каждый поиск имеет свой список шаблонов.
     */
    void extract(String patternName) {
        def p = searchPatterns[patternName]
        if (p == null) {
            println "[ERROR] Search pattern not found $patternName"
            return
        }
        if (p.s != null) {
            extractPages(wwwFolder + p.fname + '.adoc', p.title, p.s)
            createOutputIndex(wwwFolder + 'index.adoc')
        } else
        if (p.i != null) {
            extractIntervals(wwwFolder + p.fname + '.adoc', p.title, p.i)
            createOutputIndex(wwwFolder + 'index.adoc')
        } else {
            println "[ERROR] `$patternName` cannot search either pages or intervals"
        }
    }

    /**
     * Собрать в результирующий файл фрирайты, содержащие указанные шаблоны.
     * @param outName  Имя результирующего файла
     * @param title    Заголовок страницы
     * @param patterns Список ключевых слов
     */
    void extractPages(String outName,
                      String title,
                      List<String> patterns) {
        println '[extractPages] ' + '.'*60
        Writer f = new File(home, outName).newWriter("UTF-8")
        f.println "= " + title
        f.println ":toc:"
        f.println ""

        /* Показать шаблоны в паттерне
         */
        f.println patterns.join(", ") + "\n\n";


        /* Пройти по всем файлам фрирайтов
         */
        String fsep = System.getProperty('file.separator')
        String season1 = null;
        int foundCount = 0
        fdates.each { fdate ->
            String fname = fdate.path
            //println "~~ " + fname

            /* Поискать паттерны в строчках файла
             */
            File t = new File(fname)
            //println "-- t: " + t.path
            String[] sents = t.text.split(/\./)
            boolean found = false
            for (int i=0; i<sents.length; i++) {
                String sent = sents[i].toLowerCase()
                //println "~~ " + sent
                if (contains(sent, patterns)) {
                    found = true
                    foundCount++
                    //println foundCount + ": " + sents[i].replace('\n',' ').trim()
                }
            }

            /* Если что-то найдено, добавить фрирайт в результаты поиска.
             */
            if (found) {
                int k = fname.lastIndexOf(fsep) + 1
                String tstamp = fname[k..-4]
                int k2 = fname.lastIndexOf(fsep, k-2) + 1
                String season2 = fname[k2..k-2]
                if (season1!=season2) {
                    f.println "\n== " + season2.replace('-', ' ')
                    season1 = season2
                }

                f.println linkByDate(fdate.date, tstamp)
            }
        }
        f.close()
        println "`$outName` created, $foundCount findings"
        runAsciidoctor(outName)
    }

    String linkByDate(date, tstamp) {
        //println "-- linkByDate: `${date}` : `${date.getClass().getName()}` : ${tstamp}"
        //String dstr = linkFormat.format(date.toString());
        String dstr = date.toString()
        return "- link:${serverURL}/${dstr}[${tstamp}]"
    }

  /**
   * Собрать в результирующий файл фрирайты, содержащие указанные интервалы дат.
   * @param outName    Имя результирующего файла
   * @param title      Заголовок страницы
   * @param intervals  Список интервалов дат
   */
  void extractIntervals(String outName,
                    String title,
                    List<List<String>> intervals) {
    println '.'*80
    Writer f = new File(home, outName).newWriter("UTF-8")
    f.println "= " + title
    f.println ":toc:"
    f.println ""

    String fsep = System.getProperty('file.separator')
    String season1 = null;
    for (iv in intervals) {
    	LocalDate dt1 = LocalDate.parse(iv[0], ymdFormat)
    	LocalDate dt2 = LocalDate.parse(iv[1], ymdFormat)

        for (LocalDate d = dt1; d.isBefore(dt2) || d.isEqual(dt2); d = d.plusDays(1)) {

    		def fdate = fdates.find { it.date==d }
    		String fname = fdate.path
    		String text = new File(fname).text

			int k = fname.lastIndexOf(fsep) + 1
            String tstamp = fname[k..-4]
			int k2 = fname.lastIndexOf(fsep, k-2) + 1
			String season2 = fname[k2..k-2]
			if (season1!=season2) {
			  f.println "== " + season2.replace('-', ' ')
			  season1 = season2
			}
			f.println linkByDate(fdate.date, tstamp)
    	}
    }
    f.close()
    println "== `$outName` created"
    runAsciidoctor(outName)
  }

  void runAsciidoctor(String outName) {
    if (asciidoctor == null) {
        return;
    }
    try {
        def proc = "$asciidoctor $outName".execute([], new File(home))
        proc.waitForProcessOutput(System.out, System.err)
        println "asciidoctor $outName | exit code: " + proc.exitValue()

    } catch (Exception e) {
        println "[WARN] " + e.getMessage();
    }
  }

  /**
   * Указанная строка содержит хотя бы один из списка указанных шаблонов.
   */
  boolean contains(String s, List<String> patterns) {
    boolean result = false
    patterns.each {
      if (s.contains(it)) {
        result = true
      }
    }
    return result
  }

    void createOutputIndex(String outName) {
        List keys = getSortedNames()
        Writer f = new File(outName).newWriter("UTF-8")
        f.println "= Freewriting"
        f.println ""
          for (key in keys) {
                def p = searchPatterns[key]
                //assert p.fname.endsWith('.adoc')
                //String fname = p.fname.substring(0, p.fname.length()-5)
                f.println "- link:${p.fname}.html[${p.title}]"
          }
        f.close()
        println "== `$outName` created"
        runAsciidoctor(outName)
    }
}
