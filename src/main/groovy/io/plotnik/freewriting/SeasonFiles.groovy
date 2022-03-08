package io.plotnik.freewriting;

public class SeasonFiles {

    String home;

    String targetFolder;

    List<FwDate> fdates;

    SeasonFiles(String home) {
        this.home = home;
    }

    public setFdates(List<FwDate> fdates) {
        this.fdates = fdates;
    }

    void moveSeason(String season) throws FwException {
        Date now = new Date();
        int year = now.getYear() + 1900;
        moveSeason(year, season);
    }

  /**
   * Переместить файлы сезона из общего списка в отдельную папку.
   */
  void moveSeason(year, season) throws FwException {
    targetFolder = home + '/' + year + '-' + (season instanceof String? season: season.name)
    println "...Moving season to: $targetFolder"
    switch (season) {
      case 'зима':
      case Season.WINTER:
          moveMonth(year-1, 11)
          moveMonth(year, 0)
          moveMonth(year, 1)
          break
      case 'весна':
      case Season.SPRING:
          moveMonth(year, 2)
          moveMonth(year, 3)
          moveMonth(year, 4)
          break
      case 'лето':
      case Season.SUMMER:
          moveMonth(year, 5)
          moveMonth(year, 6)
          moveMonth(year, 7)
          break
      case 'осень':
      case Season.AUTUMN:
          moveMonth(year, 8)
          moveMonth(year, 9)
          moveMonth(year, 10)
          break
    }
    println "...Season moved to: $targetFolder"
  }

  /**
   * Переместить файлы указанного месяца из общего списка в отдельную папку.
   */
  void moveMonth(year, monthNum) throws FwException {
    println "...Moving month: ${monthNum+1}.$year"
    new File(targetFolder).mkdir()
    int k = 0
    for (freewrite in fdates) { // проходим по записям в базе фрирайтов
      if (freewrite.isRoot() &&
          freewrite.date.getYear() == year &&
          freewrite.date.getMonth() == monthNum) {
        //String fname = nameFormat(freewrite.date) + '.md'
        File f1 = new File(freewrite.path)
        String fname = f1.name
        println "- " + fname
        boolean fileMoved = f1.renameTo(new File(targetFolder, fname))
        if (!fileMoved) {
        	throw new FwException('I EXPECT THAT FILE `' + fname + '` WAS MOVED TO `' + targetFolder + '` FOLDER')
        }
        k++
      }
    }
    println "   $k files moved"
  }
}